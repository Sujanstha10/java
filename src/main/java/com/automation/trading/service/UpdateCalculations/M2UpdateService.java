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

import com.automation.trading.domain.fred.M2;
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
import com.automation.trading.domain.calculation.M2Calculation;
import com.automation.trading.repository.M2CalculationRepository;
import com.automation.trading.repository.M2Repository;
import com.automation.trading.service.M2Service;

@Service
public class M2UpdateService {

	@Autowired
	private M2Repository m2Repostiory;

	@Autowired
	private M2CalculationRepository m2CalculationRepository;

	@Autowired
	private M2Service m2RateOfChangeService;

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

	private Logger logger = LoggerFactory.getLogger(M2UpdateService.class);

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRoc() {

		if (NumberUtils.INTEGER_ZERO.equals(m2CalculationRepository.findAny())) {
			m2RateOfChangeService.calculateRoc();
			m2RateOfChangeService.updateRocChangeSign();
		}

		System.out.println("calculateRocRollingAnnualAvg");

		Optional<List<M2>> m2ListOpt = m2Repostiory.findByRocFlagIsFalseOrderByDate();
		Optional<M2> prevM2Opt = m2Repostiory.findFirstByRocFlagIsTrueOrderByDateDesc();
		HashMap<Date, M2Calculation> m2CalculationHashMap = new HashMap<>();
		M2Calculation prevM2CalculationRow = new M2Calculation();

		List<M2> m2List = new ArrayList<>();

		if (m2ListOpt.isPresent()) {
			m2List = m2ListOpt.get();
			if (prevM2Opt.isPresent()) {
				m2List.add(prevM2Opt.get());
			}
		} else {
			return;
		}

		Collections.sort(m2List, new FederalMoneyService.SortByDateM2());
		List<M2Calculation> m2CalculationReference = m2CalculationRepository.findAll();
		List<M2Calculation> m2CalculationModified = new ArrayList<>();
		Queue<M2> m2Queue = new LinkedList<>();

		for (M2Calculation m2Calculation : m2CalculationReference) {
			m2CalculationHashMap.put(m2Calculation.getToDate(), m2Calculation);
		}

		for (M2 m2 : m2List) {
			M2Calculation tempM2Calculation = new M2Calculation();

			if (m2Queue.size() == 2) {
				m2Queue.poll();
			}
			m2Queue.add(m2);

			if (m2.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;

			Iterator<M2> queueIterator = m2Queue.iterator();

			if (m2CalculationHashMap.containsKey(m2.getDate())) {
				tempM2Calculation = m2CalculationHashMap.get(m2.getDate());
			} else {
				tempM2Calculation.setToDate(m2.getDate());
			}

			while (queueIterator.hasNext()) {
				M2 temp = queueIterator.next();
				temp.setRocFlag(true);
				if (m2Queue.size() == 1) {
					roc = 0f;
					tempM2Calculation.setRoc(roc);
					tempM2Calculation.setToDate(m2.getDate());
					tempM2Calculation.setRocChangeSign(0);
				} else {
					roc = (m2.getValue() / ((LinkedList<M2>) m2Queue).get(0).getValue()) - 1;
					tempM2Calculation.setRoc(roc);
					tempM2Calculation.setToDate(m2.getDate());
				}

			}

			m2CalculationModified.add(tempM2Calculation);
		}

		m2List = m2Repostiory.saveAll(m2List);
		m2CalculationModified = m2CalculationRepository.saveAll(m2CalculationModified);
		logger.debug("Added new M2 row, " + m2CalculationModified);

		return;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRocRollingAnnualAvg() {

		if (NumberUtils.INTEGER_ZERO.equals(m2CalculationRepository.findAny())) {
			return;
		}

		System.out.println("calculateRocRollingAnnualAvg");
		Optional<List<M2Calculation>> m2CalculationListOpt = m2CalculationRepository
				.findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();
		Optional<List<M2Calculation>> prevM2CalculationListOpt = m2CalculationRepository
				.findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();
		List<M2Calculation> m2CalculationList = new ArrayList<>();

		if (m2CalculationListOpt.isPresent()) {
			m2CalculationList = m2CalculationListOpt.get();
			if (prevM2CalculationListOpt.isPresent()) {
				m2CalculationList.addAll(prevM2CalculationListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(m2CalculationList, new FederalMoneyService.SortByDateM2Calculation());

		Queue<M2Calculation> m2CalculationPriorityQueue = new LinkedList<M2Calculation>();
		for (M2Calculation m2Calculation : m2CalculationList) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (m2CalculationPriorityQueue.size() == 4) {
				m2CalculationPriorityQueue.poll();
			}
			m2CalculationPriorityQueue.add(m2Calculation);

			if (m2Calculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<M2Calculation> queueIterator = m2CalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				M2Calculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			m2Calculation.setRocAnnRollAvgFlag(true);
			m2Calculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(m2CalculationList);
		m2CalculationList = m2CalculationRepository.saveAll(m2CalculationList);
		logger.info("New m2 calculation record inserted" + m2CalculationList);
		return;

	}

	/**
	 * Calculates Rolling Average of Three Month M2
	 *
	 * @return M2Calculation , updated M2Calculation Table
	 */
	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRollAvgThreeMonth() {

		if (NumberUtils.INTEGER_ZERO.equals(m2CalculationRepository.findAny())) {
			return;
		}

		System.out.println("calculateRollAvgThreeMonth");

		List<M2Calculation> m2CalculationList = new ArrayList<>();
		Optional<List<M2>> m2ListOpt = m2Repostiory.findByRollAverageFlagIsFalseOrderByDate();
		Optional<List<M2>> prevM2ListOpt = m2Repostiory
				.findTop2ByRollAverageFlagIsTrueOrderByDateDesc();
		List<M2Calculation> m2CalculationReference = m2CalculationRepository.findAll();
		HashMap<Date, M2Calculation> m2CalculationHashMap = new HashMap<>();
		List<M2> m2List = new ArrayList<>();

		for (M2Calculation m2Calculation : m2CalculationReference) {
			m2CalculationHashMap.put(m2Calculation.getToDate(), m2Calculation);
		}

		Queue<M2> m2Queue = new LinkedList<>();

		if (m2ListOpt.isPresent()) {
			m2List = m2ListOpt.get();
			if (prevM2ListOpt.isPresent()) {
				m2List.addAll(prevM2ListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(m2List, new FederalMoneyService.SortByDateM2());

		for (M2 m2 : m2List) {

			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			if (m2Queue.size() == 3) {
				m2Queue.poll();
			}
			m2Queue.add(m2);
			if (m2.getRollAverageFlag()) {
				continue;
			}

			Iterator<M2> queueItr = m2Queue.iterator();

			M2Calculation tempM2Calculation = new M2Calculation();
			if (m2CalculationHashMap.containsKey(m2.getDate())) {
				tempM2Calculation = m2CalculationHashMap.get(m2.getDate());
			} else {
				tempM2Calculation.setToDate(m2.getDate());
			}

			while (queueItr.hasNext()) {
				M2 m2Val = queueItr.next();
				rollingAvg += m2Val.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			m2.setRollAverageFlag(true);
			tempM2Calculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			m2CalculationList.add(tempM2Calculation);

		}

		m2CalculationReference = m2CalculationRepository.saveAll(m2CalculationList);
		m2List = m2Repostiory.saveAll(m2List);
		return;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public List<M2> getLatestM2Records() {

		if (NumberUtils.INTEGER_ZERO.equals(m2CalculationRepository.findAny())) {
			return null;
		}
		System.out.println("getLatestM2Records");
		Optional<M2> lastRecordOpt = m2Repostiory.findTopByOrderByDateDesc();
		List<M2> response = new ArrayList<>();
		if (lastRecordOpt.isPresent()) {
			M2 lastRecord = lastRecordOpt.get();
			String lastDate = lastRecord.getDate().toString();
			String transactionUrl = QUANDL_HOST_URL + "FRED" + "/" + "M2" + "/" + QUANDL_DATA_FORMAT;

			UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(transactionUrl)
					// Add query parameter
					.queryParam("start_date", lastDate).queryParam("order", "ASC")
					.queryParam(QUANDL_API_KEY_NAME, QUANDL_API_KEY_VALUE);

			List<M2> M2List = new ArrayList<>();
			FederalResponse json = restUtility.consumeResponse(builder.toUriString());
			json.getDataset_data().getData().stream().forEach(o -> {
				ArrayList temp = (ArrayList) o;
				try {
					Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
					M2List.add(new M2(date, Float.parseFloat(temp.get(1).toString())));
					;
				} catch (ParseException e) {
					e.printStackTrace();
				}
			});

			if (M2List.size() > 1) { // As last record is already present in DB
				M2List.remove(0);
				response = m2Repostiory.saveAll(M2List);
				logger.info("New record inserted in M2");
			}

		}
		return response;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void updateRocChangeSignDgs10() {
		List<M2Calculation> m2CalculationList = m2CalculationRepository.findAllByRocIsNotNullAndRocChangeSignIsNull();
		M2Calculation lastUpdatedRecord = m2CalculationRepository.findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

		Collections.sort(m2CalculationList, new FederalMoneyService.SortByDateM2Calculation());

		if(m2CalculationList.size() == 0){
			return;
		}

		Float lastRoc = lastUpdatedRecord.getRoc();
		for (M2Calculation m2Calculation : m2CalculationList) {
			if(m2Calculation.getRoc() < lastRoc){
				m2Calculation.setRocChangeSign(-1);
			}else if (m2Calculation.getRoc() > lastRoc){
				m2Calculation.setRocChangeSign(1);
			}else if(m2Calculation.getRoc() == lastRoc){
				m2Calculation.setRocChangeSign(0);
			}

			lastRoc = m2Calculation.getRoc();
		}

		m2CalculationRepository.saveAll(m2CalculationList);
	}

}
