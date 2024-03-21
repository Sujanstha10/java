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
import com.automation.trading.domain.calculation.CIVPARTCalculation;
import com.automation.trading.domain.fred.CIVPART;
import com.automation.trading.repository.CIVPARTCalculationRepository;
import com.automation.trading.repository.CIVPARTRepository;
import com.automation.trading.service.CIVPARTService;
import com.automation.trading.service.FederalEmploymentService.SortByDateCIVPART;
import com.automation.trading.service.FederalEmploymentService.SortByDateCIVPARTCalculation;
import com.automation.trading.service.FederalReserveService;
import com.automation.trading.utility.RestUtility;

@Service
public class CIVPARTUpdateService {

	@Autowired
	CIVPARTRepository civpartRepository;

	@Autowired
	CIVPARTCalculationRepository civpartCalculationRepository;

	@Autowired
	RestTemplate restTemplate;

	@Autowired
	CIVPARTService civpartService;
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

	@Autowired
	FederalReserveService federalReserveService;

	private Logger logger = LoggerFactory.getLogger(CIVPARTUpdateService.class);

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRoc() {

		if (NumberUtils.INTEGER_ZERO.equals(civpartCalculationRepository.findAny())) {
			civpartService.calculateRoc();
			civpartService.updateRocChangeSignDff();
		}

		Optional<List<CIVPART>> civpartListOpt = civpartRepository.findByRocFlagIsFalseOrderByDate();
		Optional<CIVPART> prevCIVPARTOpt = civpartRepository.findFirstByRocFlagIsTrueOrderByDateDesc();
		HashMap<Date, CIVPARTCalculation> civpartCalculationHashMap = new HashMap<>();
		CIVPARTCalculation prevCIVPARTCalculationRow = new CIVPARTCalculation();

		List<CIVPART> civpartList = new ArrayList<>();

		if (civpartListOpt.isPresent()) {
			civpartList = civpartListOpt.get();
			if (prevCIVPARTOpt.isPresent()) {
				civpartList.add(prevCIVPARTOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(civpartList, new SortByDateCIVPART());

		List<CIVPARTCalculation> civpartCalculationReference = civpartCalculationRepository.findAll();
		List<CIVPARTCalculation> civpartCalculationModified = new ArrayList<>();
		Queue<CIVPART> civpartQueue = new LinkedList<>();

		for (CIVPARTCalculation civpartCalculation : civpartCalculationReference) {
			civpartCalculationHashMap.put(civpartCalculation.getToDate(), civpartCalculation);
		}

		for (CIVPART civpart : civpartList) {

			CIVPARTCalculation tempCIVPARTCalculation = new CIVPARTCalculation();
			
			if (civpartQueue.size() == 2) {
				civpartQueue.poll();
			}
			civpartQueue.add(civpart);

			if (civpart.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;

			Iterator<CIVPART> queueIterator = civpartQueue.iterator();

			if (civpartCalculationHashMap.containsKey(civpart.getDate())) {
				tempCIVPARTCalculation = civpartCalculationHashMap.get(civpart.getDate());
			}

			while (queueIterator.hasNext()) {
				CIVPART temp = queueIterator.next();
				temp.setRocFlag(true);
				if (civpartQueue.size() == 1) {
					roc = 0f;
					tempCIVPARTCalculation.setRoc(roc);
					tempCIVPARTCalculation.setToDate(civpart.getDate());
					tempCIVPARTCalculation.setRocChangeSign(0);
				} else {
					roc = (civpart.getValue() / ((LinkedList<CIVPART>) civpartQueue).get(0).getValue()) - 1;
					tempCIVPARTCalculation.setRoc(roc);
					tempCIVPARTCalculation.setToDate(civpart.getDate());
				}

			}

			civpartCalculationModified.add(tempCIVPARTCalculation);
		}

		civpartList = civpartRepository.saveAll(civpartList);
		civpartCalculationModified = civpartCalculationRepository.saveAll(civpartCalculationModified);
		logger.debug("Added new CIVPART row, " + civpartCalculationModified);

		return;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRocRollingAnnualAvg() {

		if (NumberUtils.INTEGER_ZERO.equals(civpartCalculationRepository.findAny())) {
			return;
		}

		Optional<List<CIVPARTCalculation>> civpartCalculationListOpt = civpartCalculationRepository
				.findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();
		Optional<List<CIVPARTCalculation>> prevCIVPARTCalculationListOpt = civpartCalculationRepository
				.findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();
		List<CIVPARTCalculation> civpartCalculationList = new ArrayList<>();

		if (civpartCalculationListOpt.isPresent()) {
			civpartCalculationList = civpartCalculationListOpt.get();
			if (prevCIVPARTCalculationListOpt.isPresent()) {
				civpartCalculationList.addAll(prevCIVPARTCalculationListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(civpartCalculationList, new SortByDateCIVPARTCalculation());

		Queue<CIVPARTCalculation> civpartCalculationPriorityQueue = new LinkedList<>();
		for (CIVPARTCalculation civpartCalculation : civpartCalculationList) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (civpartCalculationPriorityQueue.size() == 4) {
				civpartCalculationPriorityQueue.poll();
			}
			civpartCalculationPriorityQueue.add(civpartCalculation);

			if (civpartCalculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<CIVPARTCalculation> queueIterator = civpartCalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				CIVPARTCalculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			civpartCalculation.setRocAnnRollAvgFlag(true);
			civpartCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(civpartCalculationList);
		civpartCalculationList = civpartCalculationRepository.saveAll(civpartCalculationList);
		logger.info("New civpart calculation record inserted" + civpartCalculationList);
		return;

	}

	/**
	 * Calculates Rolling Average of Three Month CIVPART
	 * 
	 * @return CIVPARTCalculation , updated CIVPARTCalculation Table
	 */
	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRollAvgThreeMonth() {

		if (NumberUtils.INTEGER_ZERO.equals(civpartCalculationRepository.findAny())) {
			return;
		}

		List<CIVPARTCalculation> civpartCalculationList = new ArrayList<>();
		Optional<List<CIVPART>> civpartListOpt = civpartRepository.findByRollAverageFlagIsFalseOrderByDate();
		Optional<List<CIVPART>> prevCIVPARTListOpt = civpartRepository.findTop2ByRollAverageFlagIsTrueOrderByDateDesc();
		List<CIVPARTCalculation> civpartCalculationReference = civpartCalculationRepository.findAll();
		HashMap<Date, CIVPARTCalculation> civpartCalculationHashMap = new HashMap<>();
		List<CIVPART> civpartList = new ArrayList<>();

		for (CIVPARTCalculation civpartCalculation : civpartCalculationReference) {
			civpartCalculationHashMap.put(civpartCalculation.getToDate(), civpartCalculation);
		}

		Queue<CIVPART> civpartQueue = new LinkedList<>();

		if (civpartListOpt.isPresent()) {
			civpartList = civpartListOpt.get();
			if (prevCIVPARTListOpt.isPresent()) {
				civpartList.addAll(prevCIVPARTListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(civpartList, new SortByDateCIVPART());

		for (CIVPART civpart : civpartList) {

			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			if (civpartQueue.size() == 3) {
				civpartQueue.poll();
			}
			civpartQueue.add(civpart);
			if (civpart.getRollAverageFlag()) {
				continue;
			}

			Iterator<CIVPART> queueItr = civpartQueue.iterator();

			CIVPARTCalculation tempCIVPARTCalculation = new CIVPARTCalculation();
			if (civpartCalculationHashMap.containsKey(civpart.getDate())) {
				tempCIVPARTCalculation = civpartCalculationHashMap.get(civpart.getDate());
			} else {
				tempCIVPARTCalculation.setToDate(civpart.getDate());
			}

			while (queueItr.hasNext()) {
				CIVPART civpartVal = queueItr.next();
				rollingAvg += civpartVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			civpart.setRollAverageFlag(true);
			tempCIVPARTCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			civpartCalculationList.add(tempCIVPARTCalculation);

		}

		civpartCalculationReference = civpartCalculationRepository.saveAll(civpartCalculationList);
		civpartList = civpartRepository.saveAll(civpartList);
		return;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public List<CIVPART> getLatestCIVPARTRecords() {

		if (NumberUtils.INTEGER_ZERO.equals(civpartCalculationRepository.findAny())) {
			return null;
		}
		Optional<CIVPART> lastRecordOpt = civpartRepository.findTopByOrderByDateDesc();
		List<CIVPART> response = new ArrayList<>();
		if (lastRecordOpt.isPresent()) {
			CIVPART lastRecord = lastRecordOpt.get();
			String lastDate = lastRecord.getDate().toString();
			String transactionUrl = QUANDL_HOST_URL + "FRED" + "/" + "CIVPART" + "/" + QUANDL_DATA_FORMAT;

			UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(transactionUrl)
					// Add query parameter
					.queryParam("start_date", lastDate).queryParam("order", "ASC")
					.queryParam(QUANDL_API_KEY_NAME, QUANDL_API_KEY_VALUE);

			List<CIVPART> CIVPARTList = new ArrayList<>();
			FederalResponse json = restUtility.consumeResponse(builder.toUriString());
			json.getDataset_data().getData().stream().forEach(o -> {
				ArrayList temp = (ArrayList) o;
				try {
					Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
					CIVPARTList.add(new CIVPART(date, Float.parseFloat(temp.get(1).toString())));
					;
				} catch (ParseException e) {
					e.printStackTrace();
				}
			});

			if (CIVPARTList.size() > 1) { // As last record is already present in DB
				CIVPARTList.remove(0);
				response = civpartRepository.saveAll(CIVPARTList);
				logger.info("New record inserted in CIVPART");
			}

		}
		return response;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void updateRocChangeSignCIVPART() {
		List<CIVPARTCalculation> civpartCalculationList = civpartCalculationRepository.findAllByRocIsNotNullAndRocChangeSignIsNull();
		CIVPARTCalculation lastUpdatedRecord = civpartCalculationRepository.findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

		Collections.sort(civpartCalculationList, new SortByDateCIVPARTCalculation());
		if(civpartCalculationList.size() == 0){
			return;
		}

		Float lastRoc = lastUpdatedRecord.getRoc();
		for (CIVPARTCalculation civpartCalculation : civpartCalculationList) {
			if(civpartCalculation.getRoc() < lastRoc){
				civpartCalculation.setRocChangeSign(-1);
			}else if (civpartCalculation.getRoc() > lastRoc){
				civpartCalculation.setRocChangeSign(1);
			}else if(civpartCalculation.getRoc() == lastRoc){
				civpartCalculation.setRocChangeSign(0);
			}

			lastRoc = civpartCalculation.getRoc();
		}

		civpartCalculationRepository.saveAll(civpartCalculationList);
	}

}
