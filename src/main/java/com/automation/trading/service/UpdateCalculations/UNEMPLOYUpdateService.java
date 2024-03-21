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
import org.springframework.web.util.UriComponentsBuilder;

import com.automation.trading.common.FederalResponse;
import com.automation.trading.domain.calculation.UNEMPLOYCalculation;
import com.automation.trading.domain.fred.UNEMPLOY;
import com.automation.trading.repository.UNEMPLOYCalculationRepository;
import com.automation.trading.repository.UNEMPLOYRepository;
import com.automation.trading.service.FederalEmploymentService.SortByDateUNEMPLOY;
import com.automation.trading.service.FederalEmploymentService.SortByDateUNEMPLOYCalculation;
import com.automation.trading.service.UNEMPLOYService;
import com.automation.trading.utility.RestUtility;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UNEMPLOYUpdateService {

	@Autowired
	private UNEMPLOYRepository unemployRepostiory;

	@Autowired
	private UNEMPLOYCalculationRepository unemployCalculationRepository;

	@Autowired
	private UNEMPLOYService unemployRateOfChangeService;

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

	private Logger logger = LoggerFactory.getLogger(UNEMPLOYUpdateService.class);

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRoc() {

		if (NumberUtils.INTEGER_ZERO.equals(unemployCalculationRepository.findAny())) {
			unemployRateOfChangeService.calculateRoc();
			unemployRateOfChangeService.updateRocChangeSignUNEMPLOY();
		}

		System.out.println("calculateRocRollingAnnualAvg");

		Optional<List<UNEMPLOY>> unemployListOpt = unemployRepostiory.findByRocFlagIsFalseOrderByDate();
		Optional<UNEMPLOY> prevUNEMPLOYOpt = unemployRepostiory.findFirstByRocFlagIsTrueOrderByDateDesc();
		HashMap<Date, UNEMPLOYCalculation> unemployCalculationHashMap = new HashMap<>();
		UNEMPLOYCalculation prevUNEMPLOYCalculationRow = new UNEMPLOYCalculation();

		List<UNEMPLOY> unemployList = new ArrayList<>();

		if (unemployListOpt.isPresent()) {
			unemployList = unemployListOpt.get();
			if (prevUNEMPLOYOpt.isPresent()) {
				unemployList.add(prevUNEMPLOYOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(unemployList, new SortByDateUNEMPLOY());
		List<UNEMPLOYCalculation> unemployCalculationReference = unemployCalculationRepository.findAll();
		List<UNEMPLOYCalculation> unemployCalculationModified = new ArrayList<>();
		Queue<UNEMPLOY> unemployQueue = new LinkedList<>();

		for (UNEMPLOYCalculation unemployCalculation : unemployCalculationReference) {
			unemployCalculationHashMap.put(unemployCalculation.getToDate(), unemployCalculation);
		}

		for (UNEMPLOY unemploy : unemployList) {
			UNEMPLOYCalculation tempUNEMPLOYCalculation = new UNEMPLOYCalculation();

			if (unemployQueue.size() == 2) {
				unemployQueue.poll();
			}
			unemployQueue.add(unemploy);

			if (unemploy.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;

			Iterator<UNEMPLOY> queueIterator = unemployQueue.iterator();

			if (unemployCalculationHashMap.containsKey(unemploy.getDate())) {
				tempUNEMPLOYCalculation = unemployCalculationHashMap.get(unemploy.getDate());
			} else {
				tempUNEMPLOYCalculation.setToDate(unemploy.getDate());
			}

			while (queueIterator.hasNext()) {
				UNEMPLOY temp = queueIterator.next();
				temp.setRocFlag(true);
				if (unemployQueue.size() == 1) {
					roc = 0f;
					tempUNEMPLOYCalculation.setRoc(roc);
					tempUNEMPLOYCalculation.setToDate(unemploy.getDate());
					tempUNEMPLOYCalculation.setRocChangeSign(0);
				} else {
					roc = (unemploy.getValue() / ((LinkedList<UNEMPLOY>) unemployQueue).get(0).getValue()) - 1;
					tempUNEMPLOYCalculation.setRoc(roc);
					tempUNEMPLOYCalculation.setToDate(unemploy.getDate());
				}

			}

			unemployCalculationModified.add(tempUNEMPLOYCalculation);
		}

		unemployList = unemployRepostiory.saveAll(unemployList);
		unemployCalculationModified = unemployCalculationRepository.saveAll(unemployCalculationModified);
		logger.debug("Added new UNEMPLOY row, " + unemployCalculationModified);

		return;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRocRollingAnnualAvg() {

		if (NumberUtils.INTEGER_ZERO.equals(unemployCalculationRepository.findAny())) {
			return;
		}

		System.out.println("calculateRocRollingAnnualAvg");
		Optional<List<UNEMPLOYCalculation>> unemployCalculationListOpt = unemployCalculationRepository
				.findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();
		Optional<List<UNEMPLOYCalculation>> prevUNEMPLOYCalculationListOpt = unemployCalculationRepository
				.findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();
		List<UNEMPLOYCalculation> unemployCalculationList = new ArrayList<>();

		if (unemployCalculationListOpt.isPresent()) {
			unemployCalculationList = unemployCalculationListOpt.get();
			if (prevUNEMPLOYCalculationListOpt.isPresent()) {
				unemployCalculationList.addAll(prevUNEMPLOYCalculationListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(unemployCalculationList, new SortByDateUNEMPLOYCalculation());

		Queue<UNEMPLOYCalculation> unemployCalculationPriorityQueue = new LinkedList<UNEMPLOYCalculation>();
		for (UNEMPLOYCalculation unemployCalculation : unemployCalculationList) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (unemployCalculationPriorityQueue.size() == 4) {
				unemployCalculationPriorityQueue.poll();
			}
			unemployCalculationPriorityQueue.add(unemployCalculation);

			if (unemployCalculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<UNEMPLOYCalculation> queueIterator = unemployCalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				UNEMPLOYCalculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			unemployCalculation.setRocAnnRollAvgFlag(true);
			unemployCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(unemployCalculationList);
		unemployCalculationList = unemployCalculationRepository.saveAll(unemployCalculationList);
		logger.info("New unemploy calculation record inserted" + unemployCalculationList);
		return;

	}

	/**
	 * Calculates Rolling Average of Three Month UNEMPLOY
	 *
	 * @return UNEMPLOYCalculation , updated UNEMPLOYCalculation Table
	 */
	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRollAvgThreeMonth() {

		if (NumberUtils.INTEGER_ZERO.equals(unemployCalculationRepository.findAny())) {
			return;
		}

		System.out.println("calculateRollAvgThreeMonth");

		List<UNEMPLOYCalculation> unemployCalculationList = new ArrayList<>();
		Optional<List<UNEMPLOY>> unemployListOpt = unemployRepostiory.findByRollAverageFlagIsFalseOrderByDate();
		Optional<List<UNEMPLOY>> prevUNEMPLOYListOpt = unemployRepostiory
				.findTop2ByRollAverageFlagIsTrueOrderByDateDesc();
		List<UNEMPLOYCalculation> unemployCalculationReference = unemployCalculationRepository.findAll();
		HashMap<Date, UNEMPLOYCalculation> unemployCalculationHashMap = new HashMap<>();
		List<UNEMPLOY> unemployList = new ArrayList<>();

		for (UNEMPLOYCalculation unemployCalculation : unemployCalculationReference) {
			unemployCalculationHashMap.put(unemployCalculation.getToDate(), unemployCalculation);
		}

		Queue<UNEMPLOY> unemployQueue = new LinkedList<>();

		if (unemployListOpt.isPresent()) {
			unemployList = unemployListOpt.get();
			if (prevUNEMPLOYListOpt.isPresent()) {
				unemployList.addAll(prevUNEMPLOYListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(unemployList, new SortByDateUNEMPLOY());

		for (UNEMPLOY unemploy : unemployList) {

			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			if (unemployQueue.size() == 3) {
				unemployQueue.poll();
			}
			unemployQueue.add(unemploy);
			if (unemploy.getRollAverageFlag()) {
				continue;
			}

			Iterator<UNEMPLOY> queueItr = unemployQueue.iterator();

			UNEMPLOYCalculation tempUNEMPLOYCalculation = new UNEMPLOYCalculation();
			if (unemployCalculationHashMap.containsKey(unemploy.getDate())) {
				tempUNEMPLOYCalculation = unemployCalculationHashMap.get(unemploy.getDate());
			} else {
				tempUNEMPLOYCalculation.setToDate(unemploy.getDate());
			}

			while (queueItr.hasNext()) {
				UNEMPLOY unemployVal = queueItr.next();
				rollingAvg += unemployVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			unemploy.setRollAverageFlag(true);
			tempUNEMPLOYCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			unemployCalculationList.add(tempUNEMPLOYCalculation);

		}

		unemployCalculationReference = unemployCalculationRepository.saveAll(unemployCalculationList);
		unemployList = unemployRepostiory.saveAll(unemployList);
		return;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public List<UNEMPLOY> getLatestUNEMPLOYRecords() {

		if (NumberUtils.INTEGER_ZERO.equals(unemployCalculationRepository.findAny())) {
			return null;
		}
		System.out.println("getLatestUNEMPLOYRecords");
		Optional<UNEMPLOY> lastRecordOpt = unemployRepostiory.findTopByOrderByDateDesc();
		List<UNEMPLOY> response = new ArrayList<>();
		if (lastRecordOpt.isPresent()) {
			UNEMPLOY lastRecord = lastRecordOpt.get();
			String lastDate = lastRecord.getDate().toString();
			String transactionUrl = QUANDL_HOST_URL + "FRED" + "/" + "UNEMPLOY" + "/" + QUANDL_DATA_FORMAT;

			UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(transactionUrl)
					// Add query parameter
					.queryParam("start_date", lastDate).queryParam("order", "ASC")
					.queryParam(QUANDL_API_KEY_NAME, QUANDL_API_KEY_VALUE);

			List<UNEMPLOY> UNEMPLOYList = new ArrayList<>();
			FederalResponse json = restUtility.consumeResponse(builder.toUriString());
			json.getDataset_data().getData().stream().forEach(o -> {
				ArrayList temp = (ArrayList<?>) o;
				try {
					Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
					UNEMPLOYList.add(new UNEMPLOY(date, Float.parseFloat(temp.get(1).toString())));
				} catch (ParseException e) {

					log.error("Error on UNEMPLOYService ======================================================>",
							e.getMessage());
				}
			});

			if (UNEMPLOYList.size() > 1) { // As last record is already present in DB
				UNEMPLOYList.remove(0);
				response = unemployRepostiory.saveAll(UNEMPLOYList);
				logger.info("New record inserted in UNEMPLOY");
			}

		}
		return response;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void updateRocChangeSignUNEMPLOY() {
		List<UNEMPLOYCalculation> unemployCalculationList = unemployCalculationRepository
				.findAllByRocIsNotNullAndRocChangeSignIsNull();
		UNEMPLOYCalculation lastUpdatedRecord = unemployCalculationRepository
				.findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

		Collections.sort(unemployCalculationList, new SortByDateUNEMPLOYCalculation());

		if (unemployCalculationList.isEmpty()) {
			return;
		}

		Float lastRoc = lastUpdatedRecord.getRoc();
		for (UNEMPLOYCalculation unemployCalculation : unemployCalculationList) {
			if (unemployCalculation.getRoc() < lastRoc) {
				unemployCalculation.setRocChangeSign(-1);
			} else if (unemployCalculation.getRoc() > lastRoc) {
				unemployCalculation.setRocChangeSign(1);
			} else if (unemployCalculation.getRoc().equals(lastRoc)) {
				unemployCalculation.setRocChangeSign(0);
			}

			lastRoc = unemployCalculation.getRoc();
		}

		unemployCalculationRepository.saveAll(unemployCalculationList);
	}

}
