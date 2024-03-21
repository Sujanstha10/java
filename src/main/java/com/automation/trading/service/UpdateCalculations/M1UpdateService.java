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

import com.automation.trading.domain.fred.M1;

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
import com.automation.trading.domain.calculation.M1Calculation;
import com.automation.trading.repository.M1CalculationRepository;
import com.automation.trading.repository.M1Repository;
import com.automation.trading.service.M1Service;

@Service
public class M1UpdateService {

	@Autowired
	private M1Repository m1Repostiory;

	@Autowired
	private M1CalculationRepository m1CalculationRepository;

	@Autowired
	private M1Service m1RateOfChangeService;

	

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

	private Logger logger = LoggerFactory.getLogger(M1UpdateService.class);

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRoc() {

		if (NumberUtils.INTEGER_ZERO.equals(m1CalculationRepository.findAny())) {
			m1RateOfChangeService.calculateRoc();
			m1RateOfChangeService.updateRocChangeSign();
		}

		System.out.println("calculateRocRollingAnnualAvg");

		Optional<List<M1>> m1ListOpt = m1Repostiory.findByRocFlagIsFalseOrderByDate();
		Optional<M1> prevM1Opt = m1Repostiory.findFirstByRocFlagIsTrueOrderByDateDesc();
		HashMap<Date, M1Calculation> m1CalculationHashMap = new HashMap<>();
		M1Calculation prevM1CalculationRow = new M1Calculation();

		List<M1> m1List = new ArrayList<>();

		if (m1ListOpt.isPresent()) {
			m1List = m1ListOpt.get();
			if (prevM1Opt.isPresent()) {
				m1List.add(prevM1Opt.get());
			}
		} else {
			return;
		}

		Collections.sort(m1List, new FederalMoneyService.SortByDateM1());
		List<M1Calculation> m1CalculationReference = m1CalculationRepository.findAll();
		List<M1Calculation> m1CalculationModified = new ArrayList<>();
		Queue<M1> m1Queue = new LinkedList<>();

		for (M1Calculation m1Calculation : m1CalculationReference) {
			m1CalculationHashMap.put(m1Calculation.getToDate(), m1Calculation);
		}

		for (M1 m1 : m1List) {
			M1Calculation tempM1Calculation = new M1Calculation();

			if (m1Queue.size() == 2) {
				m1Queue.poll();
			}
			m1Queue.add(m1);

			if (m1.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;

			Iterator<M1> queueIterator = m1Queue.iterator();

			if (m1CalculationHashMap.containsKey(m1.getDate())) {
				tempM1Calculation = m1CalculationHashMap.get(m1.getDate());
			} else {
				tempM1Calculation.setToDate(m1.getDate());
			}

			while (queueIterator.hasNext()) {
				M1 temp = queueIterator.next();
				temp.setRocFlag(true);
				if (m1Queue.size() == 1) {
					roc = 0f;
					tempM1Calculation.setRoc(roc);
					tempM1Calculation.setToDate(m1.getDate());
					tempM1Calculation.setRocChangeSign(0);
				} else {
					roc = (m1.getValue() / ((LinkedList<M1>) m1Queue).get(0).getValue()) - 1;
					tempM1Calculation.setRoc(roc);
					tempM1Calculation.setToDate(m1.getDate());
				}

			}

			m1CalculationModified.add(tempM1Calculation);
		}

		m1List = m1Repostiory.saveAll(m1List);
		m1CalculationModified = m1CalculationRepository.saveAll(m1CalculationModified);
		logger.debug("Added new M1 row, " + m1CalculationModified);

		return;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRocRollingAnnualAvg() {

		if (NumberUtils.INTEGER_ZERO.equals(m1CalculationRepository.findAny())) {
			return;
		}

		System.out.println("calculateRocRollingAnnualAvg");
		Optional<List<M1Calculation>> m1CalculationListOpt = m1CalculationRepository
				.findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();
		Optional<List<M1Calculation>> prevM1CalculationListOpt = m1CalculationRepository
				.findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();
		List<M1Calculation> m1CalculationList = new ArrayList<>();

		if (m1CalculationListOpt.isPresent()) {
			m1CalculationList = m1CalculationListOpt.get();
			if (prevM1CalculationListOpt.isPresent()) {
				m1CalculationList.addAll(prevM1CalculationListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(m1CalculationList, new FederalMoneyService.SortByDateM1Calculation());

		Queue<M1Calculation> m1CalculationPriorityQueue = new LinkedList<M1Calculation>();
		for (M1Calculation m1Calculation : m1CalculationList) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (m1CalculationPriorityQueue.size() == 4) {
				m1CalculationPriorityQueue.poll();
			}
			m1CalculationPriorityQueue.add(m1Calculation);

			if (m1Calculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<M1Calculation> queueIterator = m1CalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				M1Calculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			m1Calculation.setRocAnnRollAvgFlag(true);
			m1Calculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(m1CalculationList);
		m1CalculationList = m1CalculationRepository.saveAll(m1CalculationList);
		logger.info("New m1 calculation record inserted" + m1CalculationList);
		return;

	}

	/**
	 * Calculates Rolling Average of Three Month M1
	 *
	 * @return M1Calculation , updated M1Calculation Table
	 */
	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRollAvgThreeMonth() {

		if (NumberUtils.INTEGER_ZERO.equals(m1CalculationRepository.findAny())) {
			return;
		}

		System.out.println("calculateRollAvgThreeMonth");

		List<M1Calculation> m1CalculationList = new ArrayList<>();
		Optional<List<M1>> m1ListOpt = m1Repostiory.findByRollAverageFlagIsFalseOrderByDate();
		Optional<List<M1>> prevM1ListOpt = m1Repostiory
				.findTop2ByRollAverageFlagIsTrueOrderByDateDesc();
		List<M1Calculation> m1CalculationReference = m1CalculationRepository.findAll();
		HashMap<Date, M1Calculation> m1CalculationHashMap = new HashMap<>();
		List<M1> m1List = new ArrayList<>();

		for (M1Calculation m1Calculation : m1CalculationReference) {
			m1CalculationHashMap.put(m1Calculation.getToDate(), m1Calculation);
		}

		Queue<M1> m1Queue = new LinkedList<>();

		if (m1ListOpt.isPresent()) {
			m1List = m1ListOpt.get();
			if (prevM1ListOpt.isPresent()) {
				m1List.addAll(prevM1ListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(m1List, new FederalMoneyService.SortByDateM1());

		for (M1 m1 : m1List) {

			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			if (m1Queue.size() == 3) {
				m1Queue.poll();
			}
			m1Queue.add(m1);
			if (m1.getRollAverageFlag()) {
				continue;
			}

			Iterator<M1> queueItr = m1Queue.iterator();

			M1Calculation tempM1Calculation = new M1Calculation();
			if (m1CalculationHashMap.containsKey(m1.getDate())) {
				tempM1Calculation = m1CalculationHashMap.get(m1.getDate());
			} else {
				tempM1Calculation.setToDate(m1.getDate());
			}

			while (queueItr.hasNext()) {
				M1 m1Val = queueItr.next();
				rollingAvg += m1Val.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			m1.setRollAverageFlag(true);
			tempM1Calculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			m1CalculationList.add(tempM1Calculation);

		}

		m1CalculationReference = m1CalculationRepository.saveAll(m1CalculationList);
		m1List = m1Repostiory.saveAll(m1List);
		return;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public List<M1> getLatestM1Records() {

		if (NumberUtils.INTEGER_ZERO.equals(m1CalculationRepository.findAny())) {
			return null;
		}
		System.out.println("getLatestM1Records");
		Optional<M1> lastRecordOpt = m1Repostiory.findTopByOrderByDateDesc();
		List<M1> response = new ArrayList<>();
		if (lastRecordOpt.isPresent()) {
			M1 lastRecord = lastRecordOpt.get();
			String lastDate = lastRecord.getDate().toString();
			String transactionUrl = QUANDL_HOST_URL + "FRED" + "/" + "M1" + "/" + QUANDL_DATA_FORMAT;

			UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(transactionUrl)
					// Add query parameter
					.queryParam("start_date", lastDate).queryParam("order", "ASC")
					.queryParam(QUANDL_API_KEY_NAME, QUANDL_API_KEY_VALUE);

			List<M1> M1List = new ArrayList<>();
			FederalResponse json = restUtility.consumeResponse(builder.toUriString());
			json.getDataset_data().getData().stream().forEach(o -> {
				ArrayList temp = (ArrayList) o;
				try {
					Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
					M1List.add(new M1(date, Float.parseFloat(temp.get(1).toString())));
					;
				} catch (ParseException e) {
					e.printStackTrace();
				}
			});

			if (M1List.size() > 1) { // As last record is already present in DB
				M1List.remove(0);
				response = m1Repostiory.saveAll(M1List);
				logger.info("New record inserted in M1");
			}

		}
		return response;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void updateRocChangeSignDgs10() {
		List<M1Calculation> m1CalculationList = m1CalculationRepository.findAllByRocIsNotNullAndRocChangeSignIsNull();
		M1Calculation lastUpdatedRecord = m1CalculationRepository.findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

		Collections.sort(m1CalculationList, new FederalMoneyService.SortByDateM1Calculation());

		if(m1CalculationList.size() == 0){
			return;
		}

		Float lastRoc = lastUpdatedRecord.getRoc();
		for (M1Calculation m1Calculation : m1CalculationList) {
			if(m1Calculation.getRoc() < lastRoc){
				m1Calculation.setRocChangeSign(-1);
			}else if (m1Calculation.getRoc() > lastRoc){
				m1Calculation.setRocChangeSign(1);
			}else if(m1Calculation.getRoc() == lastRoc){
				m1Calculation.setRocChangeSign(0);
			}

			lastRoc = m1Calculation.getRoc();
		}

		m1CalculationRepository.saveAll(m1CalculationList);
	}

}
