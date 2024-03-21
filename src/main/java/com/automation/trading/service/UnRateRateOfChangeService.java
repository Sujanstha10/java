package com.automation.trading.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.automation.trading.domain.calculation.UnRateCalculation;
import com.automation.trading.domain.fred.UnRate;
import com.automation.trading.repository.UnRateCalculationRepository;
import com.automation.trading.repository.UnRateRepostiory;

@Service
public class UnRateRateOfChangeService {

	@Autowired
	UnRateCalculationRepository unRateCalculationRepository;
	@Autowired
	UnRateRepostiory unRateRepostiory;

	@Autowired
	RestTemplate restTemplate;

	@Value("${quandl.host.url}")
	private String QUANDL_HOST_URL;

	@Value("${quandl.api.key.value}")
	private String QUANDL_API_KEY_VALUE;

	@Value("${quandl.api.key.name}")
	private String QUANDL_API_KEY_NAME;

	@Value("${quandl.data.format}")
	private String QUANDL_DATA_FORMAT;

	private Logger logger = LoggerFactory.getLogger(DffRateOfChangeService.class);

	public List<UnRateCalculation> calculateRoc() {
		List<UnRate> unRateList = unRateRepostiory.findAll();
		List<UnRateCalculation> unRateCalculationReference = unRateCalculationRepository.findAll();
		List<UnRateCalculation> unRateCalculationModified = new ArrayList<>();
		Queue<UnRate> unRateQueue = new LinkedList<>();
		for (UnRate unRate : unRateList) {

			Float roc = 0.0f;
			int period = 0;
			UnRateCalculation tempUnRateCalculation = new UnRateCalculation();
			if (unRateQueue.size() == 2) {
				unRateQueue.poll();
			}
			unRateQueue.add(unRate);

			if (unRate.getRocFlag()) {
				continue;
			}
			Iterator<UnRate> queueIterator = unRateQueue.iterator();
			while (queueIterator.hasNext()) {
				UnRate temp = queueIterator.next();
				temp.setRocFlag(true);

				List<UnRateCalculation> currentUnRateCalculationRef = unRateCalculationReference.stream()
						.filter(p -> p.getToDate().equals(unRate.getDate())).collect(Collectors.toList());

				if (currentUnRateCalculationRef.size() > 0)
					tempUnRateCalculation = currentUnRateCalculationRef.get(0);

				if (unRateQueue.size() == 1) {
					roc = 0f;
					tempUnRateCalculation.setRoc(roc);
					tempUnRateCalculation.setToDate(unRate.getDate());
				} else {
					roc = (unRate.getValue() / ((LinkedList<UnRate>) unRateQueue).get(0).getValue()) - 1;
					tempUnRateCalculation.setRoc(roc);
					tempUnRateCalculation.setToDate(unRate.getDate());
				}
			}

			unRateCalculationModified.add(tempUnRateCalculation);
		}

		unRateList = unRateRepostiory.saveAll(unRateList);
		unRateCalculationModified = unRateCalculationRepository.saveAll(unRateCalculationModified);

		return unRateCalculationModified;
	}

	public List<UnRateCalculation> calculateRocRollingAnnualAvg() {

		List<UnRateCalculation> unRateCalculationList = new ArrayList<>();
		List<UnRateCalculation> unRateCalculationReference = unRateCalculationRepository.findAll();
		Queue<UnRateCalculation> unRateCalculationPriorityQueue = new LinkedList<UnRateCalculation>();
		for (UnRateCalculation unRateCalculation : unRateCalculationReference) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (unRateCalculationPriorityQueue.size() == 4) {
				unRateCalculationPriorityQueue.poll();
			}
			unRateCalculationPriorityQueue.add(unRateCalculation);

			if (unRateCalculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<UnRateCalculation> queueIterator = unRateCalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				UnRateCalculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			unRateCalculation.setRocAnnRollAvgFlag(true);
			unRateCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(unRateCalculationReference);
		unRateCalculationReference = unRateCalculationRepository.saveAll(unRateCalculationReference);
		return unRateCalculationReference;

	}

	/**
	 * Update Roc change sign , 1 for +ve , 0 for neutral and -1 for -ve for UNRATE
	 * table
	 */

	public List<UnRateCalculation> updateRocChangeSignUnRate() {
		List<UnRateCalculation> UnRateCalculationList = unRateCalculationRepository.findAllByRocIsNotNull();
		if(UnRateCalculationList.isEmpty()){
			return UnRateCalculationList;
		}
		List<UnRateCalculation> modifiedSignList = new ArrayList<UnRateCalculation>();
		UnRateCalculation UnRateCalculationPrev = new UnRateCalculation();

		for (UnRateCalculation UnRateCalculation : UnRateCalculationList) {
			UnRateCalculation modifiedSignUnRateCalc = UnRateCalculation;
			if (UnRateCalculationPrev.getToDate() == null) {
				modifiedSignUnRateCalc.setRocChangeSign(0);
			} else {
				if (UnRateCalculationPrev.getRoc() < modifiedSignUnRateCalc.getRoc()) {
					modifiedSignUnRateCalc.setRocChangeSign(1);
				} else if (UnRateCalculationPrev.getRoc() > modifiedSignUnRateCalc.getRoc()) {
					modifiedSignUnRateCalc.setRocChangeSign(-1);
				} else {
					modifiedSignUnRateCalc.setRocChangeSign(0);
				}
			}
			modifiedSignList.add(modifiedSignUnRateCalc);
			UnRateCalculationPrev = modifiedSignUnRateCalc;
		}
		UnRateCalculationList = unRateCalculationRepository.saveAll(modifiedSignList);
		return UnRateCalculationList;
	}

	/**
	 * Calculates Rolling Average of Three Month UnEmployment Rate
	 * 
	 * @return UnRatecalculation , updated unrate Calculation Table
	 */

	public List<UnRateCalculation> calculateRollAvgThreeMonth() {

		List<UnRateCalculation> unRateCalculationList = new ArrayList<>();
		List<UnRate> unRateList = unRateRepostiory.findAll();
		List<UnRateCalculation> unRateCalculationReference = unRateCalculationRepository.findAll();
		Queue<UnRate> unRateQueue = new LinkedList<UnRate>();

		for (UnRate unRate : unRateList) {

			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			if (unRateQueue.size() == 3) {
				unRateQueue.poll();
			}
			unRateQueue.add(unRate);

			if (unRate.getRollAverageFlag()) {
				continue;
			}
			Iterator<UnRate> queueItr = unRateQueue.iterator();

			UnRateCalculation tempUnRateCalculation = new UnRateCalculation();
			List<UnRateCalculation> currentUnRateCalculationRef = unRateCalculationReference.stream()
					.filter(p -> p.getToDate().equals(unRate.getDate())).collect(Collectors.toList());

			if (currentUnRateCalculationRef.size() > 0)
				tempUnRateCalculation = currentUnRateCalculationRef.get(0);

			while (queueItr.hasNext()) {
				UnRate unRateVal = queueItr.next();
				rollingAvg += unRateVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			unRate.setRollAverageFlag(true);
			tempUnRateCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			unRateCalculationList.add(tempUnRateCalculation);

		}

		unRateCalculationReference = unRateCalculationRepository.saveAll(unRateCalculationList);
		unRateList = unRateRepostiory.saveAll(unRateList);
		return unRateCalculationReference;
	}

}
