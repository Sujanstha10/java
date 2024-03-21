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
import com.automation.trading.domain.calculation.IC4WSACalculation;
import com.automation.trading.domain.fred.IC4WSA;
import com.automation.trading.repository.IC4WSACalculationRepository;
import com.automation.trading.repository.IC4WSARepository;
import com.automation.trading.service.FederalEmploymentService.SortByDateIC4WSA;
import com.automation.trading.service.FederalEmploymentService.SortByDateIC4WSACalculation;
import com.automation.trading.service.IC4WSAService;
import com.automation.trading.utility.RestUtility;

@Service
public class IC4WSAUpdateService {

	@Autowired
	private IC4WSARepository ic4wsaRepostiory;

	@Autowired
	private IC4WSACalculationRepository ic4wsaCalculationRepository;

	@Autowired
	private IC4WSAService ic4wsaRateOfChangeService;

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

	private Logger logger = LoggerFactory.getLogger(IC4WSAUpdateService.class);

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRoc() {

		if (NumberUtils.INTEGER_ZERO.equals(ic4wsaCalculationRepository.findAny())) {
			ic4wsaRateOfChangeService.calculateRoc();
			ic4wsaRateOfChangeService.updateRocChangeSignIC4WSA();
		}

		System.out.println("calculateRocRollingAnnualAvg");

		Optional<List<IC4WSA>> ic4wsaListOpt = ic4wsaRepostiory.findByRocFlagIsFalseOrderByDate();
		Optional<IC4WSA> prevIC4WSAOpt = ic4wsaRepostiory.findFirstByRocFlagIsTrueOrderByDateDesc();
		HashMap<Date, IC4WSACalculation> ic4wsaCalculationHashMap = new HashMap<>();
		IC4WSACalculation prevIC4WSACalculationRow = new IC4WSACalculation();

		List<IC4WSA> ic4wsaList = new ArrayList<>();

		if (ic4wsaListOpt.isPresent()) {
			ic4wsaList = ic4wsaListOpt.get();
			if (prevIC4WSAOpt.isPresent()) {
				ic4wsaList.add(prevIC4WSAOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(ic4wsaList, new SortByDateIC4WSA());
		List<IC4WSACalculation> ic4wsaCalculationReference = ic4wsaCalculationRepository.findAll();
		List<IC4WSACalculation> ic4wsaCalculationModified = new ArrayList<>();
		Queue<IC4WSA> ic4wsaQueue = new LinkedList<>();

		for (IC4WSACalculation ic4wsaCalculation : ic4wsaCalculationReference) {
			ic4wsaCalculationHashMap.put(ic4wsaCalculation.getToDate(), ic4wsaCalculation);
		}

		for (IC4WSA ic4wsa : ic4wsaList) {
			IC4WSACalculation tempIC4WSACalculation = new IC4WSACalculation();

			if (ic4wsaQueue.size() == 2) {
				ic4wsaQueue.poll();
			}
			ic4wsaQueue.add(ic4wsa);

			if (ic4wsa.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;

			Iterator<IC4WSA> queueIterator = ic4wsaQueue.iterator();

			if (ic4wsaCalculationHashMap.containsKey(ic4wsa.getDate())) {
				tempIC4WSACalculation = ic4wsaCalculationHashMap.get(ic4wsa.getDate());
			} else {
				tempIC4WSACalculation.setToDate(ic4wsa.getDate());
			}

			while (queueIterator.hasNext()) {
				IC4WSA temp = queueIterator.next();
				temp.setRocFlag(true);
				if (ic4wsaQueue.size() == 1) {
					roc = 0f;
					tempIC4WSACalculation.setRoc(roc);
					tempIC4WSACalculation.setToDate(ic4wsa.getDate());
					tempIC4WSACalculation.setRocChangeSign(0);
				} else {
					roc = (ic4wsa.getValue() / ((LinkedList<IC4WSA>) ic4wsaQueue).get(0).getValue()) - 1;
					tempIC4WSACalculation.setRoc(roc);
					tempIC4WSACalculation.setToDate(ic4wsa.getDate());
				}

			}

			ic4wsaCalculationModified.add(tempIC4WSACalculation);
		}

		ic4wsaList = ic4wsaRepostiory.saveAll(ic4wsaList);
		ic4wsaCalculationModified = ic4wsaCalculationRepository.saveAll(ic4wsaCalculationModified);
		logger.debug("Added new IC4WSA row, " + ic4wsaCalculationModified);

		return;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRocRollingAnnualAvg() {

		if (NumberUtils.INTEGER_ZERO.equals(ic4wsaCalculationRepository.findAny())) {
			return;
		}

		System.out.println("calculateRocRollingAnnualAvg");
		Optional<List<IC4WSACalculation>> ic4wsaCalculationListOpt = ic4wsaCalculationRepository
				.findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();
		Optional<List<IC4WSACalculation>> prevIC4WSACalculationListOpt = ic4wsaCalculationRepository
				.findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();
		List<IC4WSACalculation> ic4wsaCalculationList = new ArrayList<>();

		if (ic4wsaCalculationListOpt.isPresent()) {
			ic4wsaCalculationList = ic4wsaCalculationListOpt.get();
			if (prevIC4WSACalculationListOpt.isPresent()) {
				ic4wsaCalculationList.addAll(prevIC4WSACalculationListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(ic4wsaCalculationList, new SortByDateIC4WSACalculation());

		Queue<IC4WSACalculation> ic4wsaCalculationPriorityQueue = new LinkedList<IC4WSACalculation>();
		for (IC4WSACalculation ic4wsaCalculation : ic4wsaCalculationList) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (ic4wsaCalculationPriorityQueue.size() == 4) {
				ic4wsaCalculationPriorityQueue.poll();
			}
			ic4wsaCalculationPriorityQueue.add(ic4wsaCalculation);

			if (ic4wsaCalculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<IC4WSACalculation> queueIterator = ic4wsaCalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				IC4WSACalculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			ic4wsaCalculation.setRocAnnRollAvgFlag(true);
			ic4wsaCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(ic4wsaCalculationList);
		ic4wsaCalculationList = ic4wsaCalculationRepository.saveAll(ic4wsaCalculationList);
		logger.info("New ic4wsa calculation record inserted" + ic4wsaCalculationList);
		return;

	}

	/**
	 * Calculates Rolling Average of Three Month IC4WSA
	 *
	 * @return IC4WSACalculation , updated IC4WSACalculation Table
	 */
	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRollAvgThreeMonth() {

		if (NumberUtils.INTEGER_ZERO.equals(ic4wsaCalculationRepository.findAny())) {
			return;
		}

		System.out.println("calculateRollAvgThreeMonth");

		List<IC4WSACalculation> ic4wsaCalculationList = new ArrayList<>();
		Optional<List<IC4WSA>> ic4wsaListOpt = ic4wsaRepostiory.findByRollAverageFlagIsFalseOrderByDate();
		Optional<List<IC4WSA>> prevIC4WSAListOpt = ic4wsaRepostiory.findTop2ByRollAverageFlagIsTrueOrderByDateDesc();
		List<IC4WSACalculation> ic4wsaCalculationReference = ic4wsaCalculationRepository.findAll();
		HashMap<Date, IC4WSACalculation> ic4wsaCalculationHashMap = new HashMap<>();
		List<IC4WSA> ic4wsaList = new ArrayList<>();

		for (IC4WSACalculation ic4wsaCalculation : ic4wsaCalculationReference) {
			ic4wsaCalculationHashMap.put(ic4wsaCalculation.getToDate(), ic4wsaCalculation);
		}

		Queue<IC4WSA> ic4wsaQueue = new LinkedList<>();

		if (ic4wsaListOpt.isPresent()) {
			ic4wsaList = ic4wsaListOpt.get();
			if (prevIC4WSAListOpt.isPresent()) {
				ic4wsaList.addAll(prevIC4WSAListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(ic4wsaList, new SortByDateIC4WSA());

		for (IC4WSA ic4wsa : ic4wsaList) {

			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			if (ic4wsaQueue.size() == 3) {
				ic4wsaQueue.poll();
			}
			ic4wsaQueue.add(ic4wsa);
			if (ic4wsa.getRollAverageFlag()) {
				continue;
			}

			Iterator<IC4WSA> queueItr = ic4wsaQueue.iterator();

			IC4WSACalculation tempIC4WSACalculation = new IC4WSACalculation();
			if (ic4wsaCalculationHashMap.containsKey(ic4wsa.getDate())) {
				tempIC4WSACalculation = ic4wsaCalculationHashMap.get(ic4wsa.getDate());
			} else {
				tempIC4WSACalculation.setToDate(ic4wsa.getDate());
			}

			while (queueItr.hasNext()) {
				IC4WSA ic4wsaVal = queueItr.next();
				rollingAvg += ic4wsaVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			ic4wsa.setRollAverageFlag(true);
			tempIC4WSACalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			ic4wsaCalculationList.add(tempIC4WSACalculation);

		}

		ic4wsaCalculationReference = ic4wsaCalculationRepository.saveAll(ic4wsaCalculationList);
		ic4wsaList = ic4wsaRepostiory.saveAll(ic4wsaList);
		return;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public List<IC4WSA> getLatestIC4WSARecords() {

		if (NumberUtils.INTEGER_ZERO.equals(ic4wsaCalculationRepository.findAny())) {
			return null;
		}
		System.out.println("getLatestIC4WSARecords");
		Optional<IC4WSA> lastRecordOpt = ic4wsaRepostiory.findTopByOrderByDateDesc();
		List<IC4WSA> response = new ArrayList<>();
		if (lastRecordOpt.isPresent()) {
			IC4WSA lastRecord = lastRecordOpt.get();
			String lastDate = lastRecord.getDate().toString();
			String transactionUrl = QUANDL_HOST_URL + "FRED" + "/" + "IC4WSA" + "/" + QUANDL_DATA_FORMAT;

			UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(transactionUrl)
					// Add query parameter
					.queryParam("start_date", lastDate).queryParam("order", "ASC")
					.queryParam(QUANDL_API_KEY_NAME, QUANDL_API_KEY_VALUE);

			List<IC4WSA> IC4WSAList = new ArrayList<>();
			FederalResponse json = restUtility.consumeResponse(builder.toUriString());
			json.getDataset_data().getData().stream().forEach(o -> {
				ArrayList temp = (ArrayList) o;
				try {
					Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
					IC4WSAList.add(new IC4WSA(date, Float.parseFloat(temp.get(1).toString())));
					;
				} catch (ParseException e) {
					e.printStackTrace();
				}
			});

			if (IC4WSAList.size() > 1) { // As last record is already present in DB
				IC4WSAList.remove(0);
				response = ic4wsaRepostiory.saveAll(IC4WSAList);
				logger.info("New record inserted in IC4WSA");
			}

		}
		return response;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void updateRocChangeSignIC4WSA() {
		List<IC4WSACalculation> ic4wsaCalculationList = ic4wsaCalculationRepository
				.findAllByRocIsNotNullAndRocChangeSignIsNull();
		IC4WSACalculation lastUpdatedRecord = ic4wsaCalculationRepository
				.findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

		Collections.sort(ic4wsaCalculationList, new SortByDateIC4WSACalculation());

		if (ic4wsaCalculationList.size() == 0) {
			return;
		}

		Float lastRoc = lastUpdatedRecord.getRoc();
		for (IC4WSACalculation ic4wsaCalculation : ic4wsaCalculationList) {
			if (ic4wsaCalculation.getRoc() < lastRoc) {
				ic4wsaCalculation.setRocChangeSign(-1);
			} else if (ic4wsaCalculation.getRoc() > lastRoc) {
				ic4wsaCalculation.setRocChangeSign(1);
			} else if (ic4wsaCalculation.getRoc() == lastRoc) {
				ic4wsaCalculation.setRocChangeSign(0);
			}

			lastRoc = ic4wsaCalculation.getRoc();
		}

		ic4wsaCalculationRepository.saveAll(ic4wsaCalculationList);
	}
}
