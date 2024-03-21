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
import com.automation.trading.domain.calculation.MEHOINUSA672NCalculation;
import com.automation.trading.domain.fred.MEHOINUSA672N;
import com.automation.trading.repository.MEHOINUSA672NCalculationRepository;
import com.automation.trading.repository.MEHOINUSA672NRepository;
import com.automation.trading.service.FederalIncomeAndExpenditureService.SortByDateMEHOINUSA672N;
import com.automation.trading.service.FederalIncomeAndExpenditureService.SortByDateMEHOINUSA672NCalculation;
import com.automation.trading.service.MEHOINUSA672NService;
import com.automation.trading.utility.RestUtility;
@Service
public class MEHOINUSA672NUpdateService {
	
	@Autowired
	private MEHOINUSA672NRepository mehoinusa672nRepostiory;

	@Autowired
	private MEHOINUSA672NCalculationRepository mehoinusa672nCalculationRepository;

	@Autowired
	private MEHOINUSA672NService mehoinusa672nRateOfChangeService;

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

	private Logger logger = LoggerFactory.getLogger(MEHOINUSA672NUpdateService.class);

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRoc() {

		if (NumberUtils.INTEGER_ZERO.equals(mehoinusa672nCalculationRepository.findAny())) {
			mehoinusa672nRateOfChangeService.calculateRoc();
			mehoinusa672nRateOfChangeService.updateRocChangeSign();
		}

		System.out.println("calculateRocRollingAnnualAvg");

		Optional<List<MEHOINUSA672N>> mehoinusa672nListOpt = mehoinusa672nRepostiory.findByRocFlagIsFalseOrderByDate();
		Optional<MEHOINUSA672N> prevMEHOINUSA672NOpt = mehoinusa672nRepostiory.findFirstByRocFlagIsTrueOrderByDateDesc();
		HashMap<Date, MEHOINUSA672NCalculation> mehoinusa672nCalculationHashMap = new HashMap<>();
		MEHOINUSA672NCalculation prevMEHOINUSA672NCalculationRow = new MEHOINUSA672NCalculation();

		List<MEHOINUSA672N> mehoinusa672nList = new ArrayList<>();

		if (mehoinusa672nListOpt.isPresent()) {
			mehoinusa672nList = mehoinusa672nListOpt.get();
			if (prevMEHOINUSA672NOpt.isPresent()) {
				mehoinusa672nList.add(prevMEHOINUSA672NOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(mehoinusa672nList, new SortByDateMEHOINUSA672N());
		List<MEHOINUSA672NCalculation> mehoinusa672nCalculationReference = mehoinusa672nCalculationRepository.findAll();
		List<MEHOINUSA672NCalculation> mehoinusa672nCalculationModified = new ArrayList<>();
		Queue<MEHOINUSA672N> mehoinusa672nQueue = new LinkedList<>();

		for (MEHOINUSA672NCalculation mehoinusa672nCalculation : mehoinusa672nCalculationReference) {
			mehoinusa672nCalculationHashMap.put(mehoinusa672nCalculation.getToDate(), mehoinusa672nCalculation);
		}

		for (MEHOINUSA672N mehoinusa672n : mehoinusa672nList) {
			MEHOINUSA672NCalculation tempMEHOINUSA672NCalculation = new MEHOINUSA672NCalculation();

			if (mehoinusa672nQueue.size() == 2) {
				mehoinusa672nQueue.poll();
			}
			mehoinusa672nQueue.add(mehoinusa672n);

			if (mehoinusa672n.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;

			Iterator<MEHOINUSA672N> queueIterator = mehoinusa672nQueue.iterator();

			if (mehoinusa672nCalculationHashMap.containsKey(mehoinusa672n.getDate())) {
				tempMEHOINUSA672NCalculation = mehoinusa672nCalculationHashMap.get(mehoinusa672n.getDate());
			} else {
				tempMEHOINUSA672NCalculation.setToDate(mehoinusa672n.getDate());
			}

			while (queueIterator.hasNext()) {
				MEHOINUSA672N temp = queueIterator.next();
				temp.setRocFlag(true);
				if (mehoinusa672nQueue.size() == 1) {
					roc = 0f;
					tempMEHOINUSA672NCalculation.setRoc(roc);
					tempMEHOINUSA672NCalculation.setToDate(mehoinusa672n.getDate());
					tempMEHOINUSA672NCalculation.setRocChangeSign(0);
				} else {
					roc = (mehoinusa672n.getValue() / ((LinkedList<MEHOINUSA672N>) mehoinusa672nQueue).get(0).getValue()) - 1;
					tempMEHOINUSA672NCalculation.setRoc(roc);
					tempMEHOINUSA672NCalculation.setToDate(mehoinusa672n.getDate());
				}

			}

			mehoinusa672nCalculationModified.add(tempMEHOINUSA672NCalculation);
		}

		mehoinusa672nList = mehoinusa672nRepostiory.saveAll(mehoinusa672nList);
		mehoinusa672nCalculationModified = mehoinusa672nCalculationRepository.saveAll(mehoinusa672nCalculationModified);
		logger.debug("Added new MEHOINUSA672N row, " + mehoinusa672nCalculationModified);

		return;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRocRollingAnnualAvg() {

		if (NumberUtils.INTEGER_ZERO.equals(mehoinusa672nCalculationRepository.findAny())) {
			return;
		}

		System.out.println("calculateRocRollingAnnualAvg");
		Optional<List<MEHOINUSA672NCalculation>> mehoinusa672nCalculationListOpt = mehoinusa672nCalculationRepository
				.findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();
		Optional<List<MEHOINUSA672NCalculation>> prevMEHOINUSA672NCalculationListOpt = mehoinusa672nCalculationRepository
				.findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();
		List<MEHOINUSA672NCalculation> mehoinusa672nCalculationList = new ArrayList<>();

		if (mehoinusa672nCalculationListOpt.isPresent()) {
			mehoinusa672nCalculationList = mehoinusa672nCalculationListOpt.get();
			if (prevMEHOINUSA672NCalculationListOpt.isPresent()) {
				mehoinusa672nCalculationList.addAll(prevMEHOINUSA672NCalculationListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(mehoinusa672nCalculationList, new SortByDateMEHOINUSA672NCalculation());

		Queue<MEHOINUSA672NCalculation> mehoinusa672nCalculationPriorityQueue = new LinkedList<MEHOINUSA672NCalculation>();
		for (MEHOINUSA672NCalculation mehoinusa672nCalculation : mehoinusa672nCalculationList) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (mehoinusa672nCalculationPriorityQueue.size() == 4) {
				mehoinusa672nCalculationPriorityQueue.poll();
			}
			mehoinusa672nCalculationPriorityQueue.add(mehoinusa672nCalculation);

			if (mehoinusa672nCalculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<MEHOINUSA672NCalculation> queueIterator = mehoinusa672nCalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				MEHOINUSA672NCalculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			mehoinusa672nCalculation.setRocAnnRollAvgFlag(true);
			mehoinusa672nCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(mehoinusa672nCalculationList);
		mehoinusa672nCalculationList = mehoinusa672nCalculationRepository.saveAll(mehoinusa672nCalculationList);
		logger.info("New mehoinusa672n calculation record inserted" + mehoinusa672nCalculationList);
		return;

	}

	/**
	 * Calculates Rolling Average of Three Month MEHOINUSA672N
	 *
	 * @return MEHOINUSA672NCalculation , updated MEHOINUSA672NCalculation Table
	 */
	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRollAvgThreeMonth() {

		if (NumberUtils.INTEGER_ZERO.equals(mehoinusa672nCalculationRepository.findAny())) {
			return;
		}

		System.out.println("calculateRollAvgThreeMonth");

		List<MEHOINUSA672NCalculation> mehoinusa672nCalculationList = new ArrayList<>();
		Optional<List<MEHOINUSA672N>> mehoinusa672nListOpt = mehoinusa672nRepostiory.findByRollAverageFlagIsFalseOrderByDate();
		Optional<List<MEHOINUSA672N>> prevMEHOINUSA672NListOpt = mehoinusa672nRepostiory
				.findTop2ByRollAverageFlagIsTrueOrderByDateDesc();
		List<MEHOINUSA672NCalculation> mehoinusa672nCalculationReference = mehoinusa672nCalculationRepository.findAll();
		HashMap<Date, MEHOINUSA672NCalculation> mehoinusa672nCalculationHashMap = new HashMap<>();
		List<MEHOINUSA672N> mehoinusa672nList = new ArrayList<>();

		for (MEHOINUSA672NCalculation mehoinusa672nCalculation : mehoinusa672nCalculationReference) {
			mehoinusa672nCalculationHashMap.put(mehoinusa672nCalculation.getToDate(), mehoinusa672nCalculation);
		}

		Queue<MEHOINUSA672N> mehoinusa672nQueue = new LinkedList<>();

		if (mehoinusa672nListOpt.isPresent()) {
			mehoinusa672nList = mehoinusa672nListOpt.get();
			if (prevMEHOINUSA672NListOpt.isPresent()) {
				mehoinusa672nList.addAll(prevMEHOINUSA672NListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(mehoinusa672nList, new SortByDateMEHOINUSA672N());

		for (MEHOINUSA672N mehoinusa672n : mehoinusa672nList) {

			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			if (mehoinusa672nQueue.size() == 3) {
				mehoinusa672nQueue.poll();
			}
			mehoinusa672nQueue.add(mehoinusa672n);
			if (mehoinusa672n.getRollAverageFlag()) {
				continue;
			}

			Iterator<MEHOINUSA672N> queueItr = mehoinusa672nQueue.iterator();

			MEHOINUSA672NCalculation tempMEHOINUSA672NCalculation = new MEHOINUSA672NCalculation();
			if (mehoinusa672nCalculationHashMap.containsKey(mehoinusa672n.getDate())) {
				tempMEHOINUSA672NCalculation = mehoinusa672nCalculationHashMap.get(mehoinusa672n.getDate());
			} else {
				tempMEHOINUSA672NCalculation.setToDate(mehoinusa672n.getDate());
			}

			while (queueItr.hasNext()) {
				MEHOINUSA672N mehoinusa672nVal = queueItr.next();
				rollingAvg += mehoinusa672nVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			mehoinusa672n.setRollAverageFlag(true);
			tempMEHOINUSA672NCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			mehoinusa672nCalculationList.add(tempMEHOINUSA672NCalculation);

		}

		mehoinusa672nCalculationReference = mehoinusa672nCalculationRepository.saveAll(mehoinusa672nCalculationList);
		mehoinusa672nList = mehoinusa672nRepostiory.saveAll(mehoinusa672nList);
		return;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public List<MEHOINUSA672N> getLatestMEHOINUSA672NRecords() {

		if (NumberUtils.INTEGER_ZERO.equals(mehoinusa672nCalculationRepository.findAny())) {
			return null;
		}
		System.out.println("getLatestMEHOINUSA672NRecords");
		Optional<MEHOINUSA672N> lastRecordOpt = mehoinusa672nRepostiory.findTopByOrderByDateDesc();
		List<MEHOINUSA672N> response = new ArrayList<>();
		if (lastRecordOpt.isPresent()) {
			MEHOINUSA672N lastRecord = lastRecordOpt.get();
			String lastDate = lastRecord.getDate().toString();
			String transactionUrl = QUANDL_HOST_URL + "FRED" + "/" + "MEHOINUSA672N" + "/" + QUANDL_DATA_FORMAT;

			UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(transactionUrl)
					// Add query parameter
					.queryParam("start_date", lastDate).queryParam("order", "ASC")
					.queryParam(QUANDL_API_KEY_NAME, QUANDL_API_KEY_VALUE);

			List<MEHOINUSA672N> MEHOINUSA672NList = new ArrayList<>();
			FederalResponse json = restUtility.consumeResponse(builder.toUriString());
			json.getDataset_data().getData().stream().forEach(o -> {
				ArrayList temp = (ArrayList) o;
				try {
					Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
					MEHOINUSA672NList.add(new MEHOINUSA672N(date, Float.parseFloat(temp.get(1).toString())));
					;
				} catch (ParseException e) {
					e.printStackTrace();
				}
			});

			if (MEHOINUSA672NList.size() > 1) { // As last record is already present in DB
				MEHOINUSA672NList.remove(0);
				response = mehoinusa672nRepostiory.saveAll(MEHOINUSA672NList);
				logger.info("New record inserted in MEHOINUSA672N");
			}

		}
		return response;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void updateRocChangeSignDgs10() {
		List<MEHOINUSA672NCalculation> mehoinusa672nCalculationList = mehoinusa672nCalculationRepository.findAllByRocIsNotNullAndRocChangeSignIsNull();
		MEHOINUSA672NCalculation lastUpdatedRecord = mehoinusa672nCalculationRepository.findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

		Collections.sort(mehoinusa672nCalculationList, new SortByDateMEHOINUSA672NCalculation());

		if(mehoinusa672nCalculationList.size() == 0){
			return;
		}

		Float lastRoc = lastUpdatedRecord.getRoc();
		for (MEHOINUSA672NCalculation mehoinusa672nCalculation : mehoinusa672nCalculationList) {
			if(mehoinusa672nCalculation.getRoc() < lastRoc){
				mehoinusa672nCalculation.setRocChangeSign(-1);
			}else if (mehoinusa672nCalculation.getRoc() > lastRoc){
				mehoinusa672nCalculation.setRocChangeSign(1);
			}else if(mehoinusa672nCalculation.getRoc() == lastRoc){
				mehoinusa672nCalculation.setRocChangeSign(0);
			}

			lastRoc = mehoinusa672nCalculation.getRoc();
		}

		mehoinusa672nCalculationRepository.saveAll(mehoinusa672nCalculationList);
	}

}

