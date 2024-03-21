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
import com.automation.trading.domain.calculation.M2VCalculation;
import com.automation.trading.domain.fred.M2V;
import com.automation.trading.repository.M2VCalculationRepository;
import com.automation.trading.repository.M2VRepository;
import com.automation.trading.service.FederalMoneyService.SortByDateM2V;
import com.automation.trading.service.FederalMoneyService.SortByDateM2VCalculation;
import com.automation.trading.service.M2VService;

@Service
public class M2VUpdateService {

	@Autowired
	private M2VRepository m2vRepostiory;

	@Autowired
	private M2VCalculationRepository m2vCalculationRepository;

	@Autowired
	private M2VService m2vService;

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

	private Logger logger = LoggerFactory.getLogger(M2VUpdateService.class);

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRoc() {

		if (NumberUtils.INTEGER_ZERO.equals(m2vCalculationRepository.findAny())) {
			m2vService.calculateRoc();
			m2vService.updateRocChangeSign();
		}

		System.out.println("calculateRocRollingAnnualAvg");

		Optional<List<M2V>> m2vListOpt = m2vRepostiory.findByRocFlagIsFalseOrderByDate();
		Optional<M2V> prevM2VOpt = m2vRepostiory.findFirstByRocFlagIsTrueOrderByDateDesc();
		HashMap<Date, M2VCalculation> m2vCalculationHashMap = new HashMap<>();
		M2VCalculation prevM2VCalculationRow = new M2VCalculation();

		List<M2V> m2vList = new ArrayList<>();

		if (m2vListOpt.isPresent()) {
			m2vList = m2vListOpt.get();
			if (prevM2VOpt.isPresent()) {
				m2vList.add(prevM2VOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(m2vList, new SortByDateM2V());
		List<M2VCalculation> m2vCalculationReference = m2vCalculationRepository.findAll();
		List<M2VCalculation> m2vCalculationModified = new ArrayList<>();
		Queue<M2V> m2vQueue = new LinkedList<>();

		for (M2VCalculation m2vCalculation : m2vCalculationReference) {
			m2vCalculationHashMap.put(m2vCalculation.getToDate(), m2vCalculation);
		}

		for (M2V m2v : m2vList) {
			M2VCalculation tempM2VCalculation = new M2VCalculation();

			if (m2vQueue.size() == 2) {
				m2vQueue.poll();
			}
			m2vQueue.add(m2v);

			if (m2v.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;

			Iterator<M2V> queueIterator = m2vQueue.iterator();

			if (m2vCalculationHashMap.containsKey(m2v.getDate())) {
				tempM2VCalculation = m2vCalculationHashMap.get(m2v.getDate());
			} else {
				tempM2VCalculation.setToDate(m2v.getDate());
			}

			while (queueIterator.hasNext()) {
				M2V temp = queueIterator.next();
				temp.setRocFlag(true);
				if (m2vQueue.size() == 1) {
					roc = 0f;
					tempM2VCalculation.setRoc(roc);
					tempM2VCalculation.setToDate(m2v.getDate());
					tempM2VCalculation.setRocChangeSign(0);
				} else {
					roc = (m2v.getValue() / ((LinkedList<M2V>) m2vQueue).get(0).getValue()) - 1;
					tempM2VCalculation.setRoc(roc);
					tempM2VCalculation.setToDate(m2v.getDate());
				}

			}

			m2vCalculationModified.add(tempM2VCalculation);
		}

		m2vList = m2vRepostiory.saveAll(m2vList);
		m2vCalculationModified = m2vCalculationRepository.saveAll(m2vCalculationModified);
		logger.debug("Added new M2V row, " + m2vCalculationModified);

		return;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRocRollingAnnualAvg() {

		if (NumberUtils.INTEGER_ZERO.equals(m2vCalculationRepository.findAny())) {
			return;
		}

		System.out.println("calculateRocRollingAnnualAvg");
		Optional<List<M2VCalculation>> m2vCalculationListOpt = m2vCalculationRepository
				.findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();
		Optional<List<M2VCalculation>> prevM2VCalculationListOpt = m2vCalculationRepository
				.findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();
		List<M2VCalculation> m2vCalculationList = new ArrayList<>();

		if (m2vCalculationListOpt.isPresent()) {
			m2vCalculationList = m2vCalculationListOpt.get();
			if (prevM2VCalculationListOpt.isPresent()) {
				m2vCalculationList.addAll(prevM2VCalculationListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(m2vCalculationList, new SortByDateM2VCalculation());

		Queue<M2VCalculation> m2vCalculationPriorityQueue = new LinkedList<M2VCalculation>();
		for (M2VCalculation m2vCalculation : m2vCalculationList) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (m2vCalculationPriorityQueue.size() == 4) {
				m2vCalculationPriorityQueue.poll();
			}
			m2vCalculationPriorityQueue.add(m2vCalculation);

			if (m2vCalculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<M2VCalculation> queueIterator = m2vCalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				M2VCalculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			m2vCalculation.setRocAnnRollAvgFlag(true);
			m2vCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(m2vCalculationList);
		m2vCalculationList = m2vCalculationRepository.saveAll(m2vCalculationList);
		logger.info("New m2v calculation record inserted" + m2vCalculationList);
		return;

	}

	/**
	 * Calculates Rolling Average of Three Month M2V
	 *
	 * @return M2VCalculation , updated M2VCalculation Table
	 */
	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRollAvgThreeMonth() {

		if (NumberUtils.INTEGER_ZERO.equals(m2vCalculationRepository.findAny())) {
			return;
		}

		System.out.println("calculateRollAvgThreeMonth");

		List<M2VCalculation> m2vCalculationList = new ArrayList<>();
		Optional<List<M2V>> m2vListOpt = m2vRepostiory.findByRollAverageFlagIsFalseOrderByDate();
		Optional<List<M2V>> prevM2VListOpt = m2vRepostiory.findTop2ByRollAverageFlagIsTrueOrderByDateDesc();
		List<M2VCalculation> m2vCalculationReference = m2vCalculationRepository.findAll();
		HashMap<Date, M2VCalculation> m2vCalculationHashMap = new HashMap<>();
		List<M2V> m2vList = new ArrayList<>();

		for (M2VCalculation m2vCalculation : m2vCalculationReference) {
			m2vCalculationHashMap.put(m2vCalculation.getToDate(), m2vCalculation);
		}

		Queue<M2V> m2vQueue = new LinkedList<>();

		if (m2vListOpt.isPresent()) {
			m2vList = m2vListOpt.get();
			if (prevM2VListOpt.isPresent()) {
				m2vList.addAll(prevM2VListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(m2vList, new SortByDateM2V());

		for (M2V m2v : m2vList) {

			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			if (m2vQueue.size() == 3) {
				m2vQueue.poll();
			}
			m2vQueue.add(m2v);
			if (m2v.getRollAverageFlag()) {
				continue;
			}

			Iterator<M2V> queueItr = m2vQueue.iterator();

			M2VCalculation tempM2VCalculation = new M2VCalculation();
			if (m2vCalculationHashMap.containsKey(m2v.getDate())) {
				tempM2VCalculation = m2vCalculationHashMap.get(m2v.getDate());
			} else {
				tempM2VCalculation.setToDate(m2v.getDate());
			}

			while (queueItr.hasNext()) {
				M2V m2vVal = queueItr.next();
				rollingAvg += m2vVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			m2v.setRollAverageFlag(true);
			tempM2VCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			m2vCalculationList.add(tempM2VCalculation);

		}

		m2vCalculationReference = m2vCalculationRepository.saveAll(m2vCalculationList);
		m2vList = m2vRepostiory.saveAll(m2vList);
		return;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public List<M2V> getLatestM2VRecords() {

		if (NumberUtils.INTEGER_ZERO.equals(m2vCalculationRepository.findAny())) {
			return null;
		}
		System.out.println("getLatestM2VRecords");
		Optional<M2V> lastRecordOpt = m2vRepostiory.findTopByOrderByDateDesc();
		List<M2V> response = new ArrayList<>();
		if (lastRecordOpt.isPresent()) {
			M2V lastRecord = lastRecordOpt.get();
			String lastDate = lastRecord.getDate().toString();
			String transactionUrl = QUANDL_HOST_URL + "FRED" + "/" + "M2V" + "/" + QUANDL_DATA_FORMAT;

			UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(transactionUrl)
					// Add query parameter
					.queryParam("start_date", lastDate).queryParam("order", "ASC")
					.queryParam(QUANDL_API_KEY_NAME, QUANDL_API_KEY_VALUE);

			List<M2V> M2VList = new ArrayList<>();
			FederalResponse json = restUtility.consumeResponse(builder.toUriString());
			json.getDataset_data().getData().stream().forEach(o -> {
				ArrayList temp = (ArrayList) o;
				try {
					Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
					M2VList.add(new M2V(date, Float.parseFloat(temp.get(1).toString())));
					;
				} catch (ParseException e) {
					e.printStackTrace();
				}
			});

			if (M2VList.size() > 1) { // As last record is already present in DB
				M2VList.remove(0);
				response = m2vRepostiory.saveAll(M2VList);
				logger.info("New record inserted in M2V");
			}

		}
		return response;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void updateRocChangeSignM2V() {
		List<M2VCalculation> m2vCalculationList = m2vCalculationRepository
				.findAllByRocIsNotNullAndRocChangeSignIsNull();
		M2VCalculation lastUpdatedRecord = m2vCalculationRepository
				.findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

		Collections.sort(m2vCalculationList, new SortByDateM2VCalculation());

		if (m2vCalculationList.size() == 0) {
			return;
		}

		Float lastRoc = lastUpdatedRecord.getRoc();
		for (M2VCalculation m2vCalculation : m2vCalculationList) {
			if (m2vCalculation.getRoc() < lastRoc) {
				m2vCalculation.setRocChangeSign(-1);
			} else if (m2vCalculation.getRoc() > lastRoc) {
				m2vCalculation.setRocChangeSign(1);
			} else if (m2vCalculation.getRoc() == lastRoc) {
				m2vCalculation.setRocChangeSign(0);
			}

			lastRoc = m2vCalculation.getRoc();
		}

		m2vCalculationRepository.saveAll(m2vCalculationList);
	}

}
