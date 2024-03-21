package com.automation.trading.service.UpdateCalculations;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;

import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.automation.trading.common.FederalResponse;
import com.automation.trading.domain.calculation.PCEDGCalculation;
import com.automation.trading.domain.fred.PCEDG;
import com.automation.trading.repository.PCEDGCalculationRepository;
import com.automation.trading.repository.PCEDGRepository;
import com.automation.trading.service.PCEDGService;
import com.automation.trading.service.FederalIncomeAndExpenditureService.SortByDatePCEDG;
import com.automation.trading.service.FederalIncomeAndExpenditureService.SortByDatePCEDGCalculation;
import com.automation.trading.utility.RestUtility;

@Service
public class PCEDGUpdateService {
	
	@Autowired
	private PCEDGRepository pcedgRepostiory;

	@Autowired
	private PCEDGCalculationRepository pcedgCalculationRepository;

	@Autowired
	private PCEDGService pcedgRateOfChangeService;

	@Autowired
	RestTemplate restTemplate;

	@Autowired
	RestUtility restUtility;

	@Value("${quandl.host.url}")
	private String QUANDL_HOST_URL;

	@Value("${quandl.api.key.value}")
	private String QUANDL_API_KEY_VALUE;

	@Value("${quandl.api.key.name}")
	private String QUANDL_API_KEY_NAME;

	@Value("${quandl.data.format}")
	private String QUANDL_DATA_FORMAT;

	private Logger logger = LoggerFactory.getLogger(PCEDGUpdateService.class);

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRoc() {

		if (NumberUtils.INTEGER_ZERO.equals(pcedgCalculationRepository.findAny())) {
			pcedgRateOfChangeService.calculateRoc();
			pcedgRateOfChangeService.updateRocChangeSign();
		}

		System.out.println("calculateRocRollingAnnualAvg");

		Optional<List<PCEDG>> pcedgListOpt = pcedgRepostiory.findByRocFlagIsFalseOrderByDate();
		Optional<PCEDG> prevPCEDGOpt = pcedgRepostiory.findFirstByRocFlagIsTrueOrderByDateDesc();
		HashMap<Date, PCEDGCalculation> pcedgCalculationHashMap = new HashMap<>();
		PCEDGCalculation prevPCEDGCalculationRow = new PCEDGCalculation();

		List<PCEDG> pcedgList = new ArrayList<>();

		if (pcedgListOpt.isPresent()) {
			pcedgList = pcedgListOpt.get();
			if (prevPCEDGOpt.isPresent()) {
				pcedgList.add(prevPCEDGOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(pcedgList, new SortByDatePCEDG());
		List<PCEDGCalculation> pcedgCalculationReference = pcedgCalculationRepository.findAll();
		List<PCEDGCalculation> pcedgCalculationModified = new ArrayList<>();
		Queue<PCEDG> pcedgQueue = new LinkedList<>();

		for (PCEDGCalculation pcedgCalculation : pcedgCalculationReference) {
			pcedgCalculationHashMap.put(pcedgCalculation.getToDate(), pcedgCalculation);
		}

		for (PCEDG pcedg : pcedgList) {
			PCEDGCalculation tempPCEDGCalculation = new PCEDGCalculation();

			if (pcedgQueue.size() == 2) {
				pcedgQueue.poll();
			}
			pcedgQueue.add(pcedg);

			if (pcedg.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;

			Iterator<PCEDG> queueIterator = pcedgQueue.iterator();

			if (pcedgCalculationHashMap.containsKey(pcedg.getDate())) {
				tempPCEDGCalculation = pcedgCalculationHashMap.get(pcedg.getDate());
			} else {
				tempPCEDGCalculation.setToDate(pcedg.getDate());
			}

			while (queueIterator.hasNext()) {
				PCEDG temp = queueIterator.next();
				temp.setRocFlag(true);
				if (pcedgQueue.size() == 1) {
					roc = 0f;
					tempPCEDGCalculation.setRoc(roc);
					tempPCEDGCalculation.setToDate(pcedg.getDate());
					tempPCEDGCalculation.setRocChangeSign(0);
				} else {
					roc = (pcedg.getValue() / ((LinkedList<PCEDG>) pcedgQueue).get(0).getValue()) - 1;
					tempPCEDGCalculation.setRoc(roc);
					tempPCEDGCalculation.setToDate(pcedg.getDate());
				}

			}

			pcedgCalculationModified.add(tempPCEDGCalculation);
		}

		pcedgList = pcedgRepostiory.saveAll(pcedgList);
		pcedgCalculationModified = pcedgCalculationRepository.saveAll(pcedgCalculationModified);
		logger.debug("Added new PCEDG row, " + pcedgCalculationModified);

		return;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRocRollingAnnualAvg() {

		if (NumberUtils.INTEGER_ZERO.equals(pcedgCalculationRepository.findAny())) {
			return;
		}

		System.out.println("calculateRocRollingAnnualAvg");
		Optional<List<PCEDGCalculation>> pcedgCalculationListOpt = pcedgCalculationRepository
				.findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();
		Optional<List<PCEDGCalculation>> prevPCEDGCalculationListOpt = pcedgCalculationRepository
				.findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();
		List<PCEDGCalculation> pcedgCalculationList = new ArrayList<>();

		if (pcedgCalculationListOpt.isPresent()) {
			pcedgCalculationList = pcedgCalculationListOpt.get();
			if (prevPCEDGCalculationListOpt.isPresent()) {
				pcedgCalculationList.addAll(prevPCEDGCalculationListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(pcedgCalculationList, new SortByDatePCEDGCalculation());

		Queue<PCEDGCalculation> pcedgCalculationPriorityQueue = new LinkedList<PCEDGCalculation>();
		for (PCEDGCalculation pcedgCalculation : pcedgCalculationList) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (pcedgCalculationPriorityQueue.size() == 4) {
				pcedgCalculationPriorityQueue.poll();
			}
			pcedgCalculationPriorityQueue.add(pcedgCalculation);

			if (pcedgCalculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<PCEDGCalculation> queueIterator = pcedgCalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				PCEDGCalculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			pcedgCalculation.setRocAnnRollAvgFlag(true);
			pcedgCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(pcedgCalculationList);
		pcedgCalculationList = pcedgCalculationRepository.saveAll(pcedgCalculationList);
		logger.info("New pcedg calculation record inserted" + pcedgCalculationList);
		return;

	}

	/**
	 * Calculates Rolling Average of Three Month PCEDG
	 *
	 * @return PCEDGCalculation , updated PCEDGCalculation Table
	 */
	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRollAvgThreeMonth() {

		if (NumberUtils.INTEGER_ZERO.equals(pcedgCalculationRepository.findAny())) {
			return;
		}

		System.out.println("calculateRollAvgThreeMonth");

		List<PCEDGCalculation> pcedgCalculationList = new ArrayList<>();
		Optional<List<PCEDG>> pcedgListOpt = pcedgRepostiory.findByRollAverageFlagIsFalseOrderByDate();
		Optional<List<PCEDG>> prevPCEDGListOpt = pcedgRepostiory
				.findTop2ByRollAverageFlagIsTrueOrderByDateDesc();
		List<PCEDGCalculation> pcedgCalculationReference = pcedgCalculationRepository.findAll();
		HashMap<Date, PCEDGCalculation> pcedgCalculationHashMap = new HashMap<>();
		List<PCEDG> pcedgList = new ArrayList<>();

		for (PCEDGCalculation pcedgCalculation : pcedgCalculationReference) {
			pcedgCalculationHashMap.put(pcedgCalculation.getToDate(), pcedgCalculation);
		}

		Queue<PCEDG> pcedgQueue = new LinkedList<>();

		if (pcedgListOpt.isPresent()) {
			pcedgList = pcedgListOpt.get();
			if (prevPCEDGListOpt.isPresent()) {
				pcedgList.addAll(prevPCEDGListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(pcedgList, new SortByDatePCEDG());

		for (PCEDG pcedg : pcedgList) {

			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			if (pcedgQueue.size() == 3) {
				pcedgQueue.poll();
			}
			pcedgQueue.add(pcedg);
			if (pcedg.getRollAverageFlag()) {
				continue;
			}

			Iterator<PCEDG> queueItr = pcedgQueue.iterator();

			PCEDGCalculation tempPCEDGCalculation = new PCEDGCalculation();
			if (pcedgCalculationHashMap.containsKey(pcedg.getDate())) {
				tempPCEDGCalculation = pcedgCalculationHashMap.get(pcedg.getDate());
			} else {
				tempPCEDGCalculation.setToDate(pcedg.getDate());
			}

			while (queueItr.hasNext()) {
				PCEDG pcedgVal = queueItr.next();
				rollingAvg += pcedgVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			pcedg.setRollAverageFlag(true);
			tempPCEDGCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			pcedgCalculationList.add(tempPCEDGCalculation);

		}

		pcedgCalculationReference = pcedgCalculationRepository.saveAll(pcedgCalculationList);
		pcedgList = pcedgRepostiory.saveAll(pcedgList);
		return;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public List<PCEDG> getLatestPCEDGRecords() {

		if (NumberUtils.INTEGER_ZERO.equals(pcedgCalculationRepository.findAny())) {
			return null;
		}
		System.out.println("getLatestPCEDGRecords");
		Optional<PCEDG> lastRecordOpt = pcedgRepostiory.findTopByOrderByDateDesc();
		List<PCEDG> response = new ArrayList<>();
		if (lastRecordOpt.isPresent()) {
			PCEDG lastRecord = lastRecordOpt.get();
			String lastDate = lastRecord.getDate().toString();
			String transactionUrl = QUANDL_HOST_URL + "FRED" + "/" + "PCEDG" + "/" + QUANDL_DATA_FORMAT;

			UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(transactionUrl)
					// Add query parameter
					.queryParam("start_date", lastDate).queryParam("order", "ASC")
					.queryParam(QUANDL_API_KEY_NAME, QUANDL_API_KEY_VALUE);

			List<PCEDG> PCEDGList = new ArrayList<>();
			FederalResponse json = restUtility.consumeResponse(builder.toUriString());
			json.getDataset_data().getData().stream().forEach(o -> {
				ArrayList temp = (ArrayList) o;
				try {
					Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
					PCEDGList.add(new PCEDG(date, Float.parseFloat(temp.get(1).toString())));
					;
				} catch (ParseException e) {
					e.printStackTrace();
				}
			});

			if (PCEDGList.size() > 1) { // As last record is already present in DB
				PCEDGList.remove(0);
				response = pcedgRepostiory.saveAll(PCEDGList);
				logger.info("New record inserted in PCEDG");
			}

		}
		return response;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void updateRocChangeSignDgs10() {
		List<PCEDGCalculation> pcedgCalculationList = pcedgCalculationRepository.findAllByRocIsNotNullAndRocChangeSignIsNull();
		PCEDGCalculation lastUpdatedRecord = pcedgCalculationRepository.findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

		Collections.sort(pcedgCalculationList, new SortByDatePCEDGCalculation());

		if(pcedgCalculationList.size() == 0){
			return;
		}

		Float lastRoc = lastUpdatedRecord.getRoc();
		for (PCEDGCalculation pcedgCalculation : pcedgCalculationList) {
			if(pcedgCalculation.getRoc() < lastRoc){
				pcedgCalculation.setRocChangeSign(-1);
			}else if (pcedgCalculation.getRoc() > lastRoc){
				pcedgCalculation.setRocChangeSign(1);
			}else if(pcedgCalculation.getRoc() == lastRoc){
				pcedgCalculation.setRocChangeSign(0);
			}

			lastRoc = pcedgCalculation.getRoc();
		}

		pcedgCalculationRepository.saveAll(pcedgCalculationList);
	}

}
