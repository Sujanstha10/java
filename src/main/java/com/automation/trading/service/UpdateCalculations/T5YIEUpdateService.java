//package com.automation.trading.service.UpdateCalculations;
//
//import java.text.ParseException;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.Iterator;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Optional;
//import java.util.Queue;
//
//import org.apache.commons.lang3.math.NumberUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.HttpEntity;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpMethod;
//import org.springframework.http.MediaType;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//import org.springframework.web.util.UriComponentsBuilder;
//
//import com.automation.trading.common.FederalResponse;
//import com.automation.trading.domain.calculation.T5YIECalculation;
//import com.automation.trading.domain.fred.interestrates.T5YIE;
//import com.automation.trading.repository.T5YIECalculationRepository;
//import com.automation.trading.repository.T5YIERepository;
//import com.automation.trading.service.FederalInterestRateService.SortByDateT5YIE;
//import com.automation.trading.service.FederalInterestRateService.SortByDateT5YIECalculation;
//import com.automation.trading.service.T5YIEService;
//
//@Service
//public class T5YIEUpdateService {
//
//	@Autowired
//	private T5YIERepository t5yieRepostiory;
//	@Autowired
//	private T5YIECalculationRepository t5yieCalculationRepository;
//	@Autowired
//	private T5YIEService t5yieRateOfChangeService;
//	@Autowired
//	RestTemplate restTemplate;
//
//	@Value("${quandl.host.url}")
//	private String QUANDL_HOST_URL;
//
//	@Value("${quandl.api.key.value}")
//	private String QUANDL_API_KEY_VALUE;
//
//	@Value("${quandl.api.key.name}")
//	private String QUANDL_API_KEY_NAME;
//
//	@Value("${quandl.data.format}")
//	private String QUANDL_DATA_FORMAT;
//
//	private Logger logger = LoggerFactory.getLogger(GdpUpdateService.class);
//
//	@Async
//	@Scheduled(fixedDelay = 1000 * 60)
//	public void calculateRoc() {
//
//		if (NumberUtils.INTEGER_ZERO.equals(t5yieCalculationRepository.findAny())) {
//			t5yieRateOfChangeService.calculateRoc();
//			t5yieRateOfChangeService.updateRocChangeSignT5YIE();
//		}
//
//		System.out.println("calculateRocRollingAnnualAvg");
//
//		Optional<List<T5YIE>> t5yieListOpt = t5yieRepostiory.findByRocFlagIsFalseOrderByDate();
//		Optional<T5YIE> prevT5YIEOpt = t5yieRepostiory.findFirstByRocFlagIsTrueOrderByDateDesc();
//		HashMap<Date, T5YIECalculation> t5yieCalculationHashMap = new HashMap<>();
//		T5YIECalculation tempT5YIECalculation = new T5YIECalculation();
//		T5YIECalculation prevT5YIECalculationRow = new T5YIECalculation();
//
//		List<T5YIE> t5yieList = new ArrayList<>();
//
//		if (t5yieListOpt.isPresent()) {
//			t5yieList = t5yieListOpt.get();
//			if (prevT5YIEOpt.isPresent()) {
//				t5yieList.add(prevT5YIEOpt.get());
//			}
//		} else {
//			return;
//		}
//
//		Collections.sort(t5yieList, new SortByDateT5YIE());
//		List<T5YIECalculation> t5yieCalculationReference = t5yieCalculationRepository.findAll();
//		List<T5YIECalculation> t5yieCalculationModified = new ArrayList<>();
//		Queue<T5YIE> t5yieQueue = new LinkedList<>();
//
//		for (T5YIECalculation t5yieCalculation : t5yieCalculationReference) {
//			t5yieCalculationHashMap.put(t5yieCalculation.getToDate(), t5yieCalculation);
//		}
//
//		for (T5YIE t5yie : t5yieList) {
//			if (t5yieQueue.size() == 2) {
//				t5yieQueue.poll();
//			}
//			t5yieQueue.add(t5yie);
//
//			if (t5yie.getRocFlag()) {
//				continue;
//			}
//			Float roc = 0.0f;
//
//			Iterator<T5YIE> queueIterator = t5yieQueue.iterator();
//
//			if (t5yieCalculationHashMap.containsKey(t5yie.getDate())) {
//				tempT5YIECalculation = t5yieCalculationHashMap.get(t5yie.getDate());
//			} else {
//				tempT5YIECalculation.setToDate(t5yie.getDate());
//			}
//
//			while (queueIterator.hasNext()) {
//				T5YIE temp = queueIterator.next();
//				temp.setRocFlag(true);
//				if (t5yieQueue.size() == 1) {
//					roc = 0f;
//					tempT5YIECalculation.setRoc(roc);
//					tempT5YIECalculation.setToDate(t5yie.getDate());
//					tempT5YIECalculation.setRocChangeSign(0);
//				} else {
//					roc = (t5yie.getValue() / ((LinkedList<T5YIE>) t5yieQueue).get(0).getValue()) - 1;
//					tempT5YIECalculation.setRoc(roc);
//					tempT5YIECalculation.setToDate(t5yie.getDate());
//					prevT5YIECalculationRow = t5yieCalculationHashMap
//							.get(((LinkedList<T5YIE>) t5yieQueue).get(0).getDate());
//
//					if (prevT5YIECalculationRow.getRoc() < tempT5YIECalculation.getRoc()) {
//						tempT5YIECalculation.setRocChangeSign(1);
//					} else if (prevT5YIECalculationRow.getRoc() > tempT5YIECalculation.getRoc()) {
//						tempT5YIECalculation.setRocChangeSign(-1);
//					} else {
//						tempT5YIECalculation.setRocChangeSign(0);
//					}
//				}
//
//			}
//
//			t5yieCalculationModified.add(tempT5YIECalculation);
//		}
//
//		t5yieList = t5yieRepostiory.saveAll(t5yieList);
//		t5yieCalculationModified = t5yieCalculationRepository.saveAll(t5yieCalculationModified);
//		logger.debug("Added new T5YIE row, " + t5yieCalculationModified);
//
//		return;
//	}
//
//	@Async
//	@Scheduled(fixedDelay = 1000 * 60)
//	public void calculateRocRollingAnnualAvg() {
//
//		if (NumberUtils.INTEGER_ZERO.equals(t5yieCalculationRepository.findAny())) {
//			return;
//		}
//
//		System.out.println("calculateRocRollingAnnualAvg");
//		Optional<List<T5YIECalculation>> t5yieCalculationListOpt = t5yieCalculationRepository
//				.findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();
//		Optional<List<T5YIECalculation>> prevT5YIECalculationListOpt = t5yieCalculationRepository
//				.findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();
//		List<T5YIECalculation> t5yieCalculationList = new ArrayList<>();
//
//		if (t5yieCalculationListOpt.isPresent()) {
//			t5yieCalculationList = t5yieCalculationListOpt.get();
//			if (prevT5YIECalculationListOpt.isPresent()) {
//				t5yieCalculationList.addAll(prevT5YIECalculationListOpt.get());
//			}
//		} else {
//			return;
//		}
//
//		Collections.sort(t5yieCalculationList, new SortByDateT5YIECalculation());
//
//		Queue<T5YIECalculation> t5yieCalculationPriorityQueue = new LinkedList<T5YIECalculation>();
//		for (T5YIECalculation t5yieCalculation : t5yieCalculationList) {
//			Float rocFourMonth = 0.0f;
//			Float rocFourMonthAvg = 0.0f;
//			int period = 0;
//			if (t5yieCalculationPriorityQueue.size() == 4) {
//				t5yieCalculationPriorityQueue.poll();
//			}
//			t5yieCalculationPriorityQueue.add(t5yieCalculation);
//
//			if (t5yieCalculation.getRocAnnRollAvgFlag()) {
//				continue;
//			}
//			Iterator<T5YIECalculation> queueIterator = t5yieCalculationPriorityQueue.iterator();
//			while (queueIterator.hasNext()) {
//				T5YIECalculation temp = queueIterator.next();
//				rocFourMonth += temp.getRoc();
//				period++;
//			}
//			rocFourMonthAvg = rocFourMonth / period;
//			t5yieCalculation.setRocAnnRollAvgFlag(true);
//			t5yieCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
//		}
//		System.out.println(t5yieCalculationList);
//		t5yieCalculationList = t5yieCalculationRepository.saveAll(t5yieCalculationList);
//		logger.info("New t5yie calculation record inserted" + t5yieCalculationList);
//		return;
//
//	}
//
//	/**
//	 * Calculates Rolling Average of Three Month T5YIE
//	 *
//	 * @return T5YIECalculation , updated T5YIECalculation Table
//	 */
//	@Async
//	@Scheduled(fixedDelay = 1000 * 60)
//	public void calculateRollAvgThreeMonth() {
//
//		if (NumberUtils.INTEGER_ZERO.equals(t5yieCalculationRepository.findAny())) {
//			return;
//		}
//
//		System.out.println("calculateRollAvgThreeMonth");
//
//		List<T5YIECalculation> t5yieCalculationList = new ArrayList<>();
//		Optional<List<T5YIE>> t5yieListOpt = t5yieRepostiory.findByRollAverageFlagIsFalseOrderByDate();
//		Optional<List<T5YIE>> prevT5YIEListOpt = t5yieRepostiory.findTop2ByRollAverageFlagIsTrueOrderByDateDesc();
//		List<T5YIECalculation> t5yieCalculationReference = t5yieCalculationRepository.findAll();
//		HashMap<Date, T5YIECalculation> t5yieCalculationHashMap = new HashMap<>();
//		List<T5YIE> t5yieList = new ArrayList<>();
//
//		for (T5YIECalculation t5yieCalculation : t5yieCalculationReference) {
//			t5yieCalculationHashMap.put(t5yieCalculation.getToDate(), t5yieCalculation);
//		}
//
//		Queue<T5YIE> t5yieQueue = new LinkedList<>();
//
//		if (t5yieListOpt.isPresent()) {
//			t5yieList = t5yieListOpt.get();
//			if (prevT5YIEListOpt.isPresent()) {
//				t5yieList.addAll(prevT5YIEListOpt.get());
//			}
//		} else {
//			return;
//		}
//
//		Collections.sort(t5yieList, new SortByDateT5YIE());
//
//		for (T5YIE t5yie : t5yieList) {
//
//			Float rollingAvg = 0.0f;
//			Float rollingAvgThreeMon = 0f;
//			int period = 0;
//
//			if (t5yieQueue.size() == 3) {
//				t5yieQueue.poll();
//			}
//			t5yieQueue.add(t5yie);
//			if (t5yie.getRollAverageFlag()) {
//				continue;
//			}
//
//			Iterator<T5YIE> queueItr = t5yieQueue.iterator();
//
//			T5YIECalculation tempT5YIECalculation = new T5YIECalculation();
//			if (t5yieCalculationHashMap.containsKey(t5yie.getDate())) {
//				tempT5YIECalculation = t5yieCalculationHashMap.get(t5yie.getDate());
//			} else {
//				tempT5YIECalculation.setToDate(t5yie.getDate());
//			}
//
//			while (queueItr.hasNext()) {
//				T5YIE t5yieVal = queueItr.next();
//				rollingAvg += t5yieVal.getValue();
//				period++;
//			}
//
//			rollingAvgThreeMon = rollingAvg / period;
//
//			t5yie.setRollAverageFlag(true);
//			tempT5YIECalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
//			t5yieCalculationList.add(tempT5YIECalculation);
//
//		}
//
//		t5yieCalculationReference = t5yieCalculationRepository.saveAll(t5yieCalculationList);
//		t5yieList = t5yieRepostiory.saveAll(t5yieList);
//		return;
//	}
//
//	@Scheduled(fixedDelay = 1000 * 60)
//	public List<T5YIE> getLatestT5YIERecords() {
//
//		if (NumberUtils.INTEGER_ZERO.equals(t5yieCalculationRepository.findAny())) {
//			return null;
//		}
//		System.out.println("getLatestT5YIERecords");
//		Optional<T5YIE> lastRecordOpt = t5yieRepostiory.findTopByOrderByDateDesc();
//		List<T5YIE> response = new ArrayList<>();
//		if (lastRecordOpt.isPresent()) {
//			T5YIE lastRecord = lastRecordOpt.get();
//			String lastDate = lastRecord.getDate().toString();
//			String transactionUrl = QUANDL_HOST_URL + "FRED" + "/" + "t5yie" + "/" + QUANDL_DATA_FORMAT;
//
//			UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(transactionUrl)
//					// Add query parameter
//					.queryParam("start_date", lastDate).queryParam("order", "ASC")
//					.queryParam(QUANDL_API_KEY_NAME, QUANDL_API_KEY_VALUE);
//
//			List<T5YIE> T5YIEList = new ArrayList<>();
//			FederalResponse json = consumeResponse(builder.toUriString());
//			json.getDataset_data().getData().stream().forEach(o -> {
//				ArrayList temp = (ArrayList) o;
//				try {
//					Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
//					T5YIEList.add(new T5YIE(date, Float.parseFloat(temp.get(1).toString())));
//					;
//				} catch (ParseException e) {
//					e.printStackTrace();
//				}
//			});
//
//			if (T5YIEList.size() > 1) { // As last record is already present in DB
//				T5YIEList.remove(0);
//				response = t5yieRepostiory.saveAll(T5YIEList);
//				logger.info("New record inserted in T5YIE");
//			}
//
//		}
//		return response;
//	}
//
//	private FederalResponse consumeResponse(String urlToFetch) {
//		HashMap<String, String> apiKeyMap = new HashMap<>();
//		HttpHeaders headers = new HttpHeaders();
//		headers.add("Accept", MediaType.APPLICATION_JSON.toString());
//		headers.add("Content-Type", MediaType.APPLICATION_JSON.toString());
//		headers.add("Cache-Control", "no-cache");
//		HttpEntity entity = new HttpEntity(apiKeyMap, headers);
//		FederalResponse json = restTemplate.exchange(urlToFetch, HttpMethod.GET, entity, FederalResponse.class)
//				.getBody();
//		return json;
//	}
//
//}
