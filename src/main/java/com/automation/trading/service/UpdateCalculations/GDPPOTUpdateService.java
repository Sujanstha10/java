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

import com.automation.trading.domain.fred.GDPPOT;
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
import com.automation.trading.domain.calculation.GDPPOTCalculation;
import com.automation.trading.repository.GDPPOTCalculationRepository;
import com.automation.trading.repository.GDPPOTRepository;
import com.automation.trading.service.GDPPOTService;

@Service
public class GDPPOTUpdateService {

	@Autowired
	private GDPPOTRepository gdppotRepostiory;

	@Autowired
	private GDPPOTCalculationRepository gdppotCalculationRepository;

	@Autowired
	private GDPPOTService gdppotRateOfChangeService;

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

	private Logger logger = LoggerFactory.getLogger(GDPPOTUpdateService.class);

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRoc() {

		if (NumberUtils.INTEGER_ZERO.equals(gdppotCalculationRepository.findAny())) {
			gdppotRateOfChangeService.calculateRoc();
			gdppotRateOfChangeService.updateRocChangeSign();
		}

		System.out.println("calculateRocRollingAnnualAvg");

		Optional<List<GDPPOT>> gdppotListOpt = gdppotRepostiory.findByRocFlagIsFalseOrderByDate();
		Optional<GDPPOT> prevGDPPOTOpt = gdppotRepostiory.findFirstByRocFlagIsTrueOrderByDateDesc();
		HashMap<Date, GDPPOTCalculation> gdppotCalculationHashMap = new HashMap<>();
		GDPPOTCalculation prevGDPPOTCalculationRow = new GDPPOTCalculation();

		List<GDPPOT> gdppotList = new ArrayList<>();

		if (gdppotListOpt.isPresent()) {
			gdppotList = gdppotListOpt.get();
			if (prevGDPPOTOpt.isPresent()) {
				gdppotList.add(prevGDPPOTOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(gdppotList, new FederalReserveService.SortByDateGDPPOT());
		List<GDPPOTCalculation> gdppotCalculationReference = gdppotCalculationRepository.findAll();
		List<GDPPOTCalculation> gdppotCalculationModified = new ArrayList<>();
		Queue<GDPPOT> gdppotQueue = new LinkedList<>();

		for (GDPPOTCalculation gdppotCalculation : gdppotCalculationReference) {
			gdppotCalculationHashMap.put(gdppotCalculation.getToDate(), gdppotCalculation);
		}

		for (GDPPOT gdppot : gdppotList) {
			GDPPOTCalculation tempGDPPOTCalculation = new GDPPOTCalculation();

			if (gdppotQueue.size() == 2) {
				gdppotQueue.poll();
			}
			gdppotQueue.add(gdppot);

			if (gdppot.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;

			Iterator<GDPPOT> queueIterator = gdppotQueue.iterator();

			if (gdppotCalculationHashMap.containsKey(gdppot.getDate())) {
				tempGDPPOTCalculation = gdppotCalculationHashMap.get(gdppot.getDate());
			} else {
				tempGDPPOTCalculation.setToDate(gdppot.getDate());
			}

			while (queueIterator.hasNext()) {
				GDPPOT temp = queueIterator.next();
				temp.setRocFlag(true);
				if (gdppotQueue.size() == 1) {
					roc = 0f;
					tempGDPPOTCalculation.setRoc(roc);
					tempGDPPOTCalculation.setToDate(gdppot.getDate());
					tempGDPPOTCalculation.setRocChangeSign(0);
				} else {
					roc = (gdppot.getValue() / ((LinkedList<GDPPOT>) gdppotQueue).get(0).getValue()) - 1;
					tempGDPPOTCalculation.setRoc(roc);
					tempGDPPOTCalculation.setToDate(gdppot.getDate());
				}

			}

			gdppotCalculationModified.add(tempGDPPOTCalculation);
		}

		gdppotList = gdppotRepostiory.saveAll(gdppotList);
		gdppotCalculationModified = gdppotCalculationRepository.saveAll(gdppotCalculationModified);
		logger.debug("Added new GDPPOT row, " + gdppotCalculationModified);

		return;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRocRollingAnnualAvg() {

		if (NumberUtils.INTEGER_ZERO.equals(gdppotCalculationRepository.findAny())) {
			return;
		}

		System.out.println("calculateRocRollingAnnualAvg");
		Optional<List<GDPPOTCalculation>> gdppotCalculationListOpt = gdppotCalculationRepository
				.findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();
		Optional<List<GDPPOTCalculation>> prevGDPPOTCalculationListOpt = gdppotCalculationRepository
				.findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();
		List<GDPPOTCalculation> gdppotCalculationList = new ArrayList<>();

		if (gdppotCalculationListOpt.isPresent()) {
			gdppotCalculationList = gdppotCalculationListOpt.get();
			if (prevGDPPOTCalculationListOpt.isPresent()) {
				gdppotCalculationList.addAll(prevGDPPOTCalculationListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(gdppotCalculationList, new FederalReserveService.SortByDateGDPPOTCalculation());

		Queue<GDPPOTCalculation> gdppotCalculationPriorityQueue = new LinkedList<GDPPOTCalculation>();
		for (GDPPOTCalculation gdppotCalculation : gdppotCalculationList) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (gdppotCalculationPriorityQueue.size() == 4) {
				gdppotCalculationPriorityQueue.poll();
			}
			gdppotCalculationPriorityQueue.add(gdppotCalculation);

			if (gdppotCalculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<GDPPOTCalculation> queueIterator = gdppotCalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				GDPPOTCalculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			gdppotCalculation.setRocAnnRollAvgFlag(true);
			gdppotCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(gdppotCalculationList);
		gdppotCalculationList = gdppotCalculationRepository.saveAll(gdppotCalculationList);
		logger.info("New gdppot calculation record inserted" + gdppotCalculationList);
		return;

	}

	/**
	 * Calculates Rolling Average of Three Month GDPPOT
	 *
	 * @return GDPPOTCalculation , updated GDPPOTCalculation Table
	 */
	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRollAvgThreeMonth() {

		if (NumberUtils.INTEGER_ZERO.equals(gdppotCalculationRepository.findAny())) {
			return;
		}

		System.out.println("calculateRollAvgThreeMonth");

		List<GDPPOTCalculation> gdppotCalculationList = new ArrayList<>();
		Optional<List<GDPPOT>> gdppotListOpt = gdppotRepostiory.findByRollAverageFlagIsFalseOrderByDate();
		Optional<List<GDPPOT>> prevGDPPOTListOpt = gdppotRepostiory
				.findTop2ByRollAverageFlagIsTrueOrderByDateDesc();
		List<GDPPOTCalculation> gdppotCalculationReference = gdppotCalculationRepository.findAll();
		HashMap<Date, GDPPOTCalculation> gdppotCalculationHashMap = new HashMap<>();
		List<GDPPOT> gdppotList = new ArrayList<>();

		for (GDPPOTCalculation gdppotCalculation : gdppotCalculationReference) {
			gdppotCalculationHashMap.put(gdppotCalculation.getToDate(), gdppotCalculation);
		}

		Queue<GDPPOT> gdppotQueue = new LinkedList<>();

		if (gdppotListOpt.isPresent()) {
			gdppotList = gdppotListOpt.get();
			if (prevGDPPOTListOpt.isPresent()) {
				gdppotList.addAll(prevGDPPOTListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(gdppotList, new FederalReserveService.SortByDateGDPPOT());

		for (GDPPOT gdppot : gdppotList) {

			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			if (gdppotQueue.size() == 3) {
				gdppotQueue.poll();
			}
			gdppotQueue.add(gdppot);
			if (gdppot.getRollAverageFlag()) {
				continue;
			}

			Iterator<GDPPOT> queueItr = gdppotQueue.iterator();

			GDPPOTCalculation tempGDPPOTCalculation = new GDPPOTCalculation();
			if (gdppotCalculationHashMap.containsKey(gdppot.getDate())) {
				tempGDPPOTCalculation = gdppotCalculationHashMap.get(gdppot.getDate());
			} else {
				tempGDPPOTCalculation.setToDate(gdppot.getDate());
			}

			while (queueItr.hasNext()) {
				GDPPOT gdppotVal = queueItr.next();
				rollingAvg += gdppotVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			gdppot.setRollAverageFlag(true);
			tempGDPPOTCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			gdppotCalculationList.add(tempGDPPOTCalculation);

		}

		gdppotCalculationReference = gdppotCalculationRepository.saveAll(gdppotCalculationList);
		gdppotList = gdppotRepostiory.saveAll(gdppotList);
		return;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public List<GDPPOT> getLatestGDPPOTRecords() {

		if (NumberUtils.INTEGER_ZERO.equals(gdppotCalculationRepository.findAny())) {
			return null;
		}
		System.out.println("getLatestGDPPOTRecords");
		Optional<GDPPOT> lastRecordOpt = gdppotRepostiory.findTopByOrderByDateDesc();
		List<GDPPOT> response = new ArrayList<>();
		if (lastRecordOpt.isPresent()) {
			GDPPOT lastRecord = lastRecordOpt.get();
			String lastDate = lastRecord.getDate().toString();
			String transactionUrl = QUANDL_HOST_URL + "FRED" + "/" + "GDPPOT" + "/" + QUANDL_DATA_FORMAT;

			UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(transactionUrl)
					// Add query parameter
					.queryParam("start_date", lastDate).queryParam("order", "ASC")
					.queryParam(QUANDL_API_KEY_NAME, QUANDL_API_KEY_VALUE);

			List<GDPPOT> GDPPOTList = new ArrayList<>();
			FederalResponse json = restUtility.consumeResponse(builder.toUriString());
			json.getDataset_data().getData().stream().forEach(o -> {
				ArrayList temp = (ArrayList) o;
				try {
					Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
					GDPPOTList.add(new GDPPOT(date, Float.parseFloat(temp.get(1).toString())));
					;
				} catch (ParseException e) {
					e.printStackTrace();
				}
			});

			if (GDPPOTList.size() > 1) { // As last record is already present in DB
				GDPPOTList.remove(0);
				response = gdppotRepostiory.saveAll(GDPPOTList);
				logger.info("New record inserted in GDPPOT");
			}

		}
		return response;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void updateRocChangeSignDgs10() {
		List<GDPPOTCalculation> gdppotCalculationList = gdppotCalculationRepository.findAllByRocIsNotNullAndRocChangeSignIsNull();
		GDPPOTCalculation lastUpdatedRecord = gdppotCalculationRepository.findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

		Collections.sort(gdppotCalculationList, new FederalReserveService.SortByDateGDPPOTCalculation());

		if(gdppotCalculationList.size() == 0){
			return;
		}

		Float lastRoc = lastUpdatedRecord.getRoc();
		for (GDPPOTCalculation gdppotCalculation : gdppotCalculationList) {
			if(gdppotCalculation.getRoc() < lastRoc){
				gdppotCalculation.setRocChangeSign(-1);
			}else if (gdppotCalculation.getRoc() > lastRoc){
				gdppotCalculation.setRocChangeSign(1);
			}else if(gdppotCalculation.getRoc() == lastRoc){
				gdppotCalculation.setRocChangeSign(0);
			}

			lastRoc = gdppotCalculation.getRoc();
		}

		gdppotCalculationRepository.saveAll(gdppotCalculationList);
	}


}
