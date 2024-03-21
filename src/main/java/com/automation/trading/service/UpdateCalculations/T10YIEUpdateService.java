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
import com.automation.trading.domain.calculation.T10YIECalculation;
import com.automation.trading.domain.fred.interestrates.T10YIE;
import com.automation.trading.repository.T10YIECalculationRepository;
import com.automation.trading.repository.T10YIERepository;
import com.automation.trading.service.FederalInterestRateService;
import com.automation.trading.service.FederalInterestRateService.SortByDateT10YIECalculation;
import com.automation.trading.service.T10YIEService;

@Service
public class T10YIEUpdateService {

	@Autowired
	private T10YIERepository t10yieRepostiory;

	@Autowired
	private T10YIECalculationRepository t10yieCalculationRepository;

	@Autowired
	private T10YIEService t10yieRateOfChangeService;

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

	private Logger logger = LoggerFactory.getLogger(T10YIEUpdateService.class);

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRoc() {

		if (NumberUtils.INTEGER_ZERO.equals(t10yieCalculationRepository.findAny())) {
			t10yieRateOfChangeService.calculateRoc();
			t10yieRateOfChangeService.updateRocChangeSignT10YIE();
		}

		System.out.println("calculateRocRollingAnnualAvg");

		Optional<List<T10YIE>> t10yieListOpt = t10yieRepostiory.findByRocFlagIsFalseOrderByDate();
		Optional<T10YIE> prevT10YIEOpt = t10yieRepostiory.findFirstByRocFlagIsTrueOrderByDateDesc();
		HashMap<Date, T10YIECalculation> t10yieCalculationHashMap = new HashMap<>();
		T10YIECalculation prevT10YIECalculationRow = new T10YIECalculation();

		List<T10YIE> t10yieList = new ArrayList<>();

		if (t10yieListOpt.isPresent()) {
			t10yieList = t10yieListOpt.get();
			if (prevT10YIEOpt.isPresent()) {
				t10yieList.add(prevT10YIEOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(t10yieList, new FederalInterestRateService.SortByDateT10YIE());
		List<T10YIECalculation> t10yieCalculationReference = t10yieCalculationRepository.findAll();
		List<T10YIECalculation> t10yieCalculationModified = new ArrayList<>();
		Queue<T10YIE> t10yieQueue = new LinkedList<>();

		for (T10YIECalculation t10yieCalculation : t10yieCalculationReference) {
			t10yieCalculationHashMap.put(t10yieCalculation.getToDate(), t10yieCalculation);
		}

		for (T10YIE t10yie : t10yieList) {
			T10YIECalculation tempT10YIECalculation = new T10YIECalculation();

			if (t10yieQueue.size() == 2) {
				t10yieQueue.poll();
			}
			t10yieQueue.add(t10yie);

			if (t10yie.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;

			Iterator<T10YIE> queueIterator = t10yieQueue.iterator();

			if (t10yieCalculationHashMap.containsKey(t10yie.getDate())) {
				tempT10YIECalculation = t10yieCalculationHashMap.get(t10yie.getDate());
			} else {
				tempT10YIECalculation.setToDate(t10yie.getDate());
			}

			while (queueIterator.hasNext()) {
				T10YIE temp = queueIterator.next();
				temp.setRocFlag(true);
				if (t10yieQueue.size() == 1) {
					roc = 0f;
					tempT10YIECalculation.setRoc(roc);
					tempT10YIECalculation.setToDate(t10yie.getDate());
					tempT10YIECalculation.setRocChangeSign(0);
				} else {
					roc = (t10yie.getValue() / ((LinkedList<T10YIE>) t10yieQueue).get(0).getValue()) - 1;
					tempT10YIECalculation.setRoc(roc);
					tempT10YIECalculation.setToDate(t10yie.getDate());
				}

			}

			t10yieCalculationModified.add(tempT10YIECalculation);
		}

		t10yieList = t10yieRepostiory.saveAll(t10yieList);
		t10yieCalculationModified = t10yieCalculationRepository.saveAll(t10yieCalculationModified);
		logger.debug("Added new T10YIE row, " + t10yieCalculationModified);

		return;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRocRollingAnnualAvg() {

		if (NumberUtils.INTEGER_ZERO.equals(t10yieCalculationRepository.findAny())) {
			return;
		}

		System.out.println("calculateRocRollingAnnualAvg");
		Optional<List<T10YIECalculation>> t10yieCalculationListOpt = t10yieCalculationRepository
				.findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();
		Optional<List<T10YIECalculation>> prevT10YIECalculationListOpt = t10yieCalculationRepository
				.findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();
		List<T10YIECalculation> t10yieCalculationList = new ArrayList<>();

		if (t10yieCalculationListOpt.isPresent()) {
			t10yieCalculationList = t10yieCalculationListOpt.get();
			if (prevT10YIECalculationListOpt.isPresent()) {
				t10yieCalculationList.addAll(prevT10YIECalculationListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(t10yieCalculationList, new SortByDateT10YIECalculation());

		Queue<T10YIECalculation> t10yieCalculationPriorityQueue = new LinkedList<T10YIECalculation>();
		for (T10YIECalculation t10yieCalculation : t10yieCalculationList) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (t10yieCalculationPriorityQueue.size() == 4) {
				t10yieCalculationPriorityQueue.poll();
			}
			t10yieCalculationPriorityQueue.add(t10yieCalculation);

			if (t10yieCalculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<T10YIECalculation> queueIterator = t10yieCalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				T10YIECalculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			t10yieCalculation.setRocAnnRollAvgFlag(true);
			t10yieCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(t10yieCalculationList);
		t10yieCalculationList = t10yieCalculationRepository.saveAll(t10yieCalculationList);
		logger.info("New t10yie calculation record inserted" + t10yieCalculationList);
		return;

	}

	/**
	 * Calculates Rolling Average of Three Month T10YIE
	 *
	 * @return T10YIECalculation , updated T10YIECalculation Table
	 */
	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRollAvgThreeMonth() {

		if (NumberUtils.INTEGER_ZERO.equals(t10yieCalculationRepository.findAny())) {
			return;
		}

		System.out.println("calculateRollAvgThreeMonth");

		List<T10YIECalculation> t10yieCalculationList = new ArrayList<>();
		Optional<List<T10YIE>> t10yieListOpt = t10yieRepostiory.findByRollAverageFlagIsFalseOrderByDate();
		Optional<List<T10YIE>> prevT10YIEListOpt = t10yieRepostiory.findTop2ByRollAverageFlagIsTrueOrderByDateDesc();
		List<T10YIECalculation> t10yieCalculationReference = t10yieCalculationRepository.findAll();
		HashMap<Date, T10YIECalculation> t10yieCalculationHashMap = new HashMap<>();
		List<T10YIE> t10yieList = new ArrayList<>();

		for (T10YIECalculation t10yieCalculation : t10yieCalculationReference) {
			t10yieCalculationHashMap.put(t10yieCalculation.getToDate(), t10yieCalculation);
		}

		Queue<T10YIE> t10yieQueue = new LinkedList<>();

		if (t10yieListOpt.isPresent()) {
			t10yieList = t10yieListOpt.get();
			if (prevT10YIEListOpt.isPresent()) {
				t10yieList.addAll(prevT10YIEListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(t10yieList, new FederalInterestRateService.SortByDateT10YIE());

		for (T10YIE t10yie : t10yieList) {

			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			if (t10yieQueue.size() == 3) {
				t10yieQueue.poll();
			}
			t10yieQueue.add(t10yie);
			if (t10yie.getRollAverageFlag()) {
				continue;
			}

			Iterator<T10YIE> queueItr = t10yieQueue.iterator();

			T10YIECalculation tempT10YIECalculation = new T10YIECalculation();
			if (t10yieCalculationHashMap.containsKey(t10yie.getDate())) {
				tempT10YIECalculation = t10yieCalculationHashMap.get(t10yie.getDate());
			} else {
				tempT10YIECalculation.setToDate(t10yie.getDate());
			}

			while (queueItr.hasNext()) {
				T10YIE t10yieVal = queueItr.next();
				rollingAvg += t10yieVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			t10yie.setRollAverageFlag(true);
			tempT10YIECalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			t10yieCalculationList.add(tempT10YIECalculation);

		}

		t10yieCalculationReference = t10yieCalculationRepository.saveAll(t10yieCalculationList);
		t10yieList = t10yieRepostiory.saveAll(t10yieList);
		return;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public List<T10YIE> getLatestT10YIERecords() {

		if (NumberUtils.INTEGER_ZERO.equals(t10yieCalculationRepository.findAny())) {
			return null;
		}
		System.out.println("getLatestT10YIERecords");
		Optional<T10YIE> lastRecordOpt = t10yieRepostiory.findTopByOrderByDateDesc();
		List<T10YIE> response = new ArrayList<>();
		if (lastRecordOpt.isPresent()) {
			T10YIE lastRecord = lastRecordOpt.get();
			String lastDate = lastRecord.getDate().toString();
			String transactionUrl = QUANDL_HOST_URL + "FRED" + "/" + "T10YIE" + "/" + QUANDL_DATA_FORMAT;

			UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(transactionUrl)
					// Add query parameter
					.queryParam("start_date", lastDate).queryParam("order", "ASC")
					.queryParam(QUANDL_API_KEY_NAME, QUANDL_API_KEY_VALUE);

			List<T10YIE> T10YIEList = new ArrayList<>();
			FederalResponse json = restUtility.consumeResponse(builder.toUriString());
			json.getDataset_data().getData().stream().forEach(o -> {
				ArrayList temp = (ArrayList) o;
				try {
					Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
					T10YIEList.add(new T10YIE(date, Float.parseFloat(temp.get(1).toString())));
					;
				} catch (ParseException e) {
					e.printStackTrace();
				}
			});

			if (T10YIEList.size() > 1) { // As last record is already present in DB
				T10YIEList.remove(0);
				response = t10yieRepostiory.saveAll(T10YIEList);
				logger.info("New record inserted in T10YIE");
			}

		}
		return response;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void updateRocChangeSignT10YIE() {
		List<T10YIECalculation> t10yieCalculationList = t10yieCalculationRepository
				.findAllByRocIsNotNullAndRocChangeSignIsNull();
		T10YIECalculation lastUpdatedRecord = t10yieCalculationRepository
				.findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

		Collections.sort(t10yieCalculationList, new SortByDateT10YIECalculation());

		if (t10yieCalculationList.size() == 0) {
			return;
		}

		Float lastRoc = lastUpdatedRecord.getRoc();
		for (T10YIECalculation t10yieCalculation : t10yieCalculationList) {
			if (t10yieCalculation.getRoc() < lastRoc) {
				t10yieCalculation.setRocChangeSign(-1);
			} else if (t10yieCalculation.getRoc() > lastRoc) {
				t10yieCalculation.setRocChangeSign(1);
			} else if (t10yieCalculation.getRoc() == lastRoc) {
				t10yieCalculation.setRocChangeSign(0);
			}

			lastRoc = t10yieCalculation.getRoc();
		}

		t10yieCalculationRepository.saveAll(t10yieCalculationList);
	}

}
