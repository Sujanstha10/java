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
import com.automation.trading.domain.calculation.TEDRATECalculation;
import com.automation.trading.domain.fred.interestrates.TEDRATE;
import com.automation.trading.repository.TEDRATECalculationRepository;
import com.automation.trading.repository.TEDRATERepository;
import com.automation.trading.service.FederalInterestRateService;
import com.automation.trading.service.FederalInterestRateService.SortByDateTEDRATECalculation;
import com.automation.trading.service.TEDRATEService;

@Service
public class TEDRATEUpdateService {

	@Autowired
	private TEDRATERepository tedrateRepostiory;

	@Autowired
	private TEDRATECalculationRepository tedrateCalculationRepository;

	@Autowired
	private TEDRATEService tedrateRateOfChangeService;

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

	private Logger logger = LoggerFactory.getLogger(TEDRATEUpdateService.class);

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRoc() {

		if (NumberUtils.INTEGER_ZERO.equals(tedrateCalculationRepository.findAny())) {
			tedrateRateOfChangeService.calculateRoc();
			tedrateRateOfChangeService.updateRocChangeSignTEDRATE();
		}

		System.out.println("calculateRocRollingAnnualAvg");

		Optional<List<TEDRATE>> tedrateListOpt = tedrateRepostiory.findByRocFlagIsFalseOrderByDate();
		Optional<TEDRATE> prevTEDRATEOpt = tedrateRepostiory.findFirstByRocFlagIsTrueOrderByDateDesc();
		HashMap<Date, TEDRATECalculation> tedrateCalculationHashMap = new HashMap<>();
		TEDRATECalculation prevTEDRATECalculationRow = new TEDRATECalculation();

		List<TEDRATE> tedrateList = new ArrayList<>();

		if (tedrateListOpt.isPresent()) {
			tedrateList = tedrateListOpt.get();
			if (prevTEDRATEOpt.isPresent()) {
				tedrateList.add(prevTEDRATEOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(tedrateList, new FederalInterestRateService.SortByDateTEDRATE());
		List<TEDRATECalculation> tedrateCalculationReference = tedrateCalculationRepository.findAll();
		List<TEDRATECalculation> tedrateCalculationModified = new ArrayList<>();
		Queue<TEDRATE> tedrateQueue = new LinkedList<>();

		for (TEDRATECalculation tedrateCalculation : tedrateCalculationReference) {
			tedrateCalculationHashMap.put(tedrateCalculation.getToDate(), tedrateCalculation);
		}

		for (TEDRATE tedrate : tedrateList) {
			TEDRATECalculation tempTEDRATECalculation = new TEDRATECalculation();

			if (tedrateQueue.size() == 2) {
				tedrateQueue.poll();
			}
			tedrateQueue.add(tedrate);

			if (tedrate.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;

			Iterator<TEDRATE> queueIterator = tedrateQueue.iterator();

			if (tedrateCalculationHashMap.containsKey(tedrate.getDate())) {
				tempTEDRATECalculation = tedrateCalculationHashMap.get(tedrate.getDate());
			} else {
				tempTEDRATECalculation.setToDate(tedrate.getDate());
			}

			while (queueIterator.hasNext()) {
				TEDRATE temp = queueIterator.next();
				temp.setRocFlag(true);
				if (tedrateQueue.size() == 1) {
					roc = 0f;
					tempTEDRATECalculation.setRoc(roc);
					tempTEDRATECalculation.setToDate(tedrate.getDate());
					tempTEDRATECalculation.setRocChangeSign(0);
				} else {
					roc = (tedrate.getValue() / ((LinkedList<TEDRATE>) tedrateQueue).get(0).getValue()) - 1;
					tempTEDRATECalculation.setRoc(roc);
					tempTEDRATECalculation.setToDate(tedrate.getDate());
				}

			}

			tedrateCalculationModified.add(tempTEDRATECalculation);
		}

		tedrateList = tedrateRepostiory.saveAll(tedrateList);
		tedrateCalculationModified = tedrateCalculationRepository.saveAll(tedrateCalculationModified);
		logger.debug("Added new TEDRATE row, " + tedrateCalculationModified);

		return;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRocRollingAnnualAvg() {

		if (NumberUtils.INTEGER_ZERO.equals(tedrateCalculationRepository.findAny())) {
			return;
		}

		System.out.println("calculateRocRollingAnnualAvg");
		Optional<List<TEDRATECalculation>> tedrateCalculationListOpt = tedrateCalculationRepository
				.findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();
		Optional<List<TEDRATECalculation>> prevTEDRATECalculationListOpt = tedrateCalculationRepository
				.findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();
		List<TEDRATECalculation> tedrateCalculationList = new ArrayList<>();

		if (tedrateCalculationListOpt.isPresent()) {
			tedrateCalculationList = tedrateCalculationListOpt.get();
			if (prevTEDRATECalculationListOpt.isPresent()) {
				tedrateCalculationList.addAll(prevTEDRATECalculationListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(tedrateCalculationList, new SortByDateTEDRATECalculation());

		Queue<TEDRATECalculation> tedrateCalculationPriorityQueue = new LinkedList<TEDRATECalculation>();
		for (TEDRATECalculation tedrateCalculation : tedrateCalculationList) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (tedrateCalculationPriorityQueue.size() == 4) {
				tedrateCalculationPriorityQueue.poll();
			}
			tedrateCalculationPriorityQueue.add(tedrateCalculation);

			if (tedrateCalculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<TEDRATECalculation> queueIterator = tedrateCalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				TEDRATECalculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			tedrateCalculation.setRocAnnRollAvgFlag(true);
			tedrateCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(tedrateCalculationList);
		tedrateCalculationList = tedrateCalculationRepository.saveAll(tedrateCalculationList);
		logger.info("New tedrate calculation record inserted" + tedrateCalculationList);
		return;

	}

	/**
	 * Calculates Rolling Average of Three Month TEDRATE
	 *
	 * @return TEDRATECalculation , updated TEDRATECalculation Table
	 */
	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRollAvgThreeMonth() {

		if (NumberUtils.INTEGER_ZERO.equals(tedrateCalculationRepository.findAny())) {
			return;
		}

		System.out.println("calculateRollAvgThreeMonth");

		List<TEDRATECalculation> tedrateCalculationList = new ArrayList<>();
		Optional<List<TEDRATE>> tedrateListOpt = tedrateRepostiory.findByRollAverageFlagIsFalseOrderByDate();
		Optional<List<TEDRATE>> prevTEDRATEListOpt = tedrateRepostiory.findTop2ByRollAverageFlagIsTrueOrderByDateDesc();
		List<TEDRATECalculation> tedrateCalculationReference = tedrateCalculationRepository.findAll();
		HashMap<Date, TEDRATECalculation> tedrateCalculationHashMap = new HashMap<>();
		List<TEDRATE> tedrateList = new ArrayList<>();

		for (TEDRATECalculation tedrateCalculation : tedrateCalculationReference) {
			tedrateCalculationHashMap.put(tedrateCalculation.getToDate(), tedrateCalculation);
		}

		Queue<TEDRATE> tedrateQueue = new LinkedList<>();

		if (tedrateListOpt.isPresent()) {
			tedrateList = tedrateListOpt.get();
			if (prevTEDRATEListOpt.isPresent()) {
				tedrateList.addAll(prevTEDRATEListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(tedrateList, new FederalInterestRateService.SortByDateTEDRATE());

		for (TEDRATE tedrate : tedrateList) {

			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			if (tedrateQueue.size() == 3) {
				tedrateQueue.poll();
			}
			tedrateQueue.add(tedrate);
			if (tedrate.getRollAverageFlag()) {
				continue;
			}

			Iterator<TEDRATE> queueItr = tedrateQueue.iterator();

			TEDRATECalculation tempTEDRATECalculation = new TEDRATECalculation();
			if (tedrateCalculationHashMap.containsKey(tedrate.getDate())) {
				tempTEDRATECalculation = tedrateCalculationHashMap.get(tedrate.getDate());
			} else {
				tempTEDRATECalculation.setToDate(tedrate.getDate());
			}

			while (queueItr.hasNext()) {
				TEDRATE tedrateVal = queueItr.next();
				rollingAvg += tedrateVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			tedrate.setRollAverageFlag(true);
			tempTEDRATECalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			tedrateCalculationList.add(tempTEDRATECalculation);

		}

		tedrateCalculationReference = tedrateCalculationRepository.saveAll(tedrateCalculationList);
		tedrateList = tedrateRepostiory.saveAll(tedrateList);
		return;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public List<TEDRATE> getLatestTEDRATERecords() {

		if (NumberUtils.INTEGER_ZERO.equals(tedrateCalculationRepository.findAny())) {
			return null;
		}
		System.out.println("getLatestTEDRATERecords");
		Optional<TEDRATE> lastRecordOpt = tedrateRepostiory.findTopByOrderByDateDesc();
		List<TEDRATE> response = new ArrayList<>();
		if (lastRecordOpt.isPresent()) {
			TEDRATE lastRecord = lastRecordOpt.get();
			String lastDate = lastRecord.getDate().toString();
			String transactionUrl = QUANDL_HOST_URL + "FRED" + "/" + "TEDRATE" + "/" + QUANDL_DATA_FORMAT;

			UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(transactionUrl)
					// Add query parameter
					.queryParam("start_date", lastDate).queryParam("order", "ASC")
					.queryParam(QUANDL_API_KEY_NAME, QUANDL_API_KEY_VALUE);

			List<TEDRATE> TEDRATEList = new ArrayList<>();
			FederalResponse json = restUtility.consumeResponse(builder.toUriString());
			json.getDataset_data().getData().stream().forEach(o -> {
				ArrayList temp = (ArrayList) o;
				try {
					Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
					TEDRATEList.add(new TEDRATE(date, Float.parseFloat(temp.get(1).toString())));
					;
				} catch (ParseException e) {
					e.printStackTrace();
				}
			});

			if (TEDRATEList.size() > 1) { // As last record is already present in DB
				TEDRATEList.remove(0);
				response = tedrateRepostiory.saveAll(TEDRATEList);
				logger.info("New record inserted in TEDRATE");
			}

		}
		return response;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void updateRocChangeSignTEDRATE() {
		List<TEDRATECalculation> tedrateCalculationList = tedrateCalculationRepository
				.findAllByRocIsNotNullAndRocChangeSignIsNull();
		TEDRATECalculation lastUpdatedRecord = tedrateCalculationRepository
				.findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

		Collections.sort(tedrateCalculationList, new SortByDateTEDRATECalculation());

		if (tedrateCalculationList.size() == 0) {
			return;
		}

		Float lastRoc = lastUpdatedRecord.getRoc();
		for (TEDRATECalculation tedrateCalculation : tedrateCalculationList) {
			if (tedrateCalculation.getRoc() < lastRoc) {
				tedrateCalculation.setRocChangeSign(-1);
			} else if (tedrateCalculation.getRoc() > lastRoc) {
				tedrateCalculation.setRocChangeSign(1);
			} else if (tedrateCalculation.getRoc() == lastRoc) {
				tedrateCalculation.setRocChangeSign(0);
			}

			lastRoc = tedrateCalculation.getRoc();
		}

		tedrateCalculationRepository.saveAll(tedrateCalculationList);
	}

}
