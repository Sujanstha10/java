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
import com.automation.trading.domain.calculation.ICSACalculation;
import com.automation.trading.domain.fred.ICSA;
import com.automation.trading.repository.ICSACalculationRepository;
import com.automation.trading.repository.ICSARepository;
import com.automation.trading.service.FederalEmploymentService.SortByDateICSA;
import com.automation.trading.service.FederalEmploymentService.SortByDateICSACalculation;
import com.automation.trading.service.ICSAService;
import com.automation.trading.utility.RestUtility;

@Service
public class ICSAUpdateService {

	@Autowired
	private ICSARepository icsaRepostiory;

	@Autowired
	private ICSACalculationRepository icsaCalculationRepository;

	@Autowired
	private ICSAService icsaRateOfChangeService;

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

	private Logger logger = LoggerFactory.getLogger(ICSAUpdateService.class);

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRoc() {

		if (NumberUtils.INTEGER_ZERO.equals(icsaCalculationRepository.findAny())) {
			icsaRateOfChangeService.calculateRoc();
			icsaRateOfChangeService.updateRocChangeSignICSA();
		}

		System.out.println("calculateRocRollingAnnualAvg");

		Optional<List<ICSA>> icsaListOpt = icsaRepostiory.findByRocFlagIsFalseOrderByDate();
		Optional<ICSA> prevICSAOpt = icsaRepostiory.findFirstByRocFlagIsTrueOrderByDateDesc();
		HashMap<Date, ICSACalculation> icsaCalculationHashMap = new HashMap<>();
		ICSACalculation prevICSACalculationRow = new ICSACalculation();

		List<ICSA> icsaList = new ArrayList<>();

		if (icsaListOpt.isPresent()) {
			icsaList = icsaListOpt.get();
			if (prevICSAOpt.isPresent()) {
				icsaList.add(prevICSAOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(icsaList, new SortByDateICSA());
		List<ICSACalculation> icsaCalculationReference = icsaCalculationRepository.findAll();
		List<ICSACalculation> icsaCalculationModified = new ArrayList<>();
		Queue<ICSA> icsaQueue = new LinkedList<>();

		for (ICSACalculation icsaCalculation : icsaCalculationReference) {
			icsaCalculationHashMap.put(icsaCalculation.getToDate(), icsaCalculation);
		}

		for (ICSA icsa : icsaList) {
			ICSACalculation tempICSACalculation = new ICSACalculation();

			if (icsaQueue.size() == 2) {
				icsaQueue.poll();
			}
			icsaQueue.add(icsa);

			if (icsa.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;

			Iterator<ICSA> queueIterator = icsaQueue.iterator();

			if (icsaCalculationHashMap.containsKey(icsa.getDate())) {
				tempICSACalculation = icsaCalculationHashMap.get(icsa.getDate());
			} else {
				tempICSACalculation.setToDate(icsa.getDate());
			}

			while (queueIterator.hasNext()) {
				ICSA temp = queueIterator.next();
				temp.setRocFlag(true);
				if (icsaQueue.size() == 1) {
					roc = 0f;
					tempICSACalculation.setRoc(roc);
					tempICSACalculation.setToDate(icsa.getDate());
					tempICSACalculation.setRocChangeSign(0);
				} else {
					roc = (icsa.getValue() / ((LinkedList<ICSA>) icsaQueue).get(0).getValue()) - 1;
					tempICSACalculation.setRoc(roc);
					tempICSACalculation.setToDate(icsa.getDate());
				}

			}

			icsaCalculationModified.add(tempICSACalculation);
		}

		icsaList = icsaRepostiory.saveAll(icsaList);
		icsaCalculationModified = icsaCalculationRepository.saveAll(icsaCalculationModified);
		logger.debug("Added new ICSA row, " + icsaCalculationModified);

		return;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRocRollingAnnualAvg() {

		if (NumberUtils.INTEGER_ZERO.equals(icsaCalculationRepository.findAny())) {
			return;
		}

		System.out.println("calculateRocRollingAnnualAvg");
		Optional<List<ICSACalculation>> icsaCalculationListOpt = icsaCalculationRepository
				.findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();
		Optional<List<ICSACalculation>> prevICSACalculationListOpt = icsaCalculationRepository
				.findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();
		List<ICSACalculation> icsaCalculationList = new ArrayList<>();

		if (icsaCalculationListOpt.isPresent()) {
			icsaCalculationList = icsaCalculationListOpt.get();
			if (prevICSACalculationListOpt.isPresent()) {
				icsaCalculationList.addAll(prevICSACalculationListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(icsaCalculationList, new SortByDateICSACalculation());

		Queue<ICSACalculation> icsaCalculationPriorityQueue = new LinkedList<ICSACalculation>();
		for (ICSACalculation icsaCalculation : icsaCalculationList) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (icsaCalculationPriorityQueue.size() == 4) {
				icsaCalculationPriorityQueue.poll();
			}
			icsaCalculationPriorityQueue.add(icsaCalculation);

			if (icsaCalculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<ICSACalculation> queueIterator = icsaCalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				ICSACalculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			icsaCalculation.setRocAnnRollAvgFlag(true);
			icsaCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(icsaCalculationList);
		icsaCalculationList = icsaCalculationRepository.saveAll(icsaCalculationList);
		logger.info("New icsa calculation record inserted" + icsaCalculationList);
		return;

	}

	/**
	 * Calculates Rolling Average of Three Month ICSA
	 *
	 * @return ICSACalculation , updated ICSACalculation Table
	 */
	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRollAvgThreeMonth() {

		if (NumberUtils.INTEGER_ZERO.equals(icsaCalculationRepository.findAny())) {
			return;
		}

		System.out.println("calculateRollAvgThreeMonth");

		List<ICSACalculation> icsaCalculationList = new ArrayList<>();
		Optional<List<ICSA>> icsaListOpt = icsaRepostiory.findByRollAverageFlagIsFalseOrderByDate();
		Optional<List<ICSA>> prevICSAListOpt = icsaRepostiory.findTop2ByRollAverageFlagIsTrueOrderByDateDesc();
		List<ICSACalculation> icsaCalculationReference = icsaCalculationRepository.findAll();
		HashMap<Date, ICSACalculation> icsaCalculationHashMap = new HashMap<>();
		List<ICSA> icsaList = new ArrayList<>();

		for (ICSACalculation icsaCalculation : icsaCalculationReference) {
			icsaCalculationHashMap.put(icsaCalculation.getToDate(), icsaCalculation);
		}

		Queue<ICSA> icsaQueue = new LinkedList<>();

		if (icsaListOpt.isPresent()) {
			icsaList = icsaListOpt.get();
			if (prevICSAListOpt.isPresent()) {
				icsaList.addAll(prevICSAListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(icsaList, new SortByDateICSA());

		for (ICSA icsa : icsaList) {

			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			if (icsaQueue.size() == 3) {
				icsaQueue.poll();
			}
			icsaQueue.add(icsa);
			if (icsa.getRollAverageFlag()) {
				continue;
			}

			Iterator<ICSA> queueItr = icsaQueue.iterator();

			ICSACalculation tempICSACalculation = new ICSACalculation();
			if (icsaCalculationHashMap.containsKey(icsa.getDate())) {
				tempICSACalculation = icsaCalculationHashMap.get(icsa.getDate());
			} else {
				tempICSACalculation.setToDate(icsa.getDate());
			}

			while (queueItr.hasNext()) {
				ICSA icsaVal = queueItr.next();
				rollingAvg += icsaVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			icsa.setRollAverageFlag(true);
			tempICSACalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			icsaCalculationList.add(tempICSACalculation);

		}

		icsaCalculationReference = icsaCalculationRepository.saveAll(icsaCalculationList);
		icsaList = icsaRepostiory.saveAll(icsaList);
		return;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public List<ICSA> getLatestICSARecords() {

		if (NumberUtils.INTEGER_ZERO.equals(icsaCalculationRepository.findAny())) {
			return null;
		}
		System.out.println("getLatestICSARecords");
		Optional<ICSA> lastRecordOpt = icsaRepostiory.findTopByOrderByDateDesc();
		List<ICSA> response = new ArrayList<>();
		if (lastRecordOpt.isPresent()) {
			ICSA lastRecord = lastRecordOpt.get();
			String lastDate = lastRecord.getDate().toString();
			String transactionUrl = QUANDL_HOST_URL + "FRED" + "/" + "ICSA" + "/" + QUANDL_DATA_FORMAT;

			UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(transactionUrl)
					// Add query parameter
					.queryParam("start_date", lastDate).queryParam("order", "ASC")
					.queryParam(QUANDL_API_KEY_NAME, QUANDL_API_KEY_VALUE);

			List<ICSA> ICSAList = new ArrayList<>();
			FederalResponse json = restUtility.consumeResponse(builder.toUriString());
			json.getDataset_data().getData().stream().forEach(o -> {
				ArrayList temp = (ArrayList) o;
				try {
					Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
					ICSAList.add(new ICSA(date, Float.parseFloat(temp.get(1).toString())));
					;
				} catch (ParseException e) {
					e.printStackTrace();
				}
			});

			if (ICSAList.size() > 1) { // As last record is already present in DB
				ICSAList.remove(0);
				response = icsaRepostiory.saveAll(ICSAList);
				logger.info("New record inserted in ICSA");
			}

		}
		return response;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void updateRocChangeSignICSA() {
		List<ICSACalculation> icsaCalculationList = icsaCalculationRepository
				.findAllByRocIsNotNullAndRocChangeSignIsNull();
		ICSACalculation lastUpdatedRecord = icsaCalculationRepository
				.findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

		Collections.sort(icsaCalculationList, new SortByDateICSACalculation());

		if (icsaCalculationList.size() == 0) {
			return;
		}

		Float lastRoc = lastUpdatedRecord.getRoc();
		for (ICSACalculation icsaCalculation : icsaCalculationList) {
			if (icsaCalculation.getRoc() < lastRoc) {
				icsaCalculation.setRocChangeSign(-1);
			} else if (icsaCalculation.getRoc() > lastRoc) {
				icsaCalculation.setRocChangeSign(1);
			} else if (icsaCalculation.getRoc() == lastRoc) {
				icsaCalculation.setRocChangeSign(0);
			}

			lastRoc = icsaCalculation.getRoc();
		}

		icsaCalculationRepository.saveAll(icsaCalculationList);
	}
}
