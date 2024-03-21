package com.automation.trading.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.automation.trading.domain.calculation.T10YIECalculation;
import com.automation.trading.domain.fred.interestrates.T10YIE;
import com.automation.trading.repository.T10YIECalculationRepository;
import com.automation.trading.repository.T10YIERepository;

@Service
public class T10YIEService {

	@Autowired
	private T10YIERepository t10yieRepository;

	@Autowired
	private T10YIECalculationRepository t10yieCalculationRepository;

	public void calculateRoc() {
		List<T10YIE> t10yieList = t10yieRepository.findAll();
		List<T10YIECalculation> t10yieCalculationList = t10yieCalculationRepository.findAll();
		List<T10YIECalculation> t10yieCalculationModified = new ArrayList<>();
		Queue<T10YIE> t10yieQueue = new LinkedList<>();
		for (T10YIE t10yie : t10yieList) {
			if (t10yie.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;
			int period = 0;
			T10YIECalculation baseCalculation = new T10YIECalculation();
			if (t10yieQueue.size() == 2) {
				t10yieQueue.poll();
			}
			t10yieQueue.add(t10yie);
			Iterator<T10YIE> queueIterator = t10yieQueue.iterator();
			while (queueIterator.hasNext()) {
				T10YIE temp = queueIterator.next();
				temp.setRocFlag(true);
			}

			List<T10YIECalculation> currentT10YIECalculationRef = t10yieCalculationList.stream()
					.filter(p -> p.getToDate().equals(t10yie.getDate())).collect(Collectors.toList());

			if (currentT10YIECalculationRef.size() > 0)
				baseCalculation = currentT10YIECalculationRef.get(0);

			if (t10yieQueue.size() == 1) {
				roc = 0f;
				baseCalculation.setRoc(roc);
				baseCalculation.setToDate(t10yie.getDate());
			} else {
				roc = (t10yie.getValue() / ((LinkedList<T10YIE>) t10yieQueue).get(0).getValue()) - 1;
				baseCalculation.setRoc(roc);
				baseCalculation.setToDate(t10yie.getDate());
			}
			t10yieCalculationModified.add(baseCalculation);
		}

		t10yieRepository.saveAll(t10yieList);
		t10yieCalculationRepository.saveAll(t10yieCalculationModified);

	}

	public List<T10YIECalculation> calculateRollAvgThreeMonth() {
		List<T10YIECalculation> t10yieCalculationList = new ArrayList<>();
		List<T10YIE> t10yieList = t10yieRepository.findAll();
		List<T10YIECalculation> t10yieCalculationReference = t10yieCalculationRepository.findAll();
		Queue<T10YIE> t10yieQueue = new LinkedList<>();

		for (T10YIE t10yie : t10yieList) {

			if (t10yieQueue.size() == 3) {
				t10yieQueue.poll();
			}
			t10yieQueue.add(t10yie);

			if (t10yie.getRollAverageFlag()) {
				continue;
			}
			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			Iterator<T10YIE> queueItr = t10yieQueue.iterator();

			T10YIECalculation tempGdpCalculation = new T10YIECalculation();
			List<T10YIECalculation> currentT10YIECalculationRef = t10yieCalculationReference.stream()
					.filter(p -> p.getToDate().equals(t10yie.getDate())).collect(Collectors.toList());

			if (currentT10YIECalculationRef.size() > 0)
				tempGdpCalculation = currentT10YIECalculationRef.get(0);

			while (queueItr.hasNext()) {
				T10YIE gdpVal = queueItr.next();
				rollingAvg += gdpVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			t10yie.setRollAverageFlag(true);
			tempGdpCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			t10yieCalculationList.add(tempGdpCalculation);

		}

		t10yieCalculationReference = t10yieCalculationRepository.saveAll(t10yieCalculationList);
		t10yieList = t10yieRepository.saveAll(t10yieList);
		return t10yieCalculationReference;
	}

	public List<T10YIECalculation> calculateRocRollingAnnualAvg() {

		List<T10YIECalculation> t10yieCalculationReference = t10yieCalculationRepository.findAll();
		Queue<T10YIECalculation> t10yieCalculationPriorityQueue = new LinkedList<>();
		for (T10YIECalculation t10yieCalculation : t10yieCalculationReference) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (t10yieCalculationPriorityQueue.size() == 4) {
				t10yieCalculationPriorityQueue.poll();
			}
			t10yieCalculationPriorityQueue.add(t10yieCalculation);

			if (t10yieCalculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<T10YIECalculation> queueIterator = t10yieCalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				T10YIECalculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			t10yieCalculation.setRocAnnRollAvgFlag(true);
			t10yieCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(t10yieCalculationReference);
		t10yieCalculationReference = t10yieCalculationRepository.saveAll(t10yieCalculationReference);
		return t10yieCalculationReference;
	}

	public List<T10YIECalculation> updateRocChangeSignT10YIE() {

		List<T10YIECalculation> t10yieCalculationList = t10yieCalculationRepository.findAllByRocIsNotNull();
		if (t10yieCalculationList.isEmpty()) {
			return t10yieCalculationList;
		}

		List<T10YIECalculation> modifiedSignList = new ArrayList<>();
		T10YIECalculation t10yieCalculationPrev = new T10YIECalculation();

		for (T10YIECalculation t10yieCalculation : t10yieCalculationList) {
			T10YIECalculation modifiedSigndffCalc = t10yieCalculation;
			if (t10yieCalculationPrev.getToDate() == null) {
				modifiedSigndffCalc.setRocChangeSign(0);
			} else {
				if (t10yieCalculationPrev.getRoc() < modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(1);
				} else if (t10yieCalculationPrev.getRoc() > modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(-1);
				} else {
					modifiedSigndffCalc.setRocChangeSign(0);
				}
			}
			modifiedSignList.add(modifiedSigndffCalc);
			t10yieCalculationPrev = modifiedSigndffCalc;
		}
		t10yieCalculationList = t10yieCalculationRepository.saveAll(modifiedSignList);
		return t10yieCalculationList;
	}

}
