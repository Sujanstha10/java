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
import com.automation.trading.domain.calculation.DSPIC96Calculation;
import com.automation.trading.domain.fred.DSPIC96;
import com.automation.trading.repository.DSPIC96CalculationRepository;
import com.automation.trading.repository.DSPIC96Repository;
import com.automation.trading.service.DSPIC96Service;
import com.automation.trading.service.FederalIncomeAndExpenditureService.SortByDateDSPIC96;
import com.automation.trading.service.FederalIncomeAndExpenditureService.SortByDateDSPIC96Calculation;
import com.automation.trading.utility.RestUtility;

@Service
public class DSPIC96UpdateService {

	@Autowired
	private DSPIC96Repository dspic96Repostiory;

	@Autowired
	private DSPIC96CalculationRepository dspic96CalculationRepository;

	@Autowired
	private DSPIC96Service dspic96RateOfChangeService;

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

	private Logger logger = LoggerFactory.getLogger(DSPIC96UpdateService.class);

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRoc() {

		if (NumberUtils.INTEGER_ZERO.equals(dspic96CalculationRepository.findAny())) {
			dspic96RateOfChangeService.calculateRoc();
			dspic96RateOfChangeService.updateRocChangeSign();
		}

		System.out.println("calculateRocRollingAnnualAvg");

		Optional<List<DSPIC96>> dspic96ListOpt = dspic96Repostiory.findByRocFlagIsFalseOrderByDate();
		Optional<DSPIC96> prevDSPIC96Opt = dspic96Repostiory.findFirstByRocFlagIsTrueOrderByDateDesc();
		HashMap<Date, DSPIC96Calculation> dspic96CalculationHashMap = new HashMap<>();
		DSPIC96Calculation prevDSPIC96CalculationRow = new DSPIC96Calculation();

		List<DSPIC96> dspic96List = new ArrayList<>();

		if (dspic96ListOpt.isPresent()) {
			dspic96List = dspic96ListOpt.get();
			if (prevDSPIC96Opt.isPresent()) {
				dspic96List.add(prevDSPIC96Opt.get());
			}
		} else {
			return;
		}

		Collections.sort(dspic96List, new SortByDateDSPIC96());
		List<DSPIC96Calculation> dspic96CalculationReference = dspic96CalculationRepository.findAll();
		List<DSPIC96Calculation> dspic96CalculationModified = new ArrayList<>();
		Queue<DSPIC96> dspic96Queue = new LinkedList<>();

		for (DSPIC96Calculation dspic96Calculation : dspic96CalculationReference) {
			dspic96CalculationHashMap.put(dspic96Calculation.getToDate(), dspic96Calculation);
		}

		for (DSPIC96 dspic96 : dspic96List) {
			DSPIC96Calculation tempDSPIC96Calculation = new DSPIC96Calculation();

			if (dspic96Queue.size() == 2) {
				dspic96Queue.poll();
			}
			dspic96Queue.add(dspic96);

			if (dspic96.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;

			Iterator<DSPIC96> queueIterator = dspic96Queue.iterator();

			if (dspic96CalculationHashMap.containsKey(dspic96.getDate())) {
				tempDSPIC96Calculation = dspic96CalculationHashMap.get(dspic96.getDate());
			} else {
				tempDSPIC96Calculation.setToDate(dspic96.getDate());
			}

			while (queueIterator.hasNext()) {
				DSPIC96 temp = queueIterator.next();
				temp.setRocFlag(true);
				if (dspic96Queue.size() == 1) {
					roc = 0f;
					tempDSPIC96Calculation.setRoc(roc);
					tempDSPIC96Calculation.setToDate(dspic96.getDate());
					tempDSPIC96Calculation.setRocChangeSign(0);
				} else {
					roc = (dspic96.getValue() / ((LinkedList<DSPIC96>) dspic96Queue).get(0).getValue()) - 1;
					tempDSPIC96Calculation.setRoc(roc);
					tempDSPIC96Calculation.setToDate(dspic96.getDate());
				}

			}

			dspic96CalculationModified.add(tempDSPIC96Calculation);
		}

		dspic96List = dspic96Repostiory.saveAll(dspic96List);
		dspic96CalculationModified = dspic96CalculationRepository.saveAll(dspic96CalculationModified);
		logger.debug("Added new DSPIC96 row, " + dspic96CalculationModified);

		return;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRocRollingAnnualAvg() {

		if (NumberUtils.INTEGER_ZERO.equals(dspic96CalculationRepository.findAny())) {
			return;
		}

		System.out.println("calculateRocRollingAnnualAvg");
		Optional<List<DSPIC96Calculation>> dspic96CalculationListOpt = dspic96CalculationRepository
				.findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();
		Optional<List<DSPIC96Calculation>> prevDSPIC96CalculationListOpt = dspic96CalculationRepository
				.findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();
		List<DSPIC96Calculation> dspic96CalculationList = new ArrayList<>();

		if (dspic96CalculationListOpt.isPresent()) {
			dspic96CalculationList = dspic96CalculationListOpt.get();
			if (prevDSPIC96CalculationListOpt.isPresent()) {
				dspic96CalculationList.addAll(prevDSPIC96CalculationListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(dspic96CalculationList, new SortByDateDSPIC96Calculation());

		Queue<DSPIC96Calculation> dspic96CalculationPriorityQueue = new LinkedList<DSPIC96Calculation>();
		for (DSPIC96Calculation dspic96Calculation : dspic96CalculationList) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (dspic96CalculationPriorityQueue.size() == 4) {
				dspic96CalculationPriorityQueue.poll();
			}
			dspic96CalculationPriorityQueue.add(dspic96Calculation);

			if (dspic96Calculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<DSPIC96Calculation> queueIterator = dspic96CalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				DSPIC96Calculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			dspic96Calculation.setRocAnnRollAvgFlag(true);
			dspic96Calculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(dspic96CalculationList);
		dspic96CalculationList = dspic96CalculationRepository.saveAll(dspic96CalculationList);
		logger.info("New dspic96 calculation record inserted" + dspic96CalculationList);
		return;

	}

	/**
	 * Calculates Rolling Average of Three Month DSPIC96
	 *
	 * @return DSPIC96Calculation , updated DSPIC96Calculation Table
	 */
	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRollAvgThreeMonth() {

		if (NumberUtils.INTEGER_ZERO.equals(dspic96CalculationRepository.findAny())) {
			return;
		}

		System.out.println("calculateRollAvgThreeMonth");

		List<DSPIC96Calculation> dspic96CalculationList = new ArrayList<>();
		Optional<List<DSPIC96>> dspic96ListOpt = dspic96Repostiory.findByRollAverageFlagIsFalseOrderByDate();
		Optional<List<DSPIC96>> prevDSPIC96ListOpt = dspic96Repostiory
				.findTop2ByRollAverageFlagIsTrueOrderByDateDesc();
		List<DSPIC96Calculation> dspic96CalculationReference = dspic96CalculationRepository.findAll();
		HashMap<Date, DSPIC96Calculation> dspic96CalculationHashMap = new HashMap<>();
		List<DSPIC96> dspic96List = new ArrayList<>();

		for (DSPIC96Calculation dspic96Calculation : dspic96CalculationReference) {
			dspic96CalculationHashMap.put(dspic96Calculation.getToDate(), dspic96Calculation);
		}

		Queue<DSPIC96> dspic96Queue = new LinkedList<>();

		if (dspic96ListOpt.isPresent()) {
			dspic96List = dspic96ListOpt.get();
			if (prevDSPIC96ListOpt.isPresent()) {
				dspic96List.addAll(prevDSPIC96ListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(dspic96List, new SortByDateDSPIC96());

		for (DSPIC96 dspic96 : dspic96List) {

			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			if (dspic96Queue.size() == 3) {
				dspic96Queue.poll();
			}
			dspic96Queue.add(dspic96);
			if (dspic96.getRollAverageFlag()) {
				continue;
			}

			Iterator<DSPIC96> queueItr = dspic96Queue.iterator();

			DSPIC96Calculation tempDSPIC96Calculation = new DSPIC96Calculation();
			if (dspic96CalculationHashMap.containsKey(dspic96.getDate())) {
				tempDSPIC96Calculation = dspic96CalculationHashMap.get(dspic96.getDate());
			} else {
				tempDSPIC96Calculation.setToDate(dspic96.getDate());
			}

			while (queueItr.hasNext()) {
				DSPIC96 dspic96Val = queueItr.next();
				rollingAvg += dspic96Val.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			dspic96.setRollAverageFlag(true);
			tempDSPIC96Calculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			dspic96CalculationList.add(tempDSPIC96Calculation);

		}

		dspic96CalculationReference = dspic96CalculationRepository.saveAll(dspic96CalculationList);
		dspic96List = dspic96Repostiory.saveAll(dspic96List);
		return;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public List<DSPIC96> getLatestDSPIC96Records() {

		if (NumberUtils.INTEGER_ZERO.equals(dspic96CalculationRepository.findAny())) {
			return null;
		}
		System.out.println("getLatestDSPIC96Records");
		Optional<DSPIC96> lastRecordOpt = dspic96Repostiory.findTopByOrderByDateDesc();
		List<DSPIC96> response = new ArrayList<>();
		if (lastRecordOpt.isPresent()) {
			DSPIC96 lastRecord = lastRecordOpt.get();
			String lastDate = lastRecord.getDate().toString();
			String transactionUrl = QUANDL_HOST_URL + "FRED" + "/" + "DSPIC96" + "/" + QUANDL_DATA_FORMAT;

			UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(transactionUrl)
					// Add query parameter
					.queryParam("start_date", lastDate).queryParam("order", "ASC")
					.queryParam(QUANDL_API_KEY_NAME, QUANDL_API_KEY_VALUE);

			List<DSPIC96> DSPIC96List = new ArrayList<>();
			FederalResponse json = restUtility.consumeResponse(builder.toUriString());
			json.getDataset_data().getData().stream().forEach(o -> {
				ArrayList temp = (ArrayList) o;
				try {
					Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
					DSPIC96List.add(new DSPIC96(date, Float.parseFloat(temp.get(1).toString())));
					;
				} catch (ParseException e) {
					e.printStackTrace();
				}
			});

			if (DSPIC96List.size() > 1) { // As last record is already present in DB
				DSPIC96List.remove(0);
				response = dspic96Repostiory.saveAll(DSPIC96List);
				logger.info("New record inserted in DSPIC96");
			}

		}
		return response;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void updateRocChangeSignDgs10() {
		List<DSPIC96Calculation> dspic96CalculationList = dspic96CalculationRepository.findAllByRocIsNotNullAndRocChangeSignIsNull();
		DSPIC96Calculation lastUpdatedRecord = dspic96CalculationRepository.findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

		Collections.sort(dspic96CalculationList, new SortByDateDSPIC96Calculation());

		if(dspic96CalculationList.size() == 0){
			return;
		}

		Float lastRoc = lastUpdatedRecord.getRoc();
		for (DSPIC96Calculation dspic96Calculation : dspic96CalculationList) {
			if(dspic96Calculation.getRoc() < lastRoc){
				dspic96Calculation.setRocChangeSign(-1);
			}else if (dspic96Calculation.getRoc() > lastRoc){
				dspic96Calculation.setRocChangeSign(1);
			}else if(dspic96Calculation.getRoc() == lastRoc){
				dspic96Calculation.setRocChangeSign(0);
			}

			lastRoc = dspic96Calculation.getRoc();
		}

		dspic96CalculationRepository.saveAll(dspic96CalculationList);
	}

}
