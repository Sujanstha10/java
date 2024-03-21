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

import com.automation.trading.domain.fred.GDPDEF;

import com.automation.trading.service.FederalPricesAndInflationService;
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
import com.automation.trading.domain.calculation.GDPDEFCalculation;
import com.automation.trading.repository.GDPDEFCalculationRepository;
import com.automation.trading.repository.GDPDEFRepository;
import com.automation.trading.service.GDPDEFService;

@Service
public class GDPDEFUpdateService {

	@Autowired
	private GDPDEFRepository gdpdefRepostiory;

	@Autowired
	private GDPDEFCalculationRepository gdpdefCalculationRepository;

	@Autowired
	private GDPDEFService gdpdefRateOfChangeService;

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

	private Logger logger = LoggerFactory.getLogger(GDPDEFUpdateService.class);

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRoc() {

		if (NumberUtils.INTEGER_ZERO.equals(gdpdefCalculationRepository.findAny())) {
			gdpdefRateOfChangeService.calculateRoc();
			gdpdefRateOfChangeService.updateRocChangeSign();
		}

		System.out.println("calculateRocRollingAnnualAvg");

		Optional<List<GDPDEF>> gdpdefListOpt = gdpdefRepostiory.findByRocFlagIsFalseOrderByDate();
		Optional<GDPDEF> prevGDPDEFOpt = gdpdefRepostiory.findFirstByRocFlagIsTrueOrderByDateDesc();
		HashMap<Date, GDPDEFCalculation> gdpdefCalculationHashMap = new HashMap<>();
		GDPDEFCalculation prevGDPDEFCalculationRow = new GDPDEFCalculation();

		List<GDPDEF> gdpdefList = new ArrayList<>();

		if (gdpdefListOpt.isPresent()) {
			gdpdefList = gdpdefListOpt.get();
			if (prevGDPDEFOpt.isPresent()) {
				gdpdefList.add(prevGDPDEFOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(gdpdefList, new FederalPricesAndInflationService.SortByDateGDPDEF());
		List<GDPDEFCalculation> gdpdefCalculationReference = gdpdefCalculationRepository.findAll();
		List<GDPDEFCalculation> gdpdefCalculationModified = new ArrayList<>();
		Queue<GDPDEF> gdpdefQueue = new LinkedList<>();

		for (GDPDEFCalculation gdpdefCalculation : gdpdefCalculationReference) {
			gdpdefCalculationHashMap.put(gdpdefCalculation.getToDate(), gdpdefCalculation);
		}

		for (GDPDEF gdpdef : gdpdefList) {
			GDPDEFCalculation tempGDPDEFCalculation = new GDPDEFCalculation();

			if (gdpdefQueue.size() == 2) {
				gdpdefQueue.poll();
			}
			gdpdefQueue.add(gdpdef);

			if (gdpdef.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;

			Iterator<GDPDEF> queueIterator = gdpdefQueue.iterator();

			if (gdpdefCalculationHashMap.containsKey(gdpdef.getDate())) {
				tempGDPDEFCalculation = gdpdefCalculationHashMap.get(gdpdef.getDate());
			} else {
				tempGDPDEFCalculation.setToDate(gdpdef.getDate());
			}

			while (queueIterator.hasNext()) {
				GDPDEF temp = queueIterator.next();
				temp.setRocFlag(true);
				if (gdpdefQueue.size() == 1) {
					roc = 0f;
					tempGDPDEFCalculation.setRoc(roc);
					tempGDPDEFCalculation.setToDate(gdpdef.getDate());
					tempGDPDEFCalculation.setRocChangeSign(0);
				} else {
					roc = (gdpdef.getValue() / ((LinkedList<GDPDEF>) gdpdefQueue).get(0).getValue()) - 1;
					tempGDPDEFCalculation.setRoc(roc);
					tempGDPDEFCalculation.setToDate(gdpdef.getDate());
				}

			}

			gdpdefCalculationModified.add(tempGDPDEFCalculation);
		}

		gdpdefList = gdpdefRepostiory.saveAll(gdpdefList);
		gdpdefCalculationModified = gdpdefCalculationRepository.saveAll(gdpdefCalculationModified);
		logger.debug("Added new GDPDEF row, " + gdpdefCalculationModified);

		return;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRocRollingAnnualAvg() {

		if (NumberUtils.INTEGER_ZERO.equals(gdpdefCalculationRepository.findAny())) {
			return;
		}

		System.out.println("calculateRocRollingAnnualAvg");
		Optional<List<GDPDEFCalculation>> gdpdefCalculationListOpt = gdpdefCalculationRepository
				.findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();
		Optional<List<GDPDEFCalculation>> prevGDPDEFCalculationListOpt = gdpdefCalculationRepository
				.findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();
		List<GDPDEFCalculation> gdpdefCalculationList = new ArrayList<>();

		if (gdpdefCalculationListOpt.isPresent()) {
			gdpdefCalculationList = gdpdefCalculationListOpt.get();
			if (prevGDPDEFCalculationListOpt.isPresent()) {
				gdpdefCalculationList.addAll(prevGDPDEFCalculationListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(gdpdefCalculationList, new FederalPricesAndInflationService.SortByDateGDPDEFCalculation());

		Queue<GDPDEFCalculation> gdpdefCalculationPriorityQueue = new LinkedList<GDPDEFCalculation>();
		for (GDPDEFCalculation gdpdefCalculation : gdpdefCalculationList) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (gdpdefCalculationPriorityQueue.size() == 4) {
				gdpdefCalculationPriorityQueue.poll();
			}
			gdpdefCalculationPriorityQueue.add(gdpdefCalculation);

			if (gdpdefCalculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<GDPDEFCalculation> queueIterator = gdpdefCalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				GDPDEFCalculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			gdpdefCalculation.setRocAnnRollAvgFlag(true);
			gdpdefCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(gdpdefCalculationList);
		gdpdefCalculationList = gdpdefCalculationRepository.saveAll(gdpdefCalculationList);
		logger.info("New gdpdef calculation record inserted" + gdpdefCalculationList);
		return;

	}

	/**
	 * Calculates Rolling Average of Three Month GDPDEF
	 *
	 * @return GDPDEFCalculation , updated GDPDEFCalculation Table
	 */
	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRollAvgThreeMonth() {

		if (NumberUtils.INTEGER_ZERO.equals(gdpdefCalculationRepository.findAny())) {
			return;
		}

		System.out.println("calculateRollAvgThreeMonth");

		List<GDPDEFCalculation> gdpdefCalculationList = new ArrayList<>();
		Optional<List<GDPDEF>> gdpdefListOpt = gdpdefRepostiory.findByRollAverageFlagIsFalseOrderByDate();
		Optional<List<GDPDEF>> prevGDPDEFListOpt = gdpdefRepostiory
				.findTop2ByRollAverageFlagIsTrueOrderByDateDesc();
		List<GDPDEFCalculation> gdpdefCalculationReference = gdpdefCalculationRepository.findAll();
		HashMap<Date, GDPDEFCalculation> gdpdefCalculationHashMap = new HashMap<>();
		List<GDPDEF> gdpdefList = new ArrayList<>();

		for (GDPDEFCalculation gdpdefCalculation : gdpdefCalculationReference) {
			gdpdefCalculationHashMap.put(gdpdefCalculation.getToDate(), gdpdefCalculation);
		}

		Queue<GDPDEF> gdpdefQueue = new LinkedList<>();

		if (gdpdefListOpt.isPresent()) {
			gdpdefList = gdpdefListOpt.get();
			if (prevGDPDEFListOpt.isPresent()) {
				gdpdefList.addAll(prevGDPDEFListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(gdpdefList, new FederalPricesAndInflationService.SortByDateGDPDEF());

		for (GDPDEF gdpdef : gdpdefList) {

			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			if (gdpdefQueue.size() == 3) {
				gdpdefQueue.poll();
			}
			gdpdefQueue.add(gdpdef);
			if (gdpdef.getRollAverageFlag()) {
				continue;
			}

			Iterator<GDPDEF> queueItr = gdpdefQueue.iterator();

			GDPDEFCalculation tempGDPDEFCalculation = new GDPDEFCalculation();
			if (gdpdefCalculationHashMap.containsKey(gdpdef.getDate())) {
				tempGDPDEFCalculation = gdpdefCalculationHashMap.get(gdpdef.getDate());
			} else {
				tempGDPDEFCalculation.setToDate(gdpdef.getDate());
			}

			while (queueItr.hasNext()) {
				GDPDEF gdpdefVal = queueItr.next();
				rollingAvg += gdpdefVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			gdpdef.setRollAverageFlag(true);
			tempGDPDEFCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			gdpdefCalculationList.add(tempGDPDEFCalculation);

		}

		gdpdefCalculationReference = gdpdefCalculationRepository.saveAll(gdpdefCalculationList);
		gdpdefList = gdpdefRepostiory.saveAll(gdpdefList);
		return;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public List<GDPDEF> getLatestGDPDEFRecords() {

		if (NumberUtils.INTEGER_ZERO.equals(gdpdefCalculationRepository.findAny())) {
			return null;
		}
		System.out.println("getLatestGDPDEFRecords");
		Optional<GDPDEF> lastRecordOpt = gdpdefRepostiory.findTopByOrderByDateDesc();
		List<GDPDEF> response = new ArrayList<>();
		if (lastRecordOpt.isPresent()) {
			GDPDEF lastRecord = lastRecordOpt.get();
			String lastDate = lastRecord.getDate().toString();
			String transactionUrl = QUANDL_HOST_URL + "FRED" + "/" + "GDPDEF" + "/" + QUANDL_DATA_FORMAT;

			UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(transactionUrl)
					// Add query parameter
					.queryParam("start_date", lastDate).queryParam("order", "ASC")
					.queryParam(QUANDL_API_KEY_NAME, QUANDL_API_KEY_VALUE);

			List<GDPDEF> GDPDEFList = new ArrayList<>();
			FederalResponse json = restUtility.consumeResponse(builder.toUriString());
			json.getDataset_data().getData().stream().forEach(o -> {
				ArrayList temp = (ArrayList) o;
				try {
					Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
					GDPDEFList.add(new GDPDEF(date, Float.parseFloat(temp.get(1).toString())));
					;
				} catch (ParseException e) {
					e.printStackTrace();
				}
			});

			if (GDPDEFList.size() > 1) { // As last record is already present in DB
				GDPDEFList.remove(0);
				response = gdpdefRepostiory.saveAll(GDPDEFList);
				logger.info("New record inserted in GDPDEF");
			}

		}
		return response;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void updateRocChangeSignGDPDEF() {
		List<GDPDEFCalculation> gdpdefCalculationList = gdpdefCalculationRepository.findAllByRocIsNotNullAndRocChangeSignIsNull();
		GDPDEFCalculation lastUpdatedRecord = gdpdefCalculationRepository.findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

		Collections.sort(gdpdefCalculationList, new FederalPricesAndInflationService.SortByDateGDPDEFCalculation());

		if(gdpdefCalculationList.size() == 0){
			return;
		}

		Float lastRoc = lastUpdatedRecord.getRoc();
		for (GDPDEFCalculation gdpdefCalculation : gdpdefCalculationList) {
			if(gdpdefCalculation.getRoc() < lastRoc){
				gdpdefCalculation.setRocChangeSign(-1);
			}else if (gdpdefCalculation.getRoc() > lastRoc){
				gdpdefCalculation.setRocChangeSign(1);
			}else if(gdpdefCalculation.getRoc() == lastRoc){
				gdpdefCalculation.setRocChangeSign(0);
			}

			lastRoc = gdpdefCalculation.getRoc();
		}

		gdpdefCalculationRepository.saveAll(gdpdefCalculationList);
	}

}
