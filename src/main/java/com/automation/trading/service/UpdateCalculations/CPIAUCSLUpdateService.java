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

import com.automation.trading.domain.calculation.CPIAUCSLCalculation;
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
import com.automation.trading.domain.fred.CPIAUCSL;
import com.automation.trading.repository.CPIAUCSLCalculationRepository;
import com.automation.trading.repository.CPIAUCSLRepository;
import com.automation.trading.service.CPIAUCSLService;
import com.automation.trading.service.FederalReserveService;
import com.automation.trading.utility.RestUtility;

@Service
public class CPIAUCSLUpdateService {

	@Autowired
	CPIAUCSLRepository cpiaucslRepository;

	@Autowired
	CPIAUCSLCalculationRepository cpiaucslCalculationRepository;

	@Autowired
	CPIAUCSLService cpiaucslService;

	@Autowired
	RestTemplate restTemplate;

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

	@Autowired
	RestUtility restUtility;

	private Logger logger = LoggerFactory.getLogger(CPIAUCSLUpdateService.class);


	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRoc() {

		if (NumberUtils.INTEGER_ZERO.equals(cpiaucslCalculationRepository.findAny())) {
			cpiaucslService.calculateRoc();
			cpiaucslService.updateRocChangeSignDff();
		}

		System.out.println("calculateRocRollingAnnualAvg");

		Optional<List<CPIAUCSL>> cpiaucslListOpt = cpiaucslRepository.findByRocFlagIsFalseOrderByDate();
		Optional<CPIAUCSL> prevUnRateOpt = cpiaucslRepository.findFirstByRocFlagIsTrueOrderByDateDesc();
		HashMap<Date, CPIAUCSLCalculation> cpiaucslCalculationHashMap = new HashMap<>();


		List<CPIAUCSL> cpiaucslList = new ArrayList<>();

		if (cpiaucslListOpt.isPresent()) {
			cpiaucslList = cpiaucslListOpt.get();
			if (prevUnRateOpt.isPresent()) {
				cpiaucslList.add(prevUnRateOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(cpiaucslList, new FederalReserveService.SortByDateCPIAUCSL());
		List<CPIAUCSLCalculation> cpiaucslCalculationReference = cpiaucslCalculationRepository.findAll();
		List<CPIAUCSLCalculation> cpiaucslCalculationModified = new ArrayList<>();
		Queue<CPIAUCSL> cpiaucslQueue = new LinkedList<>();

		for (CPIAUCSLCalculation cpiaucslCalculation : cpiaucslCalculationReference) {
			cpiaucslCalculationHashMap.put(cpiaucslCalculation.getToDate(), cpiaucslCalculation);
		}

		for (CPIAUCSL cpiaucsl : cpiaucslList) {
			CPIAUCSLCalculation tempCPIAUCSLCalculation = new CPIAUCSLCalculation();
			if (cpiaucslQueue.size() == 2) {
				cpiaucslQueue.poll();
			}
			cpiaucslQueue.add(cpiaucsl);

			if (cpiaucsl.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;

			Iterator<CPIAUCSL> queueIterator = cpiaucslQueue.iterator();

			if (cpiaucslCalculationHashMap.containsKey(cpiaucsl.getDate())) {
				tempCPIAUCSLCalculation = cpiaucslCalculationHashMap.get(cpiaucsl.getDate());
			} else {
				tempCPIAUCSLCalculation.setToDate(cpiaucsl.getDate());
			}

			while (queueIterator.hasNext()) {
				CPIAUCSL temp = queueIterator.next();
				temp.setRocFlag(true);
				if (cpiaucslQueue.size() == 1) {
					roc = 0f;
					tempCPIAUCSLCalculation.setRoc(roc);
					tempCPIAUCSLCalculation.setToDate(cpiaucsl.getDate());
					tempCPIAUCSLCalculation.setRocChangeSign(0);
				} else {
					roc = (cpiaucsl.getValue() / ((LinkedList<CPIAUCSL>) cpiaucslQueue).get(0).getValue()) - 1;
					tempCPIAUCSLCalculation.setRoc(roc);
					tempCPIAUCSLCalculation.setToDate(cpiaucsl.getDate());
				}

			}

			cpiaucslCalculationModified.add(tempCPIAUCSLCalculation);
		}

		cpiaucslList = cpiaucslRepository.saveAll(cpiaucslList);
		cpiaucslCalculationModified = cpiaucslCalculationRepository.saveAll(cpiaucslCalculationModified);
		return;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRocRollingAnnualAvg() {

		if (NumberUtils.INTEGER_ZERO.equals(cpiaucslCalculationRepository.findAny())) {
			return;
		}

		Optional<List<CPIAUCSLCalculation>> cpiaucslCalculationListOpt = cpiaucslCalculationRepository
				.findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();

		Optional<List<CPIAUCSLCalculation>> prevCPIAUCSLCalculationListOpt = cpiaucslCalculationRepository
				.findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();
		List<CPIAUCSLCalculation> cpiaucslCalculationList = new ArrayList<>();

		if (cpiaucslCalculationListOpt.isPresent()) {
			cpiaucslCalculationList = cpiaucslCalculationListOpt.get();
			if (prevCPIAUCSLCalculationListOpt.isPresent()) {
				cpiaucslCalculationList.addAll(prevCPIAUCSLCalculationListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(cpiaucslCalculationList, new FederalReserveService.SortByDateCPIAUCSLCalculation());

		Queue<CPIAUCSLCalculation> cpiaucslCalculationPriorityQueue = new LinkedList<>();
		for (CPIAUCSLCalculation cpiaucslCalculation : cpiaucslCalculationList) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (cpiaucslCalculationPriorityQueue.size() == 4) {
				cpiaucslCalculationPriorityQueue.poll();
			}
			cpiaucslCalculationPriorityQueue.add(cpiaucslCalculation);

			if (cpiaucslCalculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<CPIAUCSLCalculation> queueIterator = cpiaucslCalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				CPIAUCSLCalculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			cpiaucslCalculation.setRocAnnRollAvgFlag(true);
			cpiaucslCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(cpiaucslCalculationList);
		cpiaucslCalculationList = cpiaucslCalculationRepository.saveAll(cpiaucslCalculationList);
		logger.info("New cpiaucsl calculation record inserted" + cpiaucslCalculationList);
		return;

	}

	/**
	 * Calculates Rolling Average of Three Month CPIAUCSL
	 * 
	 * @return CPIAUCSLCalculation , updated CPIAUCSLCalculation Table
	 */
	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRollAvgThreeMonth() {

		if (NumberUtils.INTEGER_ZERO.equals(cpiaucslCalculationRepository.findAny())) {
			return;
		}

		List<CPIAUCSLCalculation> cpiaucslCalculationList = new ArrayList<>();
		Optional<List<CPIAUCSL>> cpiaucslListOpt = cpiaucslRepository.findByRollAverageFlagIsFalseOrderByDate();
		Optional<List<CPIAUCSL>> prevCPIAUCSLListOpt = cpiaucslRepository
				.findTop2ByRollAverageFlagIsTrueOrderByDateDesc();
		List<CPIAUCSLCalculation> cpiaucslCalculationReference = cpiaucslCalculationRepository.findAll();
		HashMap<Date, CPIAUCSLCalculation> cpiaucslCalculationHashMap = new HashMap<>();
		List<CPIAUCSL> cpiaucslList = new ArrayList<>();

		for (CPIAUCSLCalculation cpiaucslCalculation : cpiaucslCalculationReference) {
			cpiaucslCalculationHashMap.put(cpiaucslCalculation.getToDate(), cpiaucslCalculation);
		}

		Queue<CPIAUCSL> cpiaucslQueue = new LinkedList<>();

		if (cpiaucslListOpt.isPresent()) {
			cpiaucslList = cpiaucslListOpt.get();
			if (prevCPIAUCSLListOpt.isPresent()) {
				cpiaucslList.addAll(prevCPIAUCSLListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(cpiaucslList, new FederalReserveService.SortByDateCPIAUCSL());

		for (CPIAUCSL cpiaucsl : cpiaucslList) {

			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			if (cpiaucslQueue.size() == 3) {
				cpiaucslQueue.poll();
			}
			cpiaucslQueue.add(cpiaucsl);
			if (cpiaucsl.getRollAverageFlag()) {
				continue;
			}

			Iterator<CPIAUCSL> queueItr = cpiaucslQueue.iterator();

			CPIAUCSLCalculation tempCPIAUCSLCalculation = new CPIAUCSLCalculation();
			if (cpiaucslCalculationHashMap.containsKey(cpiaucsl.getDate())) {
				tempCPIAUCSLCalculation = cpiaucslCalculationHashMap.get(cpiaucsl.getDate());
			} else {
				tempCPIAUCSLCalculation.setToDate(cpiaucsl.getDate());
			}

			while (queueItr.hasNext()) {
				CPIAUCSL cpiaucslVal = queueItr.next();
				rollingAvg += cpiaucslVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			cpiaucsl.setRollAverageFlag(true);
			tempCPIAUCSLCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			cpiaucslCalculationList.add(tempCPIAUCSLCalculation);

		}

		cpiaucslCalculationReference = cpiaucslCalculationRepository.saveAll(cpiaucslCalculationList);
		cpiaucslList = cpiaucslRepository.saveAll(cpiaucslList);
		return;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public List<CPIAUCSL> getLatestCPIAUCSLRecords() {

		if (NumberUtils.INTEGER_ZERO.equals(cpiaucslCalculationRepository.findAny())) {
			return null;
		}
		Optional<CPIAUCSL> lastRecordOpt = cpiaucslRepository.findTopByOrderByDateDesc();
		List<CPIAUCSL> response = new ArrayList<>();
		if (lastRecordOpt.isPresent()) {
			CPIAUCSL lastRecord = lastRecordOpt.get();
			String lastDate = lastRecord.getDate().toString();
			String transactionUrl = QUANDL_HOST_URL + "FRED" + "/" + "CPIAUCSL" + "/" + QUANDL_DATA_FORMAT;

			UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(transactionUrl)
					// Add query parameter
					.queryParam("start_date", lastDate).queryParam("order", "ASC")
					.queryParam(QUANDL_API_KEY_NAME, QUANDL_API_KEY_VALUE);

			List<CPIAUCSL> CPIAUCSLList = new ArrayList<>();
			FederalResponse json = restUtility.consumeResponse(builder.toUriString());
			json.getDataset_data().getData().stream().forEach(o -> {
				ArrayList temp = (ArrayList) o;
				try {
					Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
					CPIAUCSLList.add(new CPIAUCSL(date, Float.parseFloat(temp.get(1).toString())));
					;
				} catch (ParseException e) {
					e.printStackTrace();
				}
			});

			if (CPIAUCSLList.size() > 1) { // As last record is already present in DB
				CPIAUCSLList.remove(0);
				response = cpiaucslRepository.saveAll(CPIAUCSLList);
				logger.info("New record inserted in CPIAUCSL");
			}

		}
		return response;
	}


	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void updateRocChangeSignCPIAUCSL() {
		List<CPIAUCSLCalculation> cpiaucslCalculationList = cpiaucslCalculationRepository.findAllByRocIsNotNullAndRocChangeSignIsNull();
		CPIAUCSLCalculation lastUpdatedRecord = cpiaucslCalculationRepository.findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

		Collections.sort(cpiaucslCalculationList, new FederalReserveService.SortByDateCPIAUCSLCalculation());
		if(cpiaucslCalculationList.size() == 0){
			return;
		}

		Float lastRoc = lastUpdatedRecord.getRoc();
		for (CPIAUCSLCalculation cpiaucslCalculation : cpiaucslCalculationList) {
			if(cpiaucslCalculation.getRoc() < lastRoc){
				cpiaucslCalculation.setRocChangeSign(-1);
			}else if (cpiaucslCalculation.getRoc() > lastRoc){
				cpiaucslCalculation.setRocChangeSign(1);
			}else if(cpiaucslCalculation.getRoc() == lastRoc){
				cpiaucslCalculation.setRocChangeSign(0);
			}

			lastRoc = cpiaucslCalculation.getRoc();
		}

		cpiaucslCalculationRepository.saveAll(cpiaucslCalculationList);
	}

}
