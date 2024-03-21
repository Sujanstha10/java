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

import com.automation.trading.domain.fred.DPRIME;
import com.automation.trading.service.FederalInterestRateService;
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
import com.automation.trading.domain.calculation.DPRIMECalculation;
import com.automation.trading.repository.DPRIMECalculationRepository;
import com.automation.trading.repository.DPRIMERepository;
import com.automation.trading.service.DPRIMEService;

@Service
public class DPRIMEUpdateService {

	@Autowired
	private DPRIMERepository dprimeRepostiory;

	@Autowired
	private DPRIMECalculationRepository dprimeCalculationRepository;

	@Autowired
	private DPRIMEService dprimeRateOfChangeService;

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

	private Logger logger = LoggerFactory.getLogger(DPRIMEUpdateService.class);

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRoc() {

		if (NumberUtils.INTEGER_ZERO.equals(dprimeCalculationRepository.findAny())) {
			dprimeRateOfChangeService.calculateRoc();
			dprimeRateOfChangeService.updateRocChangeSignDPRIME();
		}

		System.out.println("calculateRocRollingAnnualAvg");

		Optional<List<DPRIME>> dprimeListOpt = dprimeRepostiory.findByRocFlagIsFalseOrderByDate();
		Optional<DPRIME> prevDPRIMEOpt = dprimeRepostiory.findFirstByRocFlagIsTrueOrderByDateDesc();
		HashMap<Date, DPRIMECalculation> dprimeCalculationHashMap = new HashMap<>();
		DPRIMECalculation prevDPRIMECalculationRow = new DPRIMECalculation();

		List<DPRIME> dprimeList = new ArrayList<>();

		if (dprimeListOpt.isPresent()) {
			dprimeList = dprimeListOpt.get();
			if (prevDPRIMEOpt.isPresent()) {
				dprimeList.add(prevDPRIMEOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(dprimeList, new FederalInterestRateService.SortByDateDPRIME());
		List<DPRIMECalculation> dprimeCalculationReference = dprimeCalculationRepository.findAll();
		List<DPRIMECalculation> dprimeCalculationModified = new ArrayList<>();
		Queue<DPRIME> dprimeQueue = new LinkedList<>();

		for (DPRIMECalculation dprimeCalculation : dprimeCalculationReference) {
			dprimeCalculationHashMap.put(dprimeCalculation.getToDate(), dprimeCalculation);
		}

		for (DPRIME dprime : dprimeList) {
			DPRIMECalculation tempDPRIMECalculation = new DPRIMECalculation();

			if (dprimeQueue.size() == 2) {
				dprimeQueue.poll();
			}
			dprimeQueue.add(dprime);

			if (dprime.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;

			Iterator<DPRIME> queueIterator = dprimeQueue.iterator();

			if (dprimeCalculationHashMap.containsKey(dprime.getDate())) {
				tempDPRIMECalculation = dprimeCalculationHashMap.get(dprime.getDate());
			} else {
				tempDPRIMECalculation.setToDate(dprime.getDate());
			}

			while (queueIterator.hasNext()) {
				DPRIME temp = queueIterator.next();
				temp.setRocFlag(true);
				if (dprimeQueue.size() == 1) {
					roc = 0f;
					tempDPRIMECalculation.setRoc(roc);
					tempDPRIMECalculation.setToDate(dprime.getDate());
					tempDPRIMECalculation.setRocChangeSign(0);
				} else {
					roc = (dprime.getValue() / ((LinkedList<DPRIME>) dprimeQueue).get(0).getValue()) - 1;
					tempDPRIMECalculation.setRoc(roc);
					tempDPRIMECalculation.setToDate(dprime.getDate());
				}

			}

			dprimeCalculationModified.add(tempDPRIMECalculation);
		}

		dprimeList = dprimeRepostiory.saveAll(dprimeList);
		dprimeCalculationModified = dprimeCalculationRepository.saveAll(dprimeCalculationModified);
		logger.debug("Added new DPRIME row, " + dprimeCalculationModified);

		return;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRocRollingAnnualAvg() {

		if (NumberUtils.INTEGER_ZERO.equals(dprimeCalculationRepository.findAny())) {
			return;
		}

		System.out.println("calculateRocRollingAnnualAvg");
		Optional<List<DPRIMECalculation>> dprimeCalculationListOpt = dprimeCalculationRepository
				.findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();
		Optional<List<DPRIMECalculation>> prevDPRIMECalculationListOpt = dprimeCalculationRepository
				.findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();
		List<DPRIMECalculation> dprimeCalculationList = new ArrayList<>();

		if (dprimeCalculationListOpt.isPresent()) {
			dprimeCalculationList = dprimeCalculationListOpt.get();
			if (prevDPRIMECalculationListOpt.isPresent()) {
				dprimeCalculationList.addAll(prevDPRIMECalculationListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(dprimeCalculationList, new FederalInterestRateService.SortByDateDPRIMECalculation());

		Queue<DPRIMECalculation> dprimeCalculationPriorityQueue = new LinkedList<DPRIMECalculation>();
		for (DPRIMECalculation dprimeCalculation : dprimeCalculationList) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (dprimeCalculationPriorityQueue.size() == 4) {
				dprimeCalculationPriorityQueue.poll();
			}
			dprimeCalculationPriorityQueue.add(dprimeCalculation);

			if (dprimeCalculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<DPRIMECalculation> queueIterator = dprimeCalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				DPRIMECalculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			dprimeCalculation.setRocAnnRollAvgFlag(true);
			dprimeCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(dprimeCalculationList);
		dprimeCalculationList = dprimeCalculationRepository.saveAll(dprimeCalculationList);
		logger.info("New dprime calculation record inserted" + dprimeCalculationList);
		return;

	}

	/**
	 * Calculates Rolling Average of Three Month DPRIME
	 *
	 * @return DPRIMECalculation , updated DPRIMECalculation Table
	 */
	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRollAvgThreeMonth() {

		if (NumberUtils.INTEGER_ZERO.equals(dprimeCalculationRepository.findAny())) {
			return;
		}

		System.out.println("calculateRollAvgThreeMonth");

		List<DPRIMECalculation> dprimeCalculationList = new ArrayList<>();
		Optional<List<DPRIME>> dprimeListOpt = dprimeRepostiory.findByRollAverageFlagIsFalseOrderByDate();
		Optional<List<DPRIME>> prevDPRIMEListOpt = dprimeRepostiory
				.findTop2ByRollAverageFlagIsTrueOrderByDateDesc();
		List<DPRIMECalculation> dprimeCalculationReference = dprimeCalculationRepository.findAll();
		HashMap<Date, DPRIMECalculation> dprimeCalculationHashMap = new HashMap<>();
		List<DPRIME> dprimeList = new ArrayList<>();

		for (DPRIMECalculation dprimeCalculation : dprimeCalculationReference) {
			dprimeCalculationHashMap.put(dprimeCalculation.getToDate(), dprimeCalculation);
		}

		Queue<DPRIME> dprimeQueue = new LinkedList<>();

		if (dprimeListOpt.isPresent()) {
			dprimeList = dprimeListOpt.get();
			if (prevDPRIMEListOpt.isPresent()) {
				dprimeList.addAll(prevDPRIMEListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(dprimeList, new FederalInterestRateService.SortByDateDPRIME());

		for (DPRIME dprime : dprimeList) {

			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			if (dprimeQueue.size() == 3) {
				dprimeQueue.poll();
			}
			dprimeQueue.add(dprime);
			if (dprime.getRollAverageFlag()) {
				continue;
			}

			Iterator<DPRIME> queueItr = dprimeQueue.iterator();

			DPRIMECalculation tempDPRIMECalculation = new DPRIMECalculation();
			if (dprimeCalculationHashMap.containsKey(dprime.getDate())) {
				tempDPRIMECalculation = dprimeCalculationHashMap.get(dprime.getDate());
			} else {
				tempDPRIMECalculation.setToDate(dprime.getDate());
			}

			while (queueItr.hasNext()) {
				DPRIME dprimeVal = queueItr.next();
				rollingAvg += dprimeVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			dprime.setRollAverageFlag(true);
			tempDPRIMECalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			dprimeCalculationList.add(tempDPRIMECalculation);

		}

		dprimeCalculationReference = dprimeCalculationRepository.saveAll(dprimeCalculationList);
		dprimeList = dprimeRepostiory.saveAll(dprimeList);
		return;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public List<DPRIME> getLatestDPRIMERecords() {

		if (NumberUtils.INTEGER_ZERO.equals(dprimeCalculationRepository.findAny())) {
			return null;
		}
		System.out.println("getLatestDPRIMERecords");
		Optional<DPRIME> lastRecordOpt = dprimeRepostiory.findTopByOrderByDateDesc();
		List<DPRIME> response = new ArrayList<>();
		if (lastRecordOpt.isPresent()) {
			DPRIME lastRecord = lastRecordOpt.get();
			String lastDate = lastRecord.getDate().toString();
			String transactionUrl = QUANDL_HOST_URL + "FRED" + "/" + "DPRIME" + "/" + QUANDL_DATA_FORMAT;

			UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(transactionUrl)
					// Add query parameter
					.queryParam("start_date", lastDate).queryParam("order", "ASC")
					.queryParam(QUANDL_API_KEY_NAME, QUANDL_API_KEY_VALUE);

			List<DPRIME> DPRIMEList = new ArrayList<>();
			FederalResponse json = restUtility.consumeResponse(builder.toUriString());
			json.getDataset_data().getData().stream().forEach(o -> {
				ArrayList temp = (ArrayList) o;
				try {
					Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
					DPRIMEList.add(new DPRIME(date, Float.parseFloat(temp.get(1).toString())));
					;
				} catch (ParseException e) {
					e.printStackTrace();
				}
			});

			if (DPRIMEList.size() > 1) { // As last record is already present in DB
				DPRIMEList.remove(0);
				response = dprimeRepostiory.saveAll(DPRIMEList);
				logger.info("New record inserted in DPRIME");
			}

		}
		return response;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void updateRocChangeSignDgs10() {
		List<DPRIMECalculation> dprimeCalculationList = dprimeCalculationRepository.findAllByRocIsNotNullAndRocChangeSignIsNull();
		DPRIMECalculation lastUpdatedRecord = dprimeCalculationRepository.findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

		Collections.sort(dprimeCalculationList, new FederalInterestRateService.SortByDateDPRIMECalculation());

		if(dprimeCalculationList.size() == 0){
			return;
		}

		Float lastRoc = lastUpdatedRecord.getRoc();
		for (DPRIMECalculation dprimeCalculation : dprimeCalculationList) {
			if(dprimeCalculation.getRoc() < lastRoc){
				dprimeCalculation.setRocChangeSign(-1);
			}else if (dprimeCalculation.getRoc() > lastRoc){
				dprimeCalculation.setRocChangeSign(1);
			}else if(dprimeCalculation.getRoc() == lastRoc){
				dprimeCalculation.setRocChangeSign(0);
			}

			lastRoc = dprimeCalculation.getRoc();
		}

		dprimeCalculationRepository.saveAll(dprimeCalculationList);
	}

}
