package com.automation.trading.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.automation.trading.domain.calculation.M2VCalculation;
import com.automation.trading.domain.fred.M2V;
import com.automation.trading.repository.M2VCalculationRepository;
import com.automation.trading.repository.M2VRepository;

@Service
public class M2VService {

	
	@Autowired
	private M2VRepository m2vRepository;

	@Autowired
	private M2VCalculationRepository m2vCalculationRepository;

	public void calculateRoc() {
		List<M2V> m2vList = m2vRepository.findAll();
		List<M2VCalculation> m2vCalculationList = m2vCalculationRepository.findAll();
		List<M2VCalculation> m2vCalculationModified = new ArrayList<>();
		Queue<M2V> m2vQueue = new LinkedList<>();
		for (M2V m2v : m2vList) {
			if (m2v.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;
			int period = 0;
			M2VCalculation baseCalculation = new M2VCalculation();
			if (m2vQueue.size() == 2) {
				m2vQueue.poll();
			}
			m2vQueue.add(m2v);
			Iterator<M2V> queueIterator = m2vQueue.iterator();
			while (queueIterator.hasNext()) {
				M2V temp = queueIterator.next();
				temp.setRocFlag(true);
			}

			List<M2VCalculation> currentM2VCalculationRef = m2vCalculationList.stream()
					.filter(p -> p.getToDate().equals(m2v.getDate())).collect(Collectors.toList());

			if (currentM2VCalculationRef.size() > 0)
				baseCalculation = currentM2VCalculationRef.get(0);

			if (m2vQueue.size() == 1) {
				roc = 0f;
				baseCalculation.setRoc(roc);
				baseCalculation.setToDate(m2v.getDate());
			} else {
				roc = (m2v.getValue() / ((LinkedList<M2V>) m2vQueue).get(0).getValue()) - 1;
				baseCalculation.setRoc(roc);
				baseCalculation.setToDate(m2v.getDate());
			}
			m2vCalculationModified.add(baseCalculation);
		}

		m2vRepository.saveAll(m2vList);
		m2vCalculationRepository.saveAll(m2vCalculationModified);

	}

	public List<M2VCalculation> calculateRollAvgThreeMonth() {
		List<M2VCalculation> m2vCalculationList = new ArrayList<>();
		List<M2V> m2vList = m2vRepository.findAll();
		List<M2VCalculation> m2vCalculationReference = m2vCalculationRepository.findAll();
		Queue<M2V> m2vQueue = new LinkedList<>();

		for (M2V m2v : m2vList) {

			if (m2vQueue.size() == 3) {
				m2vQueue.poll();
			}
			m2vQueue.add(m2v);

			if (m2v.getRollAverageFlag()) {
				continue;
			}
			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			Iterator<M2V> queueItr = m2vQueue.iterator();

			M2VCalculation tempGdpCalculation = new M2VCalculation();
			List<M2VCalculation> currentM2VCalculationRef = m2vCalculationReference.stream()
					.filter(p -> p.getToDate().equals(m2v.getDate())).collect(Collectors.toList());

			if (currentM2VCalculationRef.size() > 0)
				tempGdpCalculation = currentM2VCalculationRef.get(0);

			while (queueItr.hasNext()) {
				M2V gdpVal = queueItr.next();
				rollingAvg += gdpVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			m2v.setRollAverageFlag(true);
			tempGdpCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			m2vCalculationList.add(tempGdpCalculation);

		}

		m2vCalculationReference = m2vCalculationRepository.saveAll(m2vCalculationList);
		m2vList = m2vRepository.saveAll(m2vList);
		return m2vCalculationReference;
	}

	public List<M2VCalculation> calculateRocRollingAnnualAvg() {

		List<M2VCalculation> m2vCalculationReference = m2vCalculationRepository.findAll();
		Queue<M2VCalculation> m2vCalculationPriorityQueue = new LinkedList<>();
		for (M2VCalculation m2vCalculation : m2vCalculationReference) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (m2vCalculationPriorityQueue.size() == 4) {
				m2vCalculationPriorityQueue.poll();
			}
			m2vCalculationPriorityQueue.add(m2vCalculation);

			if (m2vCalculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<M2VCalculation> queueIterator = m2vCalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				M2VCalculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			m2vCalculation.setRocAnnRollAvgFlag(true);
			m2vCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(m2vCalculationReference);
		m2vCalculationReference = m2vCalculationRepository.saveAll(m2vCalculationReference);
		return m2vCalculationReference;
	}

	public List<M2VCalculation> updateRocChangeSign() {
		List<M2VCalculation> m2vCalculationList = m2vCalculationRepository.findAllByRocIsNotNull();
		if(m2vCalculationList.isEmpty()){
			return m2vCalculationList;
		}
		List<M2VCalculation> modifiedSignList = new ArrayList<>();
		M2VCalculation m2vCalculationPrev = new M2VCalculation();

		for (M2VCalculation m2vCalculation : m2vCalculationList) {
			M2VCalculation modifiedSigndffCalc = m2vCalculation;
			if (m2vCalculationPrev.getToDate() == null) {
				modifiedSigndffCalc.setRocChangeSign(0);
			} else {
				if (m2vCalculationPrev.getRoc() < modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(1);
				} else if (m2vCalculationPrev.getRoc() > modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(-1);
				} else {
					modifiedSigndffCalc.setRocChangeSign(0);
				}
			}
			modifiedSignList.add(modifiedSigndffCalc);
			m2vCalculationPrev = modifiedSigndffCalc;
		}
		m2vCalculationList = m2vCalculationRepository.saveAll(modifiedSignList);
		return m2vCalculationList;
	}
}
