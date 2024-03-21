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
import com.automation.trading.domain.calculation.PSAVERTCalculation;
import com.automation.trading.domain.fred.PSAVERT;
import com.automation.trading.repository.PSAVERTCalculationRepository;
import com.automation.trading.repository.PSAVERTRepository;
import com.automation.trading.service.PSAVERTService;
import com.automation.trading.service.FederalIncomeAndExpenditureService.SortByDatePSAVERT;
import com.automation.trading.service.FederalIncomeAndExpenditureService.SortByDatePSAVERTCalculation;
import com.automation.trading.utility.RestUtility;

@Service
public class PSAVERTUpdateService {

	@Autowired
	private PSAVERTRepository psavertRepostiory;

	@Autowired
	private PSAVERTCalculationRepository psavertCalculationRepository;

	@Autowired
	private PSAVERTService psavertRateOfChangeService;

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

	private Logger logger = LoggerFactory.getLogger(PSAVERTUpdateService.class);

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRoc() {

		if (NumberUtils.INTEGER_ZERO.equals(psavertCalculationRepository.findAny())) {
			psavertRateOfChangeService.calculateRoc();
			psavertRateOfChangeService.updateRocChangeSign();
		}

		System.out.println("calculateRocRollingAnnualAvg");

		Optional<List<PSAVERT>> psavertListOpt = psavertRepostiory.findByRocFlagIsFalseOrderByDate();
		Optional<PSAVERT> prevPSAVERTOpt = psavertRepostiory.findFirstByRocFlagIsTrueOrderByDateDesc();
		HashMap<Date, PSAVERTCalculation> psavertCalculationHashMap = new HashMap<>();
		PSAVERTCalculation prevPSAVERTCalculationRow = new PSAVERTCalculation();

		List<PSAVERT> psavertList = new ArrayList<>();

		if (psavertListOpt.isPresent()) {
			psavertList = psavertListOpt.get();
			if (prevPSAVERTOpt.isPresent()) {
				psavertList.add(prevPSAVERTOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(psavertList, new SortByDatePSAVERT());
		List<PSAVERTCalculation> psavertCalculationReference = psavertCalculationRepository.findAll();
		List<PSAVERTCalculation> psavertCalculationModified = new ArrayList<>();
		Queue<PSAVERT> psavertQueue = new LinkedList<>();

		for (PSAVERTCalculation psavertCalculation : psavertCalculationReference) {
			psavertCalculationHashMap.put(psavertCalculation.getToDate(), psavertCalculation);
		}

		for (PSAVERT psavert : psavertList) {
			PSAVERTCalculation tempPSAVERTCalculation = new PSAVERTCalculation();

			if (psavertQueue.size() == 2) {
				psavertQueue.poll();
			}
			psavertQueue.add(psavert);

			if (psavert.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;

			Iterator<PSAVERT> queueIterator = psavertQueue.iterator();

			if (psavertCalculationHashMap.containsKey(psavert.getDate())) {
				tempPSAVERTCalculation = psavertCalculationHashMap.get(psavert.getDate());
			} else {
				tempPSAVERTCalculation.setToDate(psavert.getDate());
			}

			while (queueIterator.hasNext()) {
				PSAVERT temp = queueIterator.next();
				temp.setRocFlag(true);
				if (psavertQueue.size() == 1) {
					roc = 0f;
					tempPSAVERTCalculation.setRoc(roc);
					tempPSAVERTCalculation.setToDate(psavert.getDate());
					tempPSAVERTCalculation.setRocChangeSign(0);
				} else {
					roc = (psavert.getValue() / ((LinkedList<PSAVERT>) psavertQueue).get(0).getValue()) - 1;
					tempPSAVERTCalculation.setRoc(roc);
					tempPSAVERTCalculation.setToDate(psavert.getDate());
				}

			}

			psavertCalculationModified.add(tempPSAVERTCalculation);
		}

		psavertList = psavertRepostiory.saveAll(psavertList);
		psavertCalculationModified = psavertCalculationRepository.saveAll(psavertCalculationModified);
		logger.debug("Added new PSAVERT row, " + psavertCalculationModified);

		return;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRocRollingAnnualAvg() {

		if (NumberUtils.INTEGER_ZERO.equals(psavertCalculationRepository.findAny())) {
			return;
		}

		System.out.println("calculateRocRollingAnnualAvg");
		Optional<List<PSAVERTCalculation>> psavertCalculationListOpt = psavertCalculationRepository
				.findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();
		Optional<List<PSAVERTCalculation>> prevPSAVERTCalculationListOpt = psavertCalculationRepository
				.findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();
		List<PSAVERTCalculation> psavertCalculationList = new ArrayList<>();

		if (psavertCalculationListOpt.isPresent()) {
			psavertCalculationList = psavertCalculationListOpt.get();
			if (prevPSAVERTCalculationListOpt.isPresent()) {
				psavertCalculationList.addAll(prevPSAVERTCalculationListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(psavertCalculationList, new SortByDatePSAVERTCalculation());

		Queue<PSAVERTCalculation> psavertCalculationPriorityQueue = new LinkedList<PSAVERTCalculation>();
		for (PSAVERTCalculation psavertCalculation : psavertCalculationList) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (psavertCalculationPriorityQueue.size() == 4) {
				psavertCalculationPriorityQueue.poll();
			}
			psavertCalculationPriorityQueue.add(psavertCalculation);

			if (psavertCalculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<PSAVERTCalculation> queueIterator = psavertCalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				PSAVERTCalculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			psavertCalculation.setRocAnnRollAvgFlag(true);
			psavertCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(psavertCalculationList);
		psavertCalculationList = psavertCalculationRepository.saveAll(psavertCalculationList);
		logger.info("New psavert calculation record inserted" + psavertCalculationList);
		return;

	}

	/**
	 * Calculates Rolling Average of Three Month PSAVERT
	 *
	 * @return PSAVERTCalculation , updated PSAVERTCalculation Table
	 */
	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRollAvgThreeMonth() {

		if (NumberUtils.INTEGER_ZERO.equals(psavertCalculationRepository.findAny())) {
			return;
		}

		System.out.println("calculateRollAvgThreeMonth");

		List<PSAVERTCalculation> psavertCalculationList = new ArrayList<>();
		Optional<List<PSAVERT>> psavertListOpt = psavertRepostiory.findByRollAverageFlagIsFalseOrderByDate();
		Optional<List<PSAVERT>> prevPSAVERTListOpt = psavertRepostiory.findTop2ByRollAverageFlagIsTrueOrderByDateDesc();
		List<PSAVERTCalculation> psavertCalculationReference = psavertCalculationRepository.findAll();
		HashMap<Date, PSAVERTCalculation> psavertCalculationHashMap = new HashMap<>();
		List<PSAVERT> psavertList = new ArrayList<>();

		for (PSAVERTCalculation psavertCalculation : psavertCalculationReference) {
			psavertCalculationHashMap.put(psavertCalculation.getToDate(), psavertCalculation);
		}

		Queue<PSAVERT> psavertQueue = new LinkedList<>();

		if (psavertListOpt.isPresent()) {
			psavertList = psavertListOpt.get();
			if (prevPSAVERTListOpt.isPresent()) {
				psavertList.addAll(prevPSAVERTListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(psavertList, new SortByDatePSAVERT());

		for (PSAVERT psavert : psavertList) {

			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			if (psavertQueue.size() == 3) {
				psavertQueue.poll();
			}
			psavertQueue.add(psavert);
			if (psavert.getRollAverageFlag()) {
				continue;
			}

			Iterator<PSAVERT> queueItr = psavertQueue.iterator();

			PSAVERTCalculation tempPSAVERTCalculation = new PSAVERTCalculation();
			if (psavertCalculationHashMap.containsKey(psavert.getDate())) {
				tempPSAVERTCalculation = psavertCalculationHashMap.get(psavert.getDate());
			} else {
				tempPSAVERTCalculation.setToDate(psavert.getDate());
			}

			while (queueItr.hasNext()) {
				PSAVERT psavertVal = queueItr.next();
				rollingAvg += psavertVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			psavert.setRollAverageFlag(true);
			tempPSAVERTCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			psavertCalculationList.add(tempPSAVERTCalculation);

		}

		psavertCalculationReference = psavertCalculationRepository.saveAll(psavertCalculationList);
		psavertList = psavertRepostiory.saveAll(psavertList);
		return;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public List<PSAVERT> getLatestPSAVERTRecords() {

		if (NumberUtils.INTEGER_ZERO.equals(psavertCalculationRepository.findAny())) {
			return null;
		}
		System.out.println("getLatestPSAVERTRecords");
		Optional<PSAVERT> lastRecordOpt = psavertRepostiory.findTopByOrderByDateDesc();
		List<PSAVERT> response = new ArrayList<>();
		if (lastRecordOpt.isPresent()) {
			PSAVERT lastRecord = lastRecordOpt.get();
			String lastDate = lastRecord.getDate().toString();
			String transactionUrl = QUANDL_HOST_URL + "FRED" + "/" + "PSAVERT" + "/" + QUANDL_DATA_FORMAT;

			UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(transactionUrl)
					// Add query parameter
					.queryParam("start_date", lastDate).queryParam("order", "ASC")
					.queryParam(QUANDL_API_KEY_NAME, QUANDL_API_KEY_VALUE);

			List<PSAVERT> PSAVERTList = new ArrayList<>();
			FederalResponse json = restUtility.consumeResponse(builder.toUriString());
			json.getDataset_data().getData().stream().forEach(o -> {
				ArrayList temp = (ArrayList) o;
				try {
					Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
					PSAVERTList.add(new PSAVERT(date, Float.parseFloat(temp.get(1).toString())));
					;
				} catch (ParseException e) {
					e.printStackTrace();
				}
			});

			if (PSAVERTList.size() > 1) { // As last record is already present in DB
				PSAVERTList.remove(0);
				response = psavertRepostiory.saveAll(PSAVERTList);
				logger.info("New record inserted in PSAVERT");
			}

		}
		return response;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void updateRocChangeSignDgs10() {
		List<PSAVERTCalculation> psavertCalculationList = psavertCalculationRepository
				.findAllByRocIsNotNullAndRocChangeSignIsNull();
		PSAVERTCalculation lastUpdatedRecord = psavertCalculationRepository
				.findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

		Collections.sort(psavertCalculationList, new SortByDatePSAVERTCalculation());

		if (psavertCalculationList.size() == 0) {
			return;
		}

		Float lastRoc = lastUpdatedRecord.getRoc();
		for (PSAVERTCalculation psavertCalculation : psavertCalculationList) {
			if (psavertCalculation.getRoc() < lastRoc) {
				psavertCalculation.setRocChangeSign(-1);
			} else if (psavertCalculation.getRoc() > lastRoc) {
				psavertCalculation.setRocChangeSign(1);
			} else if (psavertCalculation.getRoc() == lastRoc) {
				psavertCalculation.setRocChangeSign(0);
			}

			lastRoc = psavertCalculation.getRoc();
		}

		psavertCalculationRepository.saveAll(psavertCalculationList);
	}

}
