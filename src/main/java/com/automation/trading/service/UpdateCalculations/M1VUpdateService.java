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

import com.automation.trading.domain.fred.M1V;
import com.automation.trading.service.FederalMoneyService;
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
import com.automation.trading.domain.calculation.M1VCalculation;
import com.automation.trading.repository.M1VCalculationRepository;
import com.automation.trading.repository.M1VRepository;
import com.automation.trading.service.M1VService;

@Service
public class M1VUpdateService {

	@Autowired
	private M1VRepository m1vRepostiory;

	@Autowired
	private M1VCalculationRepository m1vCalculationRepository;

	@Autowired
	private M1VService m1vRateOfChangeService;

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

	private Logger logger = LoggerFactory.getLogger(M1VUpdateService.class);

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRoc() {

		if (NumberUtils.INTEGER_ZERO.equals(m1vCalculationRepository.findAny())) {
			m1vRateOfChangeService.calculateRoc();
			m1vRateOfChangeService.updateRocChangeSign();
		}

		System.out.println("calculateRocRollingAnnualAvg");

		Optional<List<M1V>> m1vListOpt = m1vRepostiory.findByRocFlagIsFalseOrderByDate();
		Optional<M1V> prevM1VOpt = m1vRepostiory.findFirstByRocFlagIsTrueOrderByDateDesc();
		HashMap<Date, M1VCalculation> m1vCalculationHashMap = new HashMap<>();
		M1VCalculation prevM1VCalculationRow = new M1VCalculation();

		List<M1V> m1vList = new ArrayList<>();

		if (m1vListOpt.isPresent()) {
			m1vList = m1vListOpt.get();
			if (prevM1VOpt.isPresent()) {
				m1vList.add(prevM1VOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(m1vList, new FederalMoneyService.SortByDateM1V());
		List<M1VCalculation> m1vCalculationReference = m1vCalculationRepository.findAll();
		List<M1VCalculation> m1vCalculationModified = new ArrayList<>();
		Queue<M1V> m1vQueue = new LinkedList<>();

		for (M1VCalculation m1vCalculation : m1vCalculationReference) {
			m1vCalculationHashMap.put(m1vCalculation.getToDate(), m1vCalculation);
		}

		for (M1V m1v : m1vList) {
			M1VCalculation tempM1VCalculation = new M1VCalculation();

			if (m1vQueue.size() == 2) {
				m1vQueue.poll();
			}
			m1vQueue.add(m1v);

			if (m1v.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;

			Iterator<M1V> queueIterator = m1vQueue.iterator();

			if (m1vCalculationHashMap.containsKey(m1v.getDate())) {
				tempM1VCalculation = m1vCalculationHashMap.get(m1v.getDate());
			} else {
				tempM1VCalculation.setToDate(m1v.getDate());
			}

			while (queueIterator.hasNext()) {
				M1V temp = queueIterator.next();
				temp.setRocFlag(true);
				if (m1vQueue.size() == 1) {
					roc = 0f;
					tempM1VCalculation.setRoc(roc);
					tempM1VCalculation.setToDate(m1v.getDate());
					tempM1VCalculation.setRocChangeSign(0);
				} else {
					roc = (m1v.getValue() / ((LinkedList<M1V>) m1vQueue).get(0).getValue()) - 1;
					tempM1VCalculation.setRoc(roc);
					tempM1VCalculation.setToDate(m1v.getDate());
				}

			}

			m1vCalculationModified.add(tempM1VCalculation);
		}

		m1vList = m1vRepostiory.saveAll(m1vList);
		m1vCalculationModified = m1vCalculationRepository.saveAll(m1vCalculationModified);
		logger.debug("Added new M1V row, " + m1vCalculationModified);

		return;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRocRollingAnnualAvg() {

		if (NumberUtils.INTEGER_ZERO.equals(m1vCalculationRepository.findAny())) {
			return;
		}

		System.out.println("calculateRocRollingAnnualAvg");
		Optional<List<M1VCalculation>> m1vCalculationListOpt = m1vCalculationRepository
				.findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();
		Optional<List<M1VCalculation>> prevM1VCalculationListOpt = m1vCalculationRepository
				.findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();
		List<M1VCalculation> m1vCalculationList = new ArrayList<>();

		if (m1vCalculationListOpt.isPresent()) {
			m1vCalculationList = m1vCalculationListOpt.get();
			if (prevM1VCalculationListOpt.isPresent()) {
				m1vCalculationList.addAll(prevM1VCalculationListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(m1vCalculationList, new FederalMoneyService.SortByDateM1VCalculation());

		Queue<M1VCalculation> m1vCalculationPriorityQueue = new LinkedList<M1VCalculation>();
		for (M1VCalculation m1vCalculation : m1vCalculationList) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (m1vCalculationPriorityQueue.size() == 4) {
				m1vCalculationPriorityQueue.poll();
			}
			m1vCalculationPriorityQueue.add(m1vCalculation);

			if (m1vCalculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<M1VCalculation> queueIterator = m1vCalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				M1VCalculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			m1vCalculation.setRocAnnRollAvgFlag(true);
			m1vCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(m1vCalculationList);
		m1vCalculationList = m1vCalculationRepository.saveAll(m1vCalculationList);
		logger.info("New m1v calculation record inserted" + m1vCalculationList);
		return;

	}

	/**
	 * Calculates Rolling Average of Three Month M1V
	 *
	 * @return M1VCalculation , updated M1VCalculation Table
	 */
	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRollAvgThreeMonth() {

		if (NumberUtils.INTEGER_ZERO.equals(m1vCalculationRepository.findAny())) {
			return;
		}

		System.out.println("calculateRollAvgThreeMonth");

		List<M1VCalculation> m1vCalculationList = new ArrayList<>();
		Optional<List<M1V>> m1vListOpt = m1vRepostiory.findByRollAverageFlagIsFalseOrderByDate();
		Optional<List<M1V>> prevM1VListOpt = m1vRepostiory
				.findTop2ByRollAverageFlagIsTrueOrderByDateDesc();
		List<M1VCalculation> m1vCalculationReference = m1vCalculationRepository.findAll();
		HashMap<Date, M1VCalculation> m1vCalculationHashMap = new HashMap<>();
		List<M1V> m1vList = new ArrayList<>();

		for (M1VCalculation m1vCalculation : m1vCalculationReference) {
			m1vCalculationHashMap.put(m1vCalculation.getToDate(), m1vCalculation);
		}

		Queue<M1V> m1vQueue = new LinkedList<>();

		if (m1vListOpt.isPresent()) {
			m1vList = m1vListOpt.get();
			if (prevM1VListOpt.isPresent()) {
				m1vList.addAll(prevM1VListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(m1vList, new FederalMoneyService.SortByDateM1V());

		for (M1V m1v : m1vList) {

			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			if (m1vQueue.size() == 3) {
				m1vQueue.poll();
			}
			m1vQueue.add(m1v);
			if (m1v.getRollAverageFlag()) {
				continue;
			}

			Iterator<M1V> queueItr = m1vQueue.iterator();

			M1VCalculation tempM1VCalculation = new M1VCalculation();
			if (m1vCalculationHashMap.containsKey(m1v.getDate())) {
				tempM1VCalculation = m1vCalculationHashMap.get(m1v.getDate());
			} else {
				tempM1VCalculation.setToDate(m1v.getDate());
			}

			while (queueItr.hasNext()) {
				M1V m1vVal = queueItr.next();
				rollingAvg += m1vVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			m1v.setRollAverageFlag(true);
			tempM1VCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			m1vCalculationList.add(tempM1VCalculation);

		}

		m1vCalculationReference = m1vCalculationRepository.saveAll(m1vCalculationList);
		m1vList = m1vRepostiory.saveAll(m1vList);
		return;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public List<M1V> getLatestM1VRecords() {

		if (NumberUtils.INTEGER_ZERO.equals(m1vCalculationRepository.findAny())) {
			return null;
		}
		System.out.println("getLatestM1VRecords");
		Optional<M1V> lastRecordOpt = m1vRepostiory.findTopByOrderByDateDesc();
		List<M1V> response = new ArrayList<>();
		if (lastRecordOpt.isPresent()) {
			M1V lastRecord = lastRecordOpt.get();
			String lastDate = lastRecord.getDate().toString();
			String transactionUrl = QUANDL_HOST_URL + "FRED" + "/" + "M1V" + "/" + QUANDL_DATA_FORMAT;

			UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(transactionUrl)
					// Add query parameter
					.queryParam("start_date", lastDate).queryParam("order", "ASC")
					.queryParam(QUANDL_API_KEY_NAME, QUANDL_API_KEY_VALUE);

			List<M1V> M1VList = new ArrayList<>();
			FederalResponse json = restUtility.consumeResponse(builder.toUriString());
			json.getDataset_data().getData().stream().forEach(o -> {
				ArrayList temp = (ArrayList) o;
				try {
					Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
					M1VList.add(new M1V(date, Float.parseFloat(temp.get(1).toString())));
					;
				} catch (ParseException e) {
					e.printStackTrace();
				}
			});

			if (M1VList.size() > 1) { // As last record is already present in DB
				M1VList.remove(0);
				response = m1vRepostiory.saveAll(M1VList);
				logger.info("New record inserted in M1V");
			}

		}
		return response;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void updateRocChangeSignDgs10() {
		List<M1VCalculation> m1vCalculationList = m1vCalculationRepository.findAllByRocIsNotNullAndRocChangeSignIsNull();
		M1VCalculation lastUpdatedRecord = m1vCalculationRepository.findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

		Collections.sort(m1vCalculationList, new FederalMoneyService.SortByDateM1VCalculation());

		if(m1vCalculationList.size() == 0){
			return;
		}

		Float lastRoc = lastUpdatedRecord.getRoc();
		for (M1VCalculation m1vCalculation : m1vCalculationList) {
			if(m1vCalculation.getRoc() < lastRoc){
				m1vCalculation.setRocChangeSign(-1);
			}else if (m1vCalculation.getRoc() > lastRoc){
				m1vCalculation.setRocChangeSign(1);
			}else if(m1vCalculation.getRoc() == lastRoc){
				m1vCalculation.setRocChangeSign(0);
			}

			lastRoc = m1vCalculation.getRoc();
		}

		m1vCalculationRepository.saveAll(m1vCalculationList);
	}

}
