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
import com.automation.trading.domain.calculation.BASECalculation;
import com.automation.trading.domain.fred.BASE;
import com.automation.trading.repository.BASECalculationRepository;
import com.automation.trading.repository.BASERepository;
import com.automation.trading.service.BASEService;
import com.automation.trading.service.FederalMoneyService.SortByDateBASE;
import com.automation.trading.service.FederalMoneyService.SortByDateBASECalculation;
import com.automation.trading.utility.RestUtility;

@Service
public class BaseUpdateService {

	@Autowired
	private BASERepository baseRepository;

	@Autowired
	private BASECalculationRepository baseCalculationRepository;

	@Autowired
	private BASEService baseService;

	@Autowired
	private RestUtility restUtility;

	@Value("${quandl.host.url}")
	private String QUANDL_HOST_URL;

	@Value("${quandl.api.key.value}")
	private String QUANDL_API_KEY_VALUE;

	@Value("${quandl.api.key.name}")
	private String QUANDL_API_KEY_NAME;

	@Value("${quandl.data.format}")
	private String QUANDL_DATA_FORMAT;

	private Logger logger = LoggerFactory.getLogger(BaseUpdateService.class);

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRoc() {

		if (NumberUtils.INTEGER_ZERO.equals(baseCalculationRepository.findAny())) {
			baseService.calculateRoc();
			baseService.updateRocChangeSignDff();
		}

		Optional<List<BASE>> baseListOpt = baseRepository.findByRocFlagIsFalseOrderByDate();
		Optional<BASE> prevBASEOpt = baseRepository.findFirstByRocFlagIsTrueOrderByDateDesc();
		HashMap<Date, BASECalculation> baseCalculationHashMap = new HashMap<>();

		List<BASE> baseList = new ArrayList<>();

		if (baseListOpt.isPresent()) {
			baseList = baseListOpt.get();
			if (prevBASEOpt.isPresent()) {
				baseList.add(prevBASEOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(baseList, new SortByDateBASE());
		List<BASECalculation> baseCalculationReference = baseCalculationRepository.findAll();
		List<BASECalculation> baseCalculationModified = new ArrayList<>();
		Queue<BASE> baseQueue = new LinkedList<>();

		for (BASECalculation baseCalculation : baseCalculationReference) {
			baseCalculationHashMap.put(baseCalculation.getToDate(), baseCalculation);
		}

		for (BASE base : baseList) {
			BASECalculation tempBASECalculation = new BASECalculation();

			if (baseQueue.size() == 2) {
				baseQueue.poll();
			}
			baseQueue.add(base);

			if (base.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;

			Iterator<BASE> queueIterator = baseQueue.iterator();

			if (baseCalculationHashMap.containsKey(base.getDate())) {
				tempBASECalculation = baseCalculationHashMap.get(base.getDate());
			}

			while (queueIterator.hasNext()) {
				BASE temp = queueIterator.next();
				temp.setRocFlag(true);
				if (baseQueue.size() == 1) {
					roc = 0f;
					tempBASECalculation.setRoc(roc);
					tempBASECalculation.setToDate(base.getDate());
					tempBASECalculation.setRocChangeSign(0);
				} else {
					roc = (base.getValue() / ((LinkedList<BASE>) baseQueue).get(0).getValue()) - 1;
					tempBASECalculation.setRoc(roc);
					tempBASECalculation.setToDate(base.getDate());
				}

			}

			baseCalculationModified.add(tempBASECalculation);
		}

		baseList = baseRepository.saveAll(baseList);
		baseCalculationModified = baseCalculationRepository.saveAll(baseCalculationModified);
		logger.debug("Added new BASE row, " + baseCalculationModified);

		return;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRocRollingAnnualAvg() {

		if (NumberUtils.INTEGER_ZERO.equals(baseCalculationRepository.findAny())) {
			return;
		}

		Optional<List<BASECalculation>> baseCalculationListOpt = baseCalculationRepository
				.findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();
		Optional<List<BASECalculation>> prevBASECalculationListOpt = baseCalculationRepository
				.findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();
		List<BASECalculation> baseCalculationList = new ArrayList<>();

		if (baseCalculationListOpt.isPresent()) {
			baseCalculationList = baseCalculationListOpt.get();
			if (prevBASECalculationListOpt.isPresent()) {
				baseCalculationList.addAll(prevBASECalculationListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(baseCalculationList, new SortByDateBASECalculation());

		Queue<BASECalculation> baseCalculationPriorityQueue = new LinkedList<BASECalculation>();
		for (BASECalculation baseCalculation : baseCalculationList) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (baseCalculationPriorityQueue.size() == 4) {
				baseCalculationPriorityQueue.poll();
			}
			baseCalculationPriorityQueue.add(baseCalculation);

			if (baseCalculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<BASECalculation> queueIterator = baseCalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				BASECalculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			baseCalculation.setRocAnnRollAvgFlag(true);
			baseCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(baseCalculationList);
		baseCalculationList = baseCalculationRepository.saveAll(baseCalculationList);
		logger.info("New base calculation record inserted" + baseCalculationList);
		return;

	}

	/**
	 * Calculates Rolling Average of Three Month BASE
	 * 
	 * @return BASECalculation , updated BASECalculation Table
	 */
	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void calculateRollAvgThreeMonth() {

		if (NumberUtils.INTEGER_ZERO.equals(baseCalculationRepository.findAny())) {
			return;
		}

		List<BASECalculation> baseCalculationList = new ArrayList<>();
		Optional<List<BASE>> baseListOpt = baseRepository.findByRollAverageFlagIsFalseOrderByDate();
		Optional<List<BASE>> prevBASEListOpt = baseRepository.findTop2ByRollAverageFlagIsTrueOrderByDateDesc();
		List<BASECalculation> baseCalculationReference = baseCalculationRepository.findAll();
		HashMap<Date, BASECalculation> baseCalculationHashMap = new HashMap<>();
		List<BASE> baseList = new ArrayList<>();

		for (BASECalculation baseCalculation : baseCalculationReference) {
			baseCalculationHashMap.put(baseCalculation.getToDate(), baseCalculation);
		}

		Queue<BASE> baseQueue = new LinkedList<>();

		if (baseListOpt.isPresent()) {
			baseList = baseListOpt.get();
			if (prevBASEListOpt.isPresent()) {
				baseList.addAll(prevBASEListOpt.get());
			}
		} else {
			return;
		}

		Collections.sort(baseList, new SortByDateBASE());

		for (BASE base : baseList) {

			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			if (baseQueue.size() == 3) {
				baseQueue.poll();
			}
			baseQueue.add(base);
			if (base.getRollAverageFlag()) {
				continue;
			}

			Iterator<BASE> queueItr = baseQueue.iterator();

			BASECalculation tempBASECalculation = new BASECalculation();
			if (baseCalculationHashMap.containsKey(base.getDate())) {
				tempBASECalculation = baseCalculationHashMap.get(base.getDate());
			} else {
				tempBASECalculation.setToDate(base.getDate());
			}

			while (queueItr.hasNext()) {
				BASE baseVal = queueItr.next();
				rollingAvg += baseVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			base.setRollAverageFlag(true);
			tempBASECalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			baseCalculationList.add(tempBASECalculation);

		}

		baseCalculationReference = baseCalculationRepository.saveAll(baseCalculationList);
		baseList = baseRepository.saveAll(baseList);
		return;
	}

	@Scheduled(fixedDelay = 1000 * 60)
	public List<BASE> getLatestBASERecords() {

		if (NumberUtils.INTEGER_ZERO.equals(baseCalculationRepository.findAny())) {
			return null;
		}
		Optional<BASE> lastRecordOpt = baseRepository.findTopByOrderByDateDesc();
		List<BASE> response = new ArrayList<>();
		if (lastRecordOpt.isPresent()) {
			BASE lastRecord = lastRecordOpt.get();
			String lastDate = lastRecord.getDate().toString();
			String transactionUrl = QUANDL_HOST_URL + "FRED" + "/" + "BASE" + "/" + QUANDL_DATA_FORMAT;

			UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(transactionUrl)
					// Add query parameter
					.queryParam("start_date", lastDate).queryParam("order", "ASC")
					.queryParam(QUANDL_API_KEY_NAME, QUANDL_API_KEY_VALUE);

			List<BASE> BASEList = new ArrayList<>();
			FederalResponse json = restUtility.consumeResponse(builder.toUriString());
			json.getDataset_data().getData().stream().forEach(o -> {
				ArrayList temp = (ArrayList) o;
				try {
					Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
					BASEList.add(new BASE(date, Float.parseFloat(temp.get(1).toString())));
					;
				} catch (ParseException e) {
					e.printStackTrace();
				}
			});

			if (BASEList.size() > 1) { // As last record is already present in DB
				BASEList.remove(0);
				response = baseRepository.saveAll(BASEList);
				logger.info("New record inserted in BASE");
			}

		}
		return response;
	}

	@Async
	@Scheduled(fixedDelay = 1000 * 60)
	public void updateRocChangeSignBASE() {
		List<BASECalculation> baseCalculationList = baseCalculationRepository
				.findAllByRocIsNotNullAndRocChangeSignIsNull();
		BASECalculation lastUpdatedRecord = baseCalculationRepository
				.findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

		Collections.sort(baseCalculationList, new SortByDateBASECalculation());
		if (baseCalculationList.size() == 0) {
			return;
		}

		Float lastRoc = lastUpdatedRecord.getRoc();
		for (BASECalculation baseCalculation : baseCalculationList) {
			if (baseCalculation.getRoc() < lastRoc) {
				baseCalculation.setRocChangeSign(-1);
			} else if (baseCalculation.getRoc() > lastRoc) {
				baseCalculation.setRocChangeSign(1);
			} else if (baseCalculation.getRoc() == lastRoc) {
				baseCalculation.setRocChangeSign(0);
			}

			lastRoc = baseCalculation.getRoc();
		}

		baseCalculationRepository.saveAll(baseCalculationList);
	}

}
