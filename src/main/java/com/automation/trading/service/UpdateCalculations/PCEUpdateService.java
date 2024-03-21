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
import com.automation.trading.domain.calculation.PCECalculation;
import com.automation.trading.domain.fred.PCE;
import com.automation.trading.repository.PCECalculationRepository;
import com.automation.trading.repository.PCERepository;
import com.automation.trading.service.PCEService;
import com.automation.trading.service.FederalIncomeAndExpenditureService.SortByDatePCE;
import com.automation.trading.service.FederalIncomeAndExpenditureService.SortByDatePCECalculation;
import com.automation.trading.utility.RestUtility;

@Service
public class PCEUpdateService {
	
	@Autowired
	private PCERepository pceRepostiory;

	@Autowired
	private PCECalculationRepository pceCalculationRepository;

	@Autowired
	private PCEService pceRateOfChangeService;

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

	private Logger logger = LoggerFactory.getLogger(PCEUpdateService.class);

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRoc() {

		if (NumberUtils.INTEGER_ZERO.equals(pceCalculationRepository.findAny())) {
			pceRateOfChangeService.calculateRoc();
			pceRateOfChangeService.updateRocChangeSign();
		}

		System.out.println("calculateRocRollingAnnualAvg");

		Optional<List<PCE>> pceListOpt = pceRepostiory.findByRocFlagIsFalseOrderByDate();
		Optional<PCE> prevPCEOpt = pceRepostiory.findFirstByRocFlagIsTrueOrderByDateDesc();
		HashMap<Date, PCECalculation> pceCalculationHashMap = new HashMap<>();
		PCECalculation prevPCECalculationRow = new PCECalculation();

		List<PCE> pceList = new ArrayList<>();

		if (pceListOpt.isPresent()) {
			pceList = pceListOpt.get();
			if (prevPCEOpt.isPresent()) {
				pceList.add(prevPCEOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(pceList, new SortByDatePCE());
		List<PCECalculation> pceCalculationReference = pceCalculationRepository.findAll();
		List<PCECalculation> pceCalculationModified = new ArrayList<>();
		Queue<PCE> pceQueue = new LinkedList<>();

		for (PCECalculation pceCalculation : pceCalculationReference) {
			pceCalculationHashMap.put(pceCalculation.getToDate(), pceCalculation);
		}

		for (PCE pce : pceList) {
			PCECalculation tempPCECalculation = new PCECalculation();

			if (pceQueue.size() == 2) {
				pceQueue.poll();
			}
			pceQueue.add(pce);

			if (pce.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;

			Iterator<PCE> queueIterator = pceQueue.iterator();

			if (pceCalculationHashMap.containsKey(pce.getDate())) {
				tempPCECalculation = pceCalculationHashMap.get(pce.getDate());
			} else {
				tempPCECalculation.setToDate(pce.getDate());
			}

			while (queueIterator.hasNext()) {
				PCE temp = queueIterator.next();
				temp.setRocFlag(true);
				if (pceQueue.size() == 1) {
					roc = 0f;
					tempPCECalculation.setRoc(roc);
					tempPCECalculation.setToDate(pce.getDate());
					tempPCECalculation.setRocChangeSign(0);
				} else {
					roc = (pce.getValue() / ((LinkedList<PCE>) pceQueue).get(0).getValue()) - 1;
					tempPCECalculation.setRoc(roc);
					tempPCECalculation.setToDate(pce.getDate());
				}

			}

			pceCalculationModified.add(tempPCECalculation);
		}

		pceList = pceRepostiory.saveAll(pceList);
		pceCalculationModified = pceCalculationRepository.saveAll(pceCalculationModified);
		logger.debug("Added new PCE row, " + pceCalculationModified);

		return;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRocRollingAnnualAvg() {

		if (NumberUtils.INTEGER_ZERO.equals(pceCalculationRepository.findAny())) {
			return;
		}

		System.out.println("calculateRocRollingAnnualAvg");
		Optional<List<PCECalculation>> pceCalculationListOpt = pceCalculationRepository
				.findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();
		Optional<List<PCECalculation>> prevPCECalculationListOpt = pceCalculationRepository
				.findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();
		List<PCECalculation> pceCalculationList = new ArrayList<>();

		if (pceCalculationListOpt.isPresent()) {
			pceCalculationList = pceCalculationListOpt.get();
			if (prevPCECalculationListOpt.isPresent()) {
				pceCalculationList.addAll(prevPCECalculationListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(pceCalculationList, new SortByDatePCECalculation());

		Queue<PCECalculation> pceCalculationPriorityQueue = new LinkedList<PCECalculation>();
		for (PCECalculation pceCalculation : pceCalculationList) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (pceCalculationPriorityQueue.size() == 4) {
				pceCalculationPriorityQueue.poll();
			}
			pceCalculationPriorityQueue.add(pceCalculation);

			if (pceCalculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<PCECalculation> queueIterator = pceCalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				PCECalculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			pceCalculation.setRocAnnRollAvgFlag(true);
			pceCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(pceCalculationList);
		pceCalculationList = pceCalculationRepository.saveAll(pceCalculationList);
		logger.info("New pce calculation record inserted" + pceCalculationList);
		return;

	}

	/**
	 * Calculates Rolling Average of Three Month PCE
	 *
	 * @return PCECalculation , updated PCECalculation Table
	 */
	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRollAvgThreeMonth() {

		if (NumberUtils.INTEGER_ZERO.equals(pceCalculationRepository.findAny())) {
			return;
		}

		System.out.println("calculateRollAvgThreeMonth");

		List<PCECalculation> pceCalculationList = new ArrayList<>();
		Optional<List<PCE>> pceListOpt = pceRepostiory.findByRollAverageFlagIsFalseOrderByDate();
		Optional<List<PCE>> prevPCEListOpt = pceRepostiory
				.findTop2ByRollAverageFlagIsTrueOrderByDateDesc();
		List<PCECalculation> pceCalculationReference = pceCalculationRepository.findAll();
		HashMap<Date, PCECalculation> pceCalculationHashMap = new HashMap<>();
		List<PCE> pceList = new ArrayList<>();

		for (PCECalculation pceCalculation : pceCalculationReference) {
			pceCalculationHashMap.put(pceCalculation.getToDate(), pceCalculation);
		}

		Queue<PCE> pceQueue = new LinkedList<>();

		if (pceListOpt.isPresent()) {
			pceList = pceListOpt.get();
			if (prevPCEListOpt.isPresent()) {
				pceList.addAll(prevPCEListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(pceList, new SortByDatePCE());

		for (PCE pce : pceList) {

			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			if (pceQueue.size() == 3) {
				pceQueue.poll();
			}
			pceQueue.add(pce);
			if (pce.getRollAverageFlag()) {
				continue;
			}

			Iterator<PCE> queueItr = pceQueue.iterator();

			PCECalculation tempPCECalculation = new PCECalculation();
			if (pceCalculationHashMap.containsKey(pce.getDate())) {
				tempPCECalculation = pceCalculationHashMap.get(pce.getDate());
			} else {
				tempPCECalculation.setToDate(pce.getDate());
			}

			while (queueItr.hasNext()) {
				PCE pceVal = queueItr.next();
				rollingAvg += pceVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			pce.setRollAverageFlag(true);
			tempPCECalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			pceCalculationList.add(tempPCECalculation);

		}

		pceCalculationReference = pceCalculationRepository.saveAll(pceCalculationList);
		pceList = pceRepostiory.saveAll(pceList);
		return;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public List<PCE> getLatestPCERecords() {

		if (NumberUtils.INTEGER_ZERO.equals(pceCalculationRepository.findAny())) {
			return null;
		}
		System.out.println("getLatestPCERecords");
		Optional<PCE> lastRecordOpt = pceRepostiory.findTopByOrderByDateDesc();
		List<PCE> response = new ArrayList<>();
		if (lastRecordOpt.isPresent()) {
			PCE lastRecord = lastRecordOpt.get();
			String lastDate = lastRecord.getDate().toString();
			String transactionUrl = QUANDL_HOST_URL + "FRED" + "/" + "PCE" + "/" + QUANDL_DATA_FORMAT;

			UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(transactionUrl)
					// Add query parameter
					.queryParam("start_date", lastDate).queryParam("order", "ASC")
					.queryParam(QUANDL_API_KEY_NAME, QUANDL_API_KEY_VALUE);

			List<PCE> PCEList = new ArrayList<>();
			FederalResponse json = restUtility.consumeResponse(builder.toUriString());
			json.getDataset_data().getData().stream().forEach(o -> {
				ArrayList temp = (ArrayList) o;
				try {
					Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
					PCEList.add(new PCE(date, Float.parseFloat(temp.get(1).toString())));
					;
				} catch (ParseException e) {
					e.printStackTrace();
				}
			});

			if (PCEList.size() > 1) { // As last record is already present in DB
				PCEList.remove(0);
				response = pceRepostiory.saveAll(PCEList);
				logger.info("New record inserted in PCE");
			}

		}
		return response;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void updateRocChangeSignDgs10() {
		List<PCECalculation> pceCalculationList = pceCalculationRepository.findAllByRocIsNotNullAndRocChangeSignIsNull();
		PCECalculation lastUpdatedRecord = pceCalculationRepository.findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

		Collections.sort(pceCalculationList, new SortByDatePCECalculation());

		if(pceCalculationList.size() == 0){
			return;
		}

		Float lastRoc = lastUpdatedRecord.getRoc();
		for (PCECalculation pceCalculation : pceCalculationList) {
			if(pceCalculation.getRoc() < lastRoc){
				pceCalculation.setRocChangeSign(-1);
			}else if (pceCalculation.getRoc() > lastRoc){
				pceCalculation.setRocChangeSign(1);
			}else if(pceCalculation.getRoc() == lastRoc){
				pceCalculation.setRocChangeSign(0);
			}

			lastRoc = pceCalculation.getRoc();
		}

		pceCalculationRepository.saveAll(pceCalculationList);
	}

}
