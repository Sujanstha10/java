package com.automation.trading.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.automation.trading.domain.calculation.M2Calculation;
import com.automation.trading.domain.calculation.M2Calculation;
import com.automation.trading.domain.fred.BASE;
import com.automation.trading.domain.fred.M2;
import com.automation.trading.domain.fred.M2;
import com.automation.trading.repository.M2CalculationRepository;
import com.automation.trading.repository.M2Repository;

@Service
public class M2Service {

	@Autowired
	private M2Repository m2Repository;

	@Autowired
	private M2CalculationRepository m2CalculationRepository;

	public void calculateRoc() {

		List<M2> m2List = m2Repository.findAll();
		List<M2Calculation> m2CalculationList = m2CalculationRepository.findAll();
		List<M2Calculation> m2CalculationModified = new ArrayList<>();
		Queue<M2> m2Queue = new LinkedList<>();
		for (M2 m2 : m2List) {
			if (m2.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;
			int period = 0;
			M2Calculation m2Calculation = new M2Calculation();
			if (m2Queue.size() == 2) {
				m2Queue.poll();
			}
			m2Queue.add(m2);
			Iterator<M2> queueIterator = m2Queue.iterator();
			while (queueIterator.hasNext()) {
				M2 temp = queueIterator.next();
				temp.setRocFlag(true);
			}

			List<M2Calculation> currentM2CalculationRef = m2CalculationList.stream()
					.filter(p -> p.getToDate().equals(m2.getDate())).collect(Collectors.toList());

			if (currentM2CalculationRef.size() > 0)
				m2Calculation = currentM2CalculationRef.get(0);

			if (m2Queue.size() == 1) {
				roc = 0f;
				m2Calculation.setRoc(roc);
				m2Calculation.setToDate(m2.getDate());
			} else {
				roc = (m2.getValue() / ((LinkedList<M2>) m2Queue).get(0).getValue()) - 1;
				m2Calculation.setRoc(roc);
				m2Calculation.setToDate(m2.getDate());
			}
			m2CalculationModified.add(m2Calculation);
		}

		m2Repository.saveAll(m2List);
		m2CalculationRepository.saveAll(m2CalculationModified);

	}

	public List<M2Calculation> calculateRollAvgThreeMonth() {
		List<M2Calculation> m2CalculationList = new ArrayList<>();
		List<M2> m2List = m2Repository.findAll();
		List<M2Calculation> m2CalculationReference = m2CalculationRepository.findAll();
		Queue<M2> m2Queue = new LinkedList<>();

		for (M2 m2 : m2List) {

			if (m2Queue.size() == 3) {
				m2Queue.poll();
			}
			m2Queue.add(m2);

			if (m2.getRollAverageFlag()) {
				continue;
			}
			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			Iterator<M2> queueItr = m2Queue.iterator();

			M2Calculation tempGdpCalculation = new M2Calculation();
			List<M2Calculation> currentM2CalculationRef = m2CalculationReference.stream()
					.filter(p -> p.getToDate().equals(m2.getDate())).collect(Collectors.toList());

			if (currentM2CalculationRef.size() > 0)
				tempGdpCalculation = currentM2CalculationRef.get(0);

			while (queueItr.hasNext()) {
				M2 gdpVal = queueItr.next();
				rollingAvg += gdpVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			m2.setRollAverageFlag(true);
			tempGdpCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			m2CalculationList.add(tempGdpCalculation);

		}

		m2CalculationReference = m2CalculationRepository.saveAll(m2CalculationList);
		m2List = m2Repository.saveAll(m2List);
		return m2CalculationReference;
	}

	public List<M2Calculation> calculateRocRollingAnnualAvg() {

		List<M2Calculation> m2CalculationReference = m2CalculationRepository.findAll();
		Queue<M2Calculation> m2CalculationPriorityQueue = new LinkedList<>();
		for (M2Calculation m2Calculation : m2CalculationReference) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (m2CalculationPriorityQueue.size() == 4) {
				m2CalculationPriorityQueue.poll();
			}
			m2CalculationPriorityQueue.add(m2Calculation);

			if (m2Calculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<M2Calculation> queueIterator = m2CalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				M2Calculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			m2Calculation.setRocAnnRollAvgFlag(true);
			m2Calculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(m2CalculationReference);
		m2CalculationReference = m2CalculationRepository.saveAll(m2CalculationReference);
		return m2CalculationReference;
	}

	public List<M2Calculation> updateRocChangeSign() {
		List<M2Calculation> m2CalculationList = m2CalculationRepository.findAllByRocIsNotNull();
		if(m2CalculationList.isEmpty()){
			return m2CalculationList;
		}
		List<M2Calculation> modifiedSignList = new ArrayList<>();
		M2Calculation m2CalculationPrev = new M2Calculation();

		for (M2Calculation m2Calculation : m2CalculationList) {
			M2Calculation modifiedSigndffCalc = m2Calculation;
			if (m2CalculationPrev.getToDate() == null) {
				modifiedSigndffCalc.setRocChangeSign(0);
			} else {
				if (m2CalculationPrev.getRoc() < modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(1);
				} else if (m2CalculationPrev.getRoc() > modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(-1);
				} else {
					modifiedSigndffCalc.setRocChangeSign(0);
				}
			}
			modifiedSignList.add(modifiedSigndffCalc);
			m2CalculationPrev = modifiedSigndffCalc;
		}
		m2CalculationList = m2CalculationRepository.saveAll(modifiedSignList);
		return m2CalculationList;
	}

}
