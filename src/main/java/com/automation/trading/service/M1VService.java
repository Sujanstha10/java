package com.automation.trading.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.automation.trading.domain.calculation.M1VCalculation;
import com.automation.trading.domain.fred.M1V;
import com.automation.trading.repository.M1VCalculationRepository;
import com.automation.trading.repository.M1VRepository;

@Service
public class M1VService {

	@Autowired
	private M1VRepository m1vRepository;

	@Autowired
	private M1VCalculationRepository m1vCalculationRepository;

	public void calculateRoc() {
		List<M1V> m1vList = m1vRepository.findAll();
		List<M1VCalculation> m1vCalculationList = m1vCalculationRepository.findAll();
		List<M1VCalculation> m1vCalculationModified = new ArrayList<>();
		Queue<M1V> m1vQueue = new LinkedList<>();
		for (M1V m1v : m1vList) {
			if (m1v.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;
			int period = 0;
			M1VCalculation baseCalculation = new M1VCalculation();
			if (m1vQueue.size() == 2) {
				m1vQueue.poll();
			}
			m1vQueue.add(m1v);
			Iterator<M1V> queueIterator = m1vQueue.iterator();
			while (queueIterator.hasNext()) {
				M1V temp = queueIterator.next();
				temp.setRocFlag(true);
			}

			List<M1VCalculation> currentM1VCalculationRef = m1vCalculationList.stream()
					.filter(p -> p.getToDate().equals(m1v.getDate())).collect(Collectors.toList());

			if (currentM1VCalculationRef.size() > 0)
				baseCalculation = currentM1VCalculationRef.get(0);

			if (m1vQueue.size() == 1) {
				roc = 0f;
				baseCalculation.setRoc(roc);
				baseCalculation.setToDate(m1v.getDate());
			} else {
				roc = (m1v.getValue() / ((LinkedList<M1V>) m1vQueue).get(0).getValue()) - 1;
				baseCalculation.setRoc(roc);
				baseCalculation.setToDate(m1v.getDate());
			}
			m1vCalculationModified.add(baseCalculation);
		}

		m1vRepository.saveAll(m1vList);
		m1vCalculationRepository.saveAll(m1vCalculationModified);

	}

	public List<M1VCalculation> calculateRollAvgThreeMonth() {
		List<M1VCalculation> m1vCalculationList = new ArrayList<>();
		List<M1V> m1vList = m1vRepository.findAll();
		List<M1VCalculation> m1vCalculationReference = m1vCalculationRepository.findAll();
		Queue<M1V> m1vQueue = new LinkedList<>();

		for (M1V m1v : m1vList) {

			if (m1vQueue.size() == 3) {
				m1vQueue.poll();
			}
			m1vQueue.add(m1v);

			if (m1v.getRollAverageFlag()) {
				continue;
			}
			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			Iterator<M1V> queueItr = m1vQueue.iterator();

			M1VCalculation tempGdpCalculation = new M1VCalculation();
			List<M1VCalculation> currentM1VCalculationRef = m1vCalculationReference.stream()
					.filter(p -> p.getToDate().equals(m1v.getDate())).collect(Collectors.toList());

			if (currentM1VCalculationRef.size() > 0)
				tempGdpCalculation = currentM1VCalculationRef.get(0);

			while (queueItr.hasNext()) {
				M1V gdpVal = queueItr.next();
				rollingAvg += gdpVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			m1v.setRollAverageFlag(true);
			tempGdpCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			m1vCalculationList.add(tempGdpCalculation);

		}

		m1vCalculationReference = m1vCalculationRepository.saveAll(m1vCalculationList);
		m1vList = m1vRepository.saveAll(m1vList);
		return m1vCalculationReference;
	}

	public List<M1VCalculation> calculateRocRollingAnnualAvg() {

		List<M1VCalculation> m1vCalculationReference = m1vCalculationRepository.findAll();
		Queue<M1VCalculation> m1vCalculationPriorityQueue = new LinkedList<>();
		for (M1VCalculation m1vCalculation : m1vCalculationReference) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (m1vCalculationPriorityQueue.size() == 4) {
				m1vCalculationPriorityQueue.poll();
			}
			m1vCalculationPriorityQueue.add(m1vCalculation);

			if (m1vCalculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<M1VCalculation> queueIterator = m1vCalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				M1VCalculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			m1vCalculation.setRocAnnRollAvgFlag(true);
			m1vCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(m1vCalculationReference);
		m1vCalculationReference = m1vCalculationRepository.saveAll(m1vCalculationReference);
		return m1vCalculationReference;
	}

	public List<M1VCalculation> updateRocChangeSign() {
		List<M1VCalculation> m1vCalculationList = m1vCalculationRepository.findAllByRocIsNotNull();
		if(m1vCalculationList.isEmpty()){
			return m1vCalculationList;
		}
		List<M1VCalculation> modifiedSignList = new ArrayList<>();
		M1VCalculation m1vCalculationPrev = new M1VCalculation();

		for (M1VCalculation m1vCalculation : m1vCalculationList) {
			M1VCalculation modifiedSigndffCalc = m1vCalculation;
			if (m1vCalculationPrev.getToDate() == null) {
				modifiedSigndffCalc.setRocChangeSign(0);
			} else {
				if (m1vCalculationPrev.getRoc() < modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(1);
				} else if (m1vCalculationPrev.getRoc() > modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(-1);
				} else {
					modifiedSigndffCalc.setRocChangeSign(0);
				}
			}
			modifiedSignList.add(modifiedSigndffCalc);
			m1vCalculationPrev = modifiedSigndffCalc;
		}
		m1vCalculationList = m1vCalculationRepository.saveAll(modifiedSignList);
		return m1vCalculationList;
	}
}
