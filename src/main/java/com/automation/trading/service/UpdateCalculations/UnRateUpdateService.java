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
import com.automation.trading.domain.calculation.UnRateCalculation;
import com.automation.trading.domain.fred.UnRate;
import com.automation.trading.repository.UnRateCalculationRepository;
import com.automation.trading.repository.UnRateRepostiory;
import com.automation.trading.service.FederalReserveService.SortByDateUnRateCalculation;
import com.automation.trading.service.FederalReserveService.SortByDateUnrate;
import com.automation.trading.service.UnRateRateOfChangeService;

@Service
public class UnRateUpdateService {

	@Autowired
	private UnRateRepostiory unrateRepostiory;

	@Autowired
	private UnRateCalculationRepository unrateCalculationRepository;

	@Autowired
	private UnRateRateOfChangeService unrateRateOfChangeService;

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

	private Logger logger = LoggerFactory.getLogger(UnRateUpdateService.class);

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRoc() {

		if (NumberUtils.INTEGER_ZERO.equals(unrateCalculationRepository.findAny())) {
			unrateRateOfChangeService.calculateRoc();
			unrateRateOfChangeService.updateRocChangeSignUnRate();
		}

		System.out.println("calculateRocRollingAnnualAvg");

		Optional<List<UnRate>> unrateListOpt = unrateRepostiory.findByRocFlagIsFalseOrderByDate();
		Optional<UnRate> prevUnRateOpt = unrateRepostiory.findFirstByRocFlagIsTrueOrderByDateDesc();
		HashMap<Date, UnRateCalculation> unrateCalculationHashMap = new HashMap<>();
		UnRateCalculation prevUnRateCalculationRow = new UnRateCalculation();

		List<UnRate> unrateList = new ArrayList<>();

		if (unrateListOpt.isPresent()) {
			unrateList = unrateListOpt.get();
			if (prevUnRateOpt.isPresent()) {
				unrateList.add(prevUnRateOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(unrateList, new SortByDateUnrate());
		List<UnRateCalculation> unrateCalculationReference = unrateCalculationRepository.findAll();
		List<UnRateCalculation> unrateCalculationModified = new ArrayList<>();
		Queue<UnRate> unrateQueue = new LinkedList<>();

		for (UnRateCalculation unrateCalculation : unrateCalculationReference) {
			unrateCalculationHashMap.put(unrateCalculation.getToDate(), unrateCalculation);
		}

		for (UnRate unrate : unrateList) {
			UnRateCalculation tempUnRateCalculation = new UnRateCalculation();

			if (unrateQueue.size() == 2) {
				unrateQueue.poll();
			}
			unrateQueue.add(unrate);

			if (unrate.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;

			Iterator<UnRate> queueIterator = unrateQueue.iterator();

			if (unrateCalculationHashMap.containsKey(unrate.getDate())) {
				tempUnRateCalculation = unrateCalculationHashMap.get(unrate.getDate());
			} else {
				tempUnRateCalculation.setToDate(unrate.getDate());
			}

			while (queueIterator.hasNext()) {
				UnRate temp = queueIterator.next();
				temp.setRocFlag(true);
				if (unrateQueue.size() == 1) {
					roc = 0f;
					tempUnRateCalculation.setRoc(roc);
					tempUnRateCalculation.setToDate(unrate.getDate());
					tempUnRateCalculation.setRocChangeSign(0);
				} else {
					roc = (unrate.getValue() / ((LinkedList<UnRate>) unrateQueue).get(0).getValue()) - 1;
					tempUnRateCalculation.setRoc(roc);
					tempUnRateCalculation.setToDate(unrate.getDate());
				}

			}

			unrateCalculationModified.add(tempUnRateCalculation);
		}

		unrateList = unrateRepostiory.saveAll(unrateList);
		unrateCalculationModified = unrateCalculationRepository.saveAll(unrateCalculationModified);
		logger.debug("Added new UnRate row, " + unrateCalculationModified);

		return;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRocRollingAnnualAvg() {

		if (NumberUtils.INTEGER_ZERO.equals(unrateCalculationRepository.findAny())) {
			return;
		}

		System.out.println("calculateRocRollingAnnualAvg");
		Optional<List<UnRateCalculation>> unrateCalculationListOpt = unrateCalculationRepository
				.findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();
		Optional<List<UnRateCalculation>> prevUnRateCalculationListOpt = unrateCalculationRepository
				.findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();
		List<UnRateCalculation> unrateCalculationList = new ArrayList<>();

		if (unrateCalculationListOpt.isPresent()) {
			unrateCalculationList = unrateCalculationListOpt.get();
			if (prevUnRateCalculationListOpt.isPresent()) {
				unrateCalculationList.addAll(prevUnRateCalculationListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(unrateCalculationList, new SortByDateUnRateCalculation());

		Queue<UnRateCalculation> unrateCalculationPriorityQueue = new LinkedList<UnRateCalculation>();
		for (UnRateCalculation unrateCalculation : unrateCalculationList) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (unrateCalculationPriorityQueue.size() == 4) {
				unrateCalculationPriorityQueue.poll();
			}
			unrateCalculationPriorityQueue.add(unrateCalculation);

			if (unrateCalculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<UnRateCalculation> queueIterator = unrateCalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				UnRateCalculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			unrateCalculation.setRocAnnRollAvgFlag(true);
			unrateCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(unrateCalculationList);
		unrateCalculationList = unrateCalculationRepository.saveAll(unrateCalculationList);
		logger.info("New unrate calculation record inserted" + unrateCalculationList);
		return;

	}

	/**
	 * Calculates Rolling Average of Three Month UnRate
	 *
	 * @return UnRateCalculation , updated UnRateCalculation Table
	 */
	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRollAvgThreeMonth() {

		if (NumberUtils.INTEGER_ZERO.equals(unrateCalculationRepository.findAny())) {
			return;
		}

		System.out.println("calculateRollAvgThreeMonth");

		List<UnRateCalculation> unrateCalculationList = new ArrayList<>();
		Optional<List<UnRate>> unrateListOpt = unrateRepostiory.findByRollAverageFlagIsFalseOrderByDate();
		Optional<List<UnRate>> prevUnRateListOpt = unrateRepostiory.findTop2ByRollAverageFlagIsTrueOrderByDateDesc();
		List<UnRateCalculation> unrateCalculationReference = unrateCalculationRepository.findAll();
		HashMap<Date, UnRateCalculation> unrateCalculationHashMap = new HashMap<>();
		List<UnRate> unrateList = new ArrayList<>();

		for (UnRateCalculation unrateCalculation : unrateCalculationReference) {
			unrateCalculationHashMap.put(unrateCalculation.getToDate(), unrateCalculation);
		}

		Queue<UnRate> unrateQueue = new LinkedList<>();

		if (unrateListOpt.isPresent()) {
			unrateList = unrateListOpt.get();
			if (prevUnRateListOpt.isPresent()) {
				unrateList.addAll(prevUnRateListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(unrateList, new SortByDateUnrate());

		for (UnRate unrate : unrateList) {

			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			if (unrateQueue.size() == 3) {
				unrateQueue.poll();
			}
			unrateQueue.add(unrate);
			if (unrate.getRollAverageFlag()) {
				continue;
			}

			Iterator<UnRate> queueItr = unrateQueue.iterator();

			UnRateCalculation tempUnRateCalculation = new UnRateCalculation();
			if (unrateCalculationHashMap.containsKey(unrate.getDate())) {
				tempUnRateCalculation = unrateCalculationHashMap.get(unrate.getDate());
			} else {
				tempUnRateCalculation.setToDate(unrate.getDate());
			}

			while (queueItr.hasNext()) {
				UnRate unrateVal = queueItr.next();
				rollingAvg += unrateVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			unrate.setRollAverageFlag(true);
			tempUnRateCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			unrateCalculationList.add(tempUnRateCalculation);

		}

		unrateCalculationReference = unrateCalculationRepository.saveAll(unrateCalculationList);
		unrateList = unrateRepostiory.saveAll(unrateList);
		return;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public List<UnRate> getLatestUnRateRecords() {

		if (NumberUtils.INTEGER_ZERO.equals(unrateCalculationRepository.findAny())) {
			return null;
		}
		System.out.println("getLatestUnRateRecords");
		Optional<UnRate> lastRecordOpt = unrateRepostiory.findTopByOrderByDateDesc();
		List<UnRate> response = new ArrayList<>();
		if (lastRecordOpt.isPresent()) {
			UnRate lastRecord = lastRecordOpt.get();
			String lastDate = lastRecord.getDate().toString();
			String transactionUrl = QUANDL_HOST_URL + "FRED" + "/" + "UnRate" + "/" + QUANDL_DATA_FORMAT;

			UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(transactionUrl)
					// Add query parameter
					.queryParam("start_date", lastDate).queryParam("order", "ASC")
					.queryParam(QUANDL_API_KEY_NAME, QUANDL_API_KEY_VALUE);

			List<UnRate> UnRateList = new ArrayList<>();
			FederalResponse json = restUtility.consumeResponse(builder.toUriString());
			json.getDataset_data().getData().stream().forEach(o -> {
				ArrayList temp = (ArrayList) o;
				try {
					Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
					UnRateList.add(new UnRate(date, Float.parseFloat(temp.get(1).toString())));
					;
				} catch (ParseException e) {
					e.printStackTrace();
				}
			});

			if (UnRateList.size() > 1) { // As last record is already present in DB
				UnRateList.remove(0);
				response = unrateRepostiory.saveAll(UnRateList);
				logger.info("New record inserted in UnRate");
			}

		}
		return response;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void updateRocChangeSignUnRate() {
		List<UnRateCalculation> unrateCalculationList = unrateCalculationRepository
				.findAllByRocIsNotNullAndRocChangeSignIsNull();
		UnRateCalculation lastUpdatedRecord = unrateCalculationRepository
				.findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

		Collections.sort(unrateCalculationList, new SortByDateUnRateCalculation());

		if (unrateCalculationList.size() == 0) {
			return;
		}

		Float lastRoc = lastUpdatedRecord.getRoc();
		for (UnRateCalculation unrateCalculation : unrateCalculationList) {
			if (unrateCalculation.getRoc() < lastRoc) {
				unrateCalculation.setRocChangeSign(-1);
			} else if (unrateCalculation.getRoc() > lastRoc) {
				unrateCalculation.setRocChangeSign(1);
			} else if (unrateCalculation.getRoc() == lastRoc) {
				unrateCalculation.setRocChangeSign(0);
			}

			lastRoc = unrateCalculation.getRoc();
		}

		unrateCalculationRepository.saveAll(unrateCalculationList);
	}

}
