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

import com.automation.trading.domain.calculation.GDPC1Calculation;
import com.automation.trading.domain.fred.GDPC1;
import com.automation.trading.repository.GDPC1CalculationRepository;
import com.automation.trading.repository.GDPC1Repository;

@Service
public class GDPC1Service {

	@Autowired
	GDPC1Repository gdpc1Repository;

	@Autowired
	private GDPC1CalculationRepository gdpc1CalculationRepository;

	@Value("${quandl.host.url}")
	private String QUANDL_HOST_URL;

	@Value("${quandl.api.key.value}")
	private String QUANDL_API_KEY_VALUE;

	@Value("${quandl.api.key.name}")
	private String QUANDL_API_KEY_NAME;

	@Value("${quandl.data.format}")
	private String QUANDL_DATA_FORMAT;

	private Logger logger = LoggerFactory.getLogger(GDPC1Service.class);

	/**
	 * Update Roc change sign , 1 for +ve , 0 for neutral and -1 for -ve
	 */

	public List<GDPC1Calculation> updateRocChangeSignGDPC1() {
		List<GDPC1Calculation> gdpc1CalculationList = gdpc1CalculationRepository.findAllByRocIsNotNull();
		if (gdpc1CalculationList.isEmpty()) {
			return gdpc1CalculationList;
		}
		List<GDPC1Calculation> modifiedSignList = new ArrayList<GDPC1Calculation>();
		GDPC1Calculation gdpc1CalculationPrev = new GDPC1Calculation();

		for (GDPC1Calculation gdpc1Calculation : gdpc1CalculationList) {
			GDPC1Calculation modifiedSignGDPC1Calc = gdpc1Calculation;
			if (gdpc1CalculationPrev.getToDate() == null) {
				modifiedSignGDPC1Calc.setRocChangeSign(0);
			} else {
				if (gdpc1CalculationPrev.getRoc() < modifiedSignGDPC1Calc.getRoc()) {
					modifiedSignGDPC1Calc.setRocChangeSign(1);
				} else if (gdpc1CalculationPrev.getRoc() > modifiedSignGDPC1Calc.getRoc()) {
					modifiedSignGDPC1Calc.setRocChangeSign(-1);
				} else {
					modifiedSignGDPC1Calc.setRocChangeSign(0);
				}
			}
			modifiedSignList.add(modifiedSignGDPC1Calc);
			gdpc1CalculationPrev = modifiedSignGDPC1Calc;
		}
		gdpc1CalculationList = gdpc1CalculationRepository.saveAll(modifiedSignList);
		return gdpc1CalculationList;
	}

	/**
	 * Function to calculate roc roll average flag
	 */

	public List<GDPC1Calculation> calculateRocRollingAnnualAvgGDPC1() {

		List<GDPC1Calculation> gdpc1CalculationList = new ArrayList<>();
		List<GDPC1Calculation> gdpc1CalculationReference = gdpc1CalculationRepository.findAll();
		Queue<GDPC1Calculation> gdpc1CalculationPriorityQueue = new LinkedList<GDPC1Calculation>();
		for (GDPC1Calculation gdpc1Calculation : gdpc1CalculationReference) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (gdpc1CalculationPriorityQueue.size() == 4) {
				gdpc1CalculationPriorityQueue.poll();
			}
			gdpc1CalculationPriorityQueue.add(gdpc1Calculation);

			if (gdpc1Calculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<GDPC1Calculation> queueIterator = gdpc1CalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				GDPC1Calculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			gdpc1Calculation.setRocAnnRollAvgFlag(true);
			gdpc1Calculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(gdpc1CalculationReference);
		gdpc1CalculationReference = gdpc1CalculationRepository.saveAll(gdpc1CalculationReference);
		return gdpc1CalculationReference;

	}

	public List<GDPC1Calculation> calculateRoc() {
		List<GDPC1> gdpc1List = gdpc1Repository.findAll();
		List<GDPC1Calculation> gdpc1CalculationReference = gdpc1CalculationRepository.findAll();
		List<GDPC1Calculation> gdpc1CalculationModified = new ArrayList<>();
		Queue<GDPC1> gdpc1Queue = new LinkedList<>();
		for (GDPC1 gdpc1 : gdpc1List) {
			if (gdpc1Queue.size() == 2) {
				gdpc1Queue.poll();
			}
			gdpc1Queue.add(gdpc1);

			if (gdpc1.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;
			int period = 0;
			GDPC1Calculation tempGDPC1Calculation = new GDPC1Calculation();

			Iterator<GDPC1> queueIterator = gdpc1Queue.iterator();

			while (queueIterator.hasNext()) {
				GDPC1 temp = queueIterator.next();
				temp.setRocFlag(true);

				List<GDPC1Calculation> currentGDPC1CalculationRef = gdpc1CalculationReference.stream()
						.filter(p -> p.getToDate().equals(gdpc1.getDate())).collect(Collectors.toList());

				if (currentGDPC1CalculationRef.size() > 0)
					tempGDPC1Calculation = currentGDPC1CalculationRef.get(0);

				if (gdpc1Queue.size() == 1) {
					roc = 0f;
					tempGDPC1Calculation.setRoc(roc);
					tempGDPC1Calculation.setToDate(gdpc1.getDate());
				} else {
					roc = (gdpc1.getValue() / ((LinkedList<GDPC1>) gdpc1Queue).get(0).getValue()) - 1;
					tempGDPC1Calculation.setRoc(roc);
					tempGDPC1Calculation.setToDate(gdpc1.getDate());
				}
			}

			gdpc1CalculationModified.add(tempGDPC1Calculation);
		}

		gdpc1List = gdpc1Repository.saveAll(gdpc1List);
		gdpc1CalculationModified = gdpc1CalculationRepository.saveAll(gdpc1CalculationModified);

		return gdpc1CalculationModified;
	}

	/**
	 * Calculates Rolling Average of Three Month GDP
	 *
	 * @return GDPC1Calculation , updated GDPC1Calculation Table
	 */
	public List<GDPC1Calculation> calculateRollAvgThreeMonth() {

		List<GDPC1Calculation> gdpc1CalculationList = new ArrayList<>();
		List<GDPC1> gdpc1List = gdpc1Repository.findAll();
		List<GDPC1Calculation> gdpc1CalculationReference = gdpc1CalculationRepository.findAll();
		Queue<GDPC1> gdpc1Queue = new LinkedList<GDPC1>();

		for (GDPC1 gdpc1 : gdpc1List) {

			if (gdpc1Queue.size() == 3) {
				gdpc1Queue.poll();
			}
			gdpc1Queue.add(gdpc1);

			if (gdpc1.getRollAverageFlag()) {
				continue;
			}
			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			Iterator<GDPC1> queueItr = gdpc1Queue.iterator();

			GDPC1Calculation tempGDPC1Calculation = new GDPC1Calculation();
			List<GDPC1Calculation> currentGDPC1CalculationRef = gdpc1CalculationReference.stream()
					.filter(p -> p.getToDate().equals(gdpc1.getDate())).collect(Collectors.toList());

			if (currentGDPC1CalculationRef.size() > 0)
				tempGDPC1Calculation = currentGDPC1CalculationRef.get(0);

			while (queueItr.hasNext()) {
				GDPC1 gdpc1Val = queueItr.next();
				rollingAvg += gdpc1Val.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			gdpc1.setRollAverageFlag(true);
			tempGDPC1Calculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			gdpc1CalculationList.add(tempGDPC1Calculation);

		}

		gdpc1CalculationReference = gdpc1CalculationRepository.saveAll(gdpc1CalculationList);
		gdpc1List = gdpc1Repository.saveAll(gdpc1List);
		return gdpc1CalculationReference;
	}

}
