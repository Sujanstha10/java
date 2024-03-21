package com.automation.trading.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.automation.trading.domain.calculation.T5YIECalculation;
import com.automation.trading.domain.fred.interestrates.T5YIE;
import com.automation.trading.repository.T5YIECalculationRepository;
import com.automation.trading.repository.T5YIERepository;

@Service
public class T5YIEService {

	@Autowired
	private T5YIERepository t5yieRepository;

	@Autowired
	private T5YIECalculationRepository t5yieCalculationRepository;

	public void calculateRoc() {
		List<T5YIE> t5yieList = t5yieRepository.findAll();
		List<T5YIECalculation> t5yieCalculationList = t5yieCalculationRepository.findAll();
		List<T5YIECalculation> t5yieCalculationModified = new ArrayList<>();
		Queue<T5YIE> t5yieQueue = new LinkedList<>();
		for (T5YIE t5yie : t5yieList) {
			if (t5yie.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;
			int period = 0;
			T5YIECalculation baseCalculation = new T5YIECalculation();
			if (t5yieQueue.size() == 2) {
				t5yieQueue.poll();
			}
			t5yieQueue.add(t5yie);
			Iterator<T5YIE> queueIterator = t5yieQueue.iterator();
			while (queueIterator.hasNext()) {
				T5YIE temp = queueIterator.next();
				temp.setRocFlag(true);
			}

			List<T5YIECalculation> currentT5YIECalculationRef = t5yieCalculationList.stream()
					.filter(p -> p.getToDate().equals(t5yie.getDate())).collect(Collectors.toList());

			if (currentT5YIECalculationRef.size() > 0)
				baseCalculation = currentT5YIECalculationRef.get(0);

			if (t5yieQueue.size() == 1) {
				roc = 0f;
				baseCalculation.setRoc(roc);
				baseCalculation.setToDate(t5yie.getDate());
			} else {
				roc = (t5yie.getValue() / ((LinkedList<T5YIE>) t5yieQueue).get(0).getValue()) - 1;
				baseCalculation.setRoc(roc);
				baseCalculation.setToDate(t5yie.getDate());
			}
			t5yieCalculationModified.add(baseCalculation);
		}

		t5yieRepository.saveAll(t5yieList);
		t5yieCalculationRepository.saveAll(t5yieCalculationModified);

	}

	public List<T5YIECalculation> calculateRollAvgThreeMonth() {
		List<T5YIECalculation> t5yieCalculationList = new ArrayList<>();
		List<T5YIE> t5yieList = t5yieRepository.findAll();
		List<T5YIECalculation> t5yieCalculationReference = t5yieCalculationRepository.findAll();
		Queue<T5YIE> t5yieQueue = new LinkedList<>();

		for (T5YIE t5yie : t5yieList) {

			if (t5yieQueue.size() == 3) {
				t5yieQueue.poll();
			}
			t5yieQueue.add(t5yie);

			if (t5yie.getRollAverageFlag()) {
				continue;
			}
			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			Iterator<T5YIE> queueItr = t5yieQueue.iterator();

			T5YIECalculation tempGdpCalculation = new T5YIECalculation();
			List<T5YIECalculation> currentT5YIECalculationRef = t5yieCalculationReference.stream()
					.filter(p -> p.getToDate().equals(t5yie.getDate())).collect(Collectors.toList());

			if (currentT5YIECalculationRef.size() > 0)
				tempGdpCalculation = currentT5YIECalculationRef.get(0);

			while (queueItr.hasNext()) {
				T5YIE gdpVal = queueItr.next();
				rollingAvg += gdpVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			t5yie.setRollAverageFlag(true);
			tempGdpCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			t5yieCalculationList.add(tempGdpCalculation);

		}

		t5yieCalculationReference = t5yieCalculationRepository.saveAll(t5yieCalculationList);
		t5yieList = t5yieRepository.saveAll(t5yieList);
		return t5yieCalculationReference;
	}

	public List<T5YIECalculation> calculateRocRollingAnnualAvg() {

		List<T5YIECalculation> t5yieCalculationReference = t5yieCalculationRepository.findAll();
		Queue<T5YIECalculation> t5yieCalculationPriorityQueue = new LinkedList<>();
		for (T5YIECalculation t5yieCalculation : t5yieCalculationReference) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (t5yieCalculationPriorityQueue.size() == 4) {
				t5yieCalculationPriorityQueue.poll();
			}
			t5yieCalculationPriorityQueue.add(t5yieCalculation);

			if (t5yieCalculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<T5YIECalculation> queueIterator = t5yieCalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				T5YIECalculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			t5yieCalculation.setRocAnnRollAvgFlag(true);
			t5yieCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(t5yieCalculationReference);
		t5yieCalculationReference = t5yieCalculationRepository.saveAll(t5yieCalculationReference);
		return t5yieCalculationReference;
	}

	public List<T5YIECalculation> updateRocChangeSignT5YIE() {
		List<T5YIECalculation> t5yieCalculationList = t5yieCalculationRepository.findAll();
		List<T5YIECalculation> modifiedSignList = new ArrayList<>();
		T5YIECalculation t5yieCalculationPrev = new T5YIECalculation();

		for (T5YIECalculation t5yieCalculation : t5yieCalculationList) {
			T5YIECalculation modifiedSigndffCalc = t5yieCalculation;
			if (t5yieCalculationPrev.getToDate() == null) {
				modifiedSigndffCalc.setRocChangeSign(0);
			} else {
				if (t5yieCalculationPrev.getRoc() < modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(1);
				} else if (t5yieCalculationPrev.getRoc() > modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(-1);
				} else {
					modifiedSigndffCalc.setRocChangeSign(0);
				}
			}
			modifiedSignList.add(modifiedSigndffCalc);
			t5yieCalculationPrev = modifiedSigndffCalc;
		}
		t5yieCalculationList = t5yieCalculationRepository.saveAll(modifiedSignList);
		return t5yieCalculationList;
	}

}
