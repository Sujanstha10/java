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

import com.automation.trading.domain.fred.EMRATIO;
import com.automation.trading.service.FederalEmploymentService;
import com.automation.trading.service.FederalInterestRateService;
import com.automation.trading.service.FederalReserveService;
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
import com.automation.trading.domain.calculation.EMRATIOCalculation;
import com.automation.trading.repository.EMRATIOCalculationRepository;
import com.automation.trading.repository.EMRATIORepository;
import com.automation.trading.service.EMRATIOService;

@Service
public class EMRATIOUpdateService {

	@Autowired
	private EMRATIORepository emratioRepostiory;

	@Autowired
	private EMRATIOCalculationRepository emratioCalculationRepository;

	@Autowired
	private EMRATIOService emratioRateOfChangeService;

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

	private Logger logger = LoggerFactory.getLogger(EMRATIOUpdateService.class);

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRoc() {

		if (NumberUtils.INTEGER_ZERO.equals(emratioCalculationRepository.findAny())) {
			emratioRateOfChangeService.calculateRoc();
			emratioRateOfChangeService.updateRocChangeSign();
		}

		System.out.println("calculateRocRollingAnnualAvg");

		Optional<List<EMRATIO>> emratioListOpt = emratioRepostiory.findByRocFlagIsFalseOrderByDate();
		Optional<EMRATIO> prevEMRATIOOpt = emratioRepostiory.findFirstByRocFlagIsTrueOrderByDateDesc();
		HashMap<Date, EMRATIOCalculation> emratioCalculationHashMap = new HashMap<>();
		EMRATIOCalculation prevEMRATIOCalculationRow = new EMRATIOCalculation();

		List<EMRATIO> emratioList = new ArrayList<>();

		if (emratioListOpt.isPresent()) {
			emratioList = emratioListOpt.get();
			if (prevEMRATIOOpt.isPresent()) {
				emratioList.add(prevEMRATIOOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(emratioList, new FederalEmploymentService.SortByDateEMRATIO());
		List<EMRATIOCalculation> emratioCalculationReference = emratioCalculationRepository.findAll();
		List<EMRATIOCalculation> emratioCalculationModified = new ArrayList<>();
		Queue<EMRATIO> emratioQueue = new LinkedList<>();

		for (EMRATIOCalculation emratioCalculation : emratioCalculationReference) {
			emratioCalculationHashMap.put(emratioCalculation.getToDate(), emratioCalculation);
		}

		for (EMRATIO emratio : emratioList) {
			EMRATIOCalculation tempEMRATIOCalculation = new EMRATIOCalculation();

			if (emratioQueue.size() == 2) {
				emratioQueue.poll();
			}
			emratioQueue.add(emratio);

			if (emratio.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;

			Iterator<EMRATIO> queueIterator = emratioQueue.iterator();

			if (emratioCalculationHashMap.containsKey(emratio.getDate())) {
				tempEMRATIOCalculation = emratioCalculationHashMap.get(emratio.getDate());
			} else {
				tempEMRATIOCalculation.setToDate(emratio.getDate());
			}

			while (queueIterator.hasNext()) {
				EMRATIO temp = queueIterator.next();
				temp.setRocFlag(true);
				if (emratioQueue.size() == 1) {
					roc = 0f;
					tempEMRATIOCalculation.setRoc(roc);
					tempEMRATIOCalculation.setToDate(emratio.getDate());
					tempEMRATIOCalculation.setRocChangeSign(0);
				} else {
					roc = (emratio.getValue() / ((LinkedList<EMRATIO>) emratioQueue).get(0).getValue()) - 1;
					tempEMRATIOCalculation.setRoc(roc);
					tempEMRATIOCalculation.setToDate(emratio.getDate());
				}

			}

			emratioCalculationModified.add(tempEMRATIOCalculation);
		}

		emratioList = emratioRepostiory.saveAll(emratioList);
		emratioCalculationModified = emratioCalculationRepository.saveAll(emratioCalculationModified);
		logger.debug("Added new EMRATIO row, " + emratioCalculationModified);

		return;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRocRollingAnnualAvg() {

		if (NumberUtils.INTEGER_ZERO.equals(emratioCalculationRepository.findAny())) {
			return;
		}

		System.out.println("calculateRocRollingAnnualAvg");
		Optional<List<EMRATIOCalculation>> emratioCalculationListOpt = emratioCalculationRepository
				.findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();
		Optional<List<EMRATIOCalculation>> prevEMRATIOCalculationListOpt = emratioCalculationRepository
				.findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();
		List<EMRATIOCalculation> emratioCalculationList = new ArrayList<>();

		if (emratioCalculationListOpt.isPresent()) {
			emratioCalculationList = emratioCalculationListOpt.get();
			if (prevEMRATIOCalculationListOpt.isPresent()) {
				emratioCalculationList.addAll(prevEMRATIOCalculationListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(emratioCalculationList, new FederalEmploymentService.SortByDateEMRATIOCalculation());

		Queue<EMRATIOCalculation> emratioCalculationPriorityQueue = new LinkedList<EMRATIOCalculation>();
		for (EMRATIOCalculation emratioCalculation : emratioCalculationList) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (emratioCalculationPriorityQueue.size() == 4) {
				emratioCalculationPriorityQueue.poll();
			}
			emratioCalculationPriorityQueue.add(emratioCalculation);

			if (emratioCalculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<EMRATIOCalculation> queueIterator = emratioCalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				EMRATIOCalculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			emratioCalculation.setRocAnnRollAvgFlag(true);
			emratioCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(emratioCalculationList);
		emratioCalculationList = emratioCalculationRepository.saveAll(emratioCalculationList);
		logger.info("New emratio calculation record inserted" + emratioCalculationList);
		return;

	}

	/**
	 * Calculates Rolling Average of Three Month EMRATIO
	 *
	 * @return EMRATIOCalculation , updated EMRATIOCalculation Table
	 */
	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRollAvgThreeMonth() {

		if (NumberUtils.INTEGER_ZERO.equals(emratioCalculationRepository.findAny())) {
			return;
		}

		System.out.println("calculateRollAvgThreeMonth");

		List<EMRATIOCalculation> emratioCalculationList = new ArrayList<>();
		Optional<List<EMRATIO>> emratioListOpt = emratioRepostiory.findByRollAverageFlagIsFalseOrderByDate();
		Optional<List<EMRATIO>> prevEMRATIOListOpt = emratioRepostiory
				.findTop2ByRollAverageFlagIsTrueOrderByDateDesc();
		List<EMRATIOCalculation> emratioCalculationReference = emratioCalculationRepository.findAll();
		HashMap<Date, EMRATIOCalculation> emratioCalculationHashMap = new HashMap<>();
		List<EMRATIO> emratioList = new ArrayList<>();

		for (EMRATIOCalculation emratioCalculation : emratioCalculationReference) {
			emratioCalculationHashMap.put(emratioCalculation.getToDate(), emratioCalculation);
		}

		Queue<EMRATIO> emratioQueue = new LinkedList<>();

		if (emratioListOpt.isPresent()) {
			emratioList = emratioListOpt.get();
			if (prevEMRATIOListOpt.isPresent()) {
				emratioList.addAll(prevEMRATIOListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(emratioList, new FederalEmploymentService.SortByDateEMRATIO());

		for (EMRATIO emratio : emratioList) {

			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			if (emratioQueue.size() == 3) {
				emratioQueue.poll();
			}
			emratioQueue.add(emratio);
			if (emratio.getRollAverageFlag()) {
				continue;
			}

			Iterator<EMRATIO> queueItr = emratioQueue.iterator();

			EMRATIOCalculation tempEMRATIOCalculation = new EMRATIOCalculation();
			if (emratioCalculationHashMap.containsKey(emratio.getDate())) {
				tempEMRATIOCalculation = emratioCalculationHashMap.get(emratio.getDate());
			} else {
				tempEMRATIOCalculation.setToDate(emratio.getDate());
			}

			while (queueItr.hasNext()) {
				EMRATIO emratioVal = queueItr.next();
				rollingAvg += emratioVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			emratio.setRollAverageFlag(true);
			tempEMRATIOCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			emratioCalculationList.add(tempEMRATIOCalculation);

		}

		emratioCalculationReference = emratioCalculationRepository.saveAll(emratioCalculationList);
		emratioList = emratioRepostiory.saveAll(emratioList);
		return;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public List<EMRATIO> getLatestEMRATIORecords() {

		if (NumberUtils.INTEGER_ZERO.equals(emratioCalculationRepository.findAny())) {
			return null;
		}
		System.out.println("getLatestEMRATIORecords");
		Optional<EMRATIO> lastRecordOpt = emratioRepostiory.findTopByOrderByDateDesc();
		List<EMRATIO> response = new ArrayList<>();
		if (lastRecordOpt.isPresent()) {
			EMRATIO lastRecord = lastRecordOpt.get();
			String lastDate = lastRecord.getDate().toString();
			String transactionUrl = QUANDL_HOST_URL + "FRED" + "/" + "EMRATIO" + "/" + QUANDL_DATA_FORMAT;

			UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(transactionUrl)
					// Add query parameter
					.queryParam("start_date", lastDate).queryParam("order", "ASC")
					.queryParam(QUANDL_API_KEY_NAME, QUANDL_API_KEY_VALUE);

			List<EMRATIO> EMRATIOList = new ArrayList<>();
			FederalResponse json = restUtility.consumeResponse(builder.toUriString());
			json.getDataset_data().getData().stream().forEach(o -> {
				ArrayList temp = (ArrayList) o;
				try {
					Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
					EMRATIOList.add(new EMRATIO(date, Float.parseFloat(temp.get(1).toString())));
					;
				} catch (ParseException e) {
					e.printStackTrace();
				}
			});

			if (EMRATIOList.size() > 1) { // As last record is already present in DB
				EMRATIOList.remove(0);
				response = emratioRepostiory.saveAll(EMRATIOList);
				logger.info("New record inserted in EMRATIO");
			}

		}
		return response;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void updateRocChangeSignDgs10() {
		List<EMRATIOCalculation> emratioCalculationList = emratioCalculationRepository.findAllByRocIsNotNullAndRocChangeSignIsNull();
		EMRATIOCalculation lastUpdatedRecord = emratioCalculationRepository.findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

		Collections.sort(emratioCalculationList, new FederalEmploymentService.SortByDateEMRATIOCalculation());

		if(emratioCalculationList.size() == 0){
			return;
		}

		Float lastRoc = lastUpdatedRecord.getRoc();
		for (EMRATIOCalculation emratioCalculation : emratioCalculationList) {
			if(emratioCalculation.getRoc() < lastRoc){
				emratioCalculation.setRocChangeSign(-1);
			}else if (emratioCalculation.getRoc() > lastRoc){
				emratioCalculation.setRocChangeSign(1);
			}else if(emratioCalculation.getRoc() == lastRoc){
				emratioCalculation.setRocChangeSign(0);
			}

			lastRoc = emratioCalculation.getRoc();
		}

		emratioCalculationRepository.saveAll(emratioCalculationList);
	}


}
