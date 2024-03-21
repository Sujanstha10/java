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

import com.automation.trading.domain.fred.GDPC1;
import com.automation.trading.service.FederalInterestRateService;
import com.automation.trading.service.FederalReserveService;
import com.automation.trading.utility.RestUtility;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.automation.trading.common.FederalResponse;
import com.automation.trading.domain.calculation.GDPC1Calculation;
import com.automation.trading.repository.GDPC1CalculationRepository;
import com.automation.trading.repository.GDPC1Repository;
import com.automation.trading.service.GDPC1Service;

@Service
public class GDPC1UpdateService {

	@Autowired
	private GDPC1Repository gdpc1Repostiory;

	@Autowired
	private GDPC1CalculationRepository gdpc1CalculationRepository;

	@Autowired
	private GDPC1Service gdpc1RateOfChangeService;

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

	private Logger logger = LoggerFactory.getLogger(GDPC1UpdateService.class);

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRoc() {

		if (NumberUtils.INTEGER_ZERO.equals(gdpc1CalculationRepository.findAny())) {
			gdpc1RateOfChangeService.calculateRoc();
			gdpc1RateOfChangeService.updateRocChangeSignGDPC1();
		}

		System.out.println("calculateRocRollingAnnualAvg");

		Optional<List<GDPC1>> gdpc1ListOpt = gdpc1Repostiory.findByRocFlagIsFalseOrderByDate();
		Optional<GDPC1> prevGDPC1Opt = gdpc1Repostiory.findFirstByRocFlagIsTrueOrderByDateDesc();
		HashMap<Date, GDPC1Calculation> gdpc1CalculationHashMap = new HashMap<>();
		GDPC1Calculation prevGDPC1CalculationRow = new GDPC1Calculation();

		List<GDPC1> gdpc1List = new ArrayList<>();

		if (gdpc1ListOpt.isPresent()) {
			gdpc1List = gdpc1ListOpt.get();
			if (prevGDPC1Opt.isPresent()) {
				gdpc1List.add(prevGDPC1Opt.get());
			}
		} else {
			return;
		}

		Collections.sort(gdpc1List, new FederalReserveService.SortByDateGDPC1());
		List<GDPC1Calculation> gdpc1CalculationReference = gdpc1CalculationRepository.findAll();
		List<GDPC1Calculation> gdpc1CalculationModified = new ArrayList<>();
		Queue<GDPC1> gdpc1Queue = new LinkedList<>();

		for (GDPC1Calculation gdpc1Calculation : gdpc1CalculationReference) {
			gdpc1CalculationHashMap.put(gdpc1Calculation.getToDate(), gdpc1Calculation);
		}

		for (GDPC1 gdpc1 : gdpc1List) {
			GDPC1Calculation tempGDPC1Calculation = new GDPC1Calculation();

			if (gdpc1Queue.size() == 2) {
				gdpc1Queue.poll();
			}
			gdpc1Queue.add(gdpc1);

			if (gdpc1.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;

			Iterator<GDPC1> queueIterator = gdpc1Queue.iterator();

			if (gdpc1CalculationHashMap.containsKey(gdpc1.getDate())) {
				tempGDPC1Calculation = gdpc1CalculationHashMap.get(gdpc1.getDate());
			} else {
				tempGDPC1Calculation.setToDate(gdpc1.getDate());
			}

			while (queueIterator.hasNext()) {
				GDPC1 temp = queueIterator.next();
				temp.setRocFlag(true);
				if (gdpc1Queue.size() == 1) {
					roc = 0f;
					tempGDPC1Calculation.setRoc(roc);
					tempGDPC1Calculation.setToDate(gdpc1.getDate());
					tempGDPC1Calculation.setRocChangeSign(0);
				} else {
					roc = (gdpc1.getValue() / ((LinkedList<GDPC1>) gdpc1Queue).get(0).getValue()) - 1;
					tempGDPC1Calculation.setRoc(roc);
					tempGDPC1Calculation.setToDate(gdpc1.getDate());
				}

			}

			gdpc1CalculationModified.add(tempGDPC1Calculation);
		}

		gdpc1List = gdpc1Repostiory.saveAll(gdpc1List);
		gdpc1CalculationModified = gdpc1CalculationRepository.saveAll(gdpc1CalculationModified);
		logger.debug("Added new GDPC1 row, " + gdpc1CalculationModified);

		return;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRocRollingAnnualAvg() {

		if (NumberUtils.INTEGER_ZERO.equals(gdpc1CalculationRepository.findAny())) {
			return;
		}

		System.out.println("calculateRocRollingAnnualAvg");
		Optional<List<GDPC1Calculation>> gdpc1CalculationListOpt = gdpc1CalculationRepository
				.findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();
		Optional<List<GDPC1Calculation>> prevGDPC1CalculationListOpt = gdpc1CalculationRepository
				.findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();
		List<GDPC1Calculation> gdpc1CalculationList = new ArrayList<>();

		if (gdpc1CalculationListOpt.isPresent()) {
			gdpc1CalculationList = gdpc1CalculationListOpt.get();
			if (prevGDPC1CalculationListOpt.isPresent()) {
				gdpc1CalculationList.addAll(prevGDPC1CalculationListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(gdpc1CalculationList, new FederalReserveService.SortByDateGDPC1Calculation());

		Queue<GDPC1Calculation> gdpc1CalculationPriorityQueue = new LinkedList<GDPC1Calculation>();
		for (GDPC1Calculation gdpc1Calculation : gdpc1CalculationList) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (gdpc1CalculationPriorityQueue.size() == 4) {
				gdpc1CalculationPriorityQueue.poll();
			}
			gdpc1CalculationPriorityQueue.add(gdpc1Calculation);

			if (gdpc1Calculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<GDPC1Calculation> queueIterator = gdpc1CalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				GDPC1Calculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			gdpc1Calculation.setRocAnnRollAvgFlag(true);
			gdpc1Calculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(gdpc1CalculationList);
		gdpc1CalculationList = gdpc1CalculationRepository.saveAll(gdpc1CalculationList);
		logger.info("New gdpc1 calculation record inserted" + gdpc1CalculationList);
		return;

	}

	/**
	 * Calculates Rolling Average of Three Month GDPC1
	 *
	 * @return GDPC1Calculation , updated GDPC1Calculation Table
	 */
	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRollAvgThreeMonth() {

		if (NumberUtils.INTEGER_ZERO.equals(gdpc1CalculationRepository.findAny())) {
			return;
		}

		System.out.println("calculateRollAvgThreeMonth");

		List<GDPC1Calculation> gdpc1CalculationList = new ArrayList<>();
		Optional<List<GDPC1>> gdpc1ListOpt = gdpc1Repostiory.findByRollAverageFlagIsFalseOrderByDate();
		Optional<List<GDPC1>> prevGDPC1ListOpt = gdpc1Repostiory
				.findTop2ByRollAverageFlagIsTrueOrderByDateDesc();
		List<GDPC1Calculation> gdpc1CalculationReference = gdpc1CalculationRepository.findAll();
		HashMap<Date, GDPC1Calculation> gdpc1CalculationHashMap = new HashMap<>();
		List<GDPC1> gdpc1List = new ArrayList<>();

		for (GDPC1Calculation gdpc1Calculation : gdpc1CalculationReference) {
			gdpc1CalculationHashMap.put(gdpc1Calculation.getToDate(), gdpc1Calculation);
		}

		Queue<GDPC1> gdpc1Queue = new LinkedList<>();

		if (gdpc1ListOpt.isPresent()) {
			gdpc1List = gdpc1ListOpt.get();
			if (prevGDPC1ListOpt.isPresent()) {
				gdpc1List.addAll(prevGDPC1ListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(gdpc1List, new FederalReserveService.SortByDateGDPC1());

		for (GDPC1 gdpc1 : gdpc1List) {

			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			if (gdpc1Queue.size() == 3) {
				gdpc1Queue.poll();
			}
			gdpc1Queue.add(gdpc1);
			if (gdpc1.getRollAverageFlag()) {
				continue;
			}

			Iterator<GDPC1> queueItr = gdpc1Queue.iterator();

			GDPC1Calculation tempGDPC1Calculation = new GDPC1Calculation();
			if (gdpc1CalculationHashMap.containsKey(gdpc1.getDate())) {
				tempGDPC1Calculation = gdpc1CalculationHashMap.get(gdpc1.getDate());
			} else {
				tempGDPC1Calculation.setToDate(gdpc1.getDate());
			}

			while (queueItr.hasNext()) {
				GDPC1 gdpc1Val = queueItr.next();
				rollingAvg += gdpc1Val.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			gdpc1.setRollAverageFlag(true);
			tempGDPC1Calculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			gdpc1CalculationList.add(tempGDPC1Calculation);

		}

		gdpc1CalculationReference = gdpc1CalculationRepository.saveAll(gdpc1CalculationList);
		gdpc1List = gdpc1Repostiory.saveAll(gdpc1List);
		return;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public List<GDPC1> getLatestGDPC1Records() {

		if (NumberUtils.INTEGER_ZERO.equals(gdpc1CalculationRepository.findAny())) {
			return null;
		}
		System.out.println("getLatestGDPC1Records");
		Optional<GDPC1> lastRecordOpt = gdpc1Repostiory.findTopByOrderByDateDesc();
		List<GDPC1> response = new ArrayList<>();
		if (lastRecordOpt.isPresent()) {
			GDPC1 lastRecord = lastRecordOpt.get();
			String lastDate = lastRecord.getDate().toString();
			String transactionUrl = QUANDL_HOST_URL + "FRED" + "/" + "GDPC1" + "/" + QUANDL_DATA_FORMAT;

			UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(transactionUrl)
					// Add query parameter
					.queryParam("start_date", lastDate).queryParam("order", "ASC")
					.queryParam(QUANDL_API_KEY_NAME, QUANDL_API_KEY_VALUE);

			List<GDPC1> GDPC1List = new ArrayList<>();
			FederalResponse json = restUtility.consumeResponse(builder.toUriString());
			json.getDataset_data().getData().stream().forEach(o -> {
				ArrayList temp = (ArrayList) o;
				try {
					Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
					GDPC1List.add(new GDPC1(date, Float.parseFloat(temp.get(1).toString())));
					;
				} catch (ParseException e) {
					e.printStackTrace();
				}
			});

			if (GDPC1List.size() > 1) { // As last record is already present in DB
				GDPC1List.remove(0);
				response = gdpc1Repostiory.saveAll(GDPC1List);
				logger.info("New record inserted in GDPC1");
			}

		}
		return response;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void updateRocChangeSignDgs10() {
		List<GDPC1Calculation> gdpc1CalculationList = gdpc1CalculationRepository.findAllByRocIsNotNullAndRocChangeSignIsNull();
		GDPC1Calculation lastUpdatedRecord = gdpc1CalculationRepository.findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

		Collections.sort(gdpc1CalculationList, new FederalReserveService.SortByDateGDPC1Calculation());

		if(gdpc1CalculationList.size() == 0){
			return;
		}

		Float lastRoc = lastUpdatedRecord.getRoc();
		for (GDPC1Calculation gdpc1Calculation : gdpc1CalculationList) {
			if(gdpc1Calculation.getRoc() < lastRoc){
				gdpc1Calculation.setRocChangeSign(-1);
			}else if (gdpc1Calculation.getRoc() > lastRoc){
				gdpc1Calculation.setRocChangeSign(1);
			}else if(gdpc1Calculation.getRoc() == lastRoc){
				gdpc1Calculation.setRocChangeSign(0);
			}

			lastRoc = gdpc1Calculation.getRoc();
		}

		gdpc1CalculationRepository.saveAll(gdpc1CalculationList);
	}


}
