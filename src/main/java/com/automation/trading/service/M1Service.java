package com.automation.trading.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.automation.trading.domain.calculation.M1Calculation;
import com.automation.trading.domain.fred.M1;
import com.automation.trading.repository.M1CalculationRepository;
import com.automation.trading.repository.M1Repository;

@Service
public class M1Service {
	
	@Autowired
	private M1Repository m1Repository;
	
	@Autowired
	private M1CalculationRepository m1CalculationRepository;

	public void calculateRoc() {
		List<M1> m1List = m1Repository.findAll();
		List<M1Calculation> m1CalculationList = m1CalculationRepository.findAll();
		List<M1Calculation> m1CalculationModified = new ArrayList<>();
		Queue<M1> m1Queue = new LinkedList<>();
		for (M1 m1 : m1List) {
			if (m1.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;
			int period = 0;
			M1Calculation baseCalculation = new M1Calculation();
			if (m1Queue.size() == 2) {
				m1Queue.poll();
			}
			m1Queue.add(m1);
			Iterator<M1> queueIterator = m1Queue.iterator();
			while (queueIterator.hasNext()) {
				M1 temp = queueIterator.next();
				temp.setRocFlag(true);
			}

			List<M1Calculation> currentM1CalculationRef = m1CalculationList.stream()
					.filter(p -> p.getToDate().equals(m1.getDate())).collect(Collectors.toList());

			if (currentM1CalculationRef.size() > 0)
				baseCalculation = currentM1CalculationRef.get(0);

			if (m1Queue.size() == 1) {
				roc = 0f;
				baseCalculation.setRoc(roc);
				baseCalculation.setToDate(m1.getDate());
			} else {
				roc = (m1.getValue() / ((LinkedList<M1>) m1Queue).get(0).getValue()) - 1;
				baseCalculation.setRoc(roc);
				baseCalculation.setToDate(m1.getDate());
			}
			m1CalculationModified.add(baseCalculation);
		}

		m1Repository.saveAll(m1List);
		m1CalculationRepository.saveAll(m1CalculationModified);
		
		
	}
	
	public List<M1Calculation> calculateRollAvgThreeMonth() {
		List<M1Calculation> m1CalculationList = new ArrayList<>();
		List<M1> m1List = m1Repository.findAll();
		List<M1Calculation> m1CalculationReference = m1CalculationRepository.findAll();
		Queue<M1> m1Queue = new LinkedList<>();

		for (M1 m1 : m1List) {

			if (m1Queue.size() == 3) {
				m1Queue.poll();
			}
			m1Queue.add(m1);

			if (m1.getRollAverageFlag()) {
				continue;
			}
			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			Iterator<M1> queueItr = m1Queue.iterator();

			M1Calculation tempGdpCalculation = new M1Calculation();
			List<M1Calculation> currentM1CalculationRef = m1CalculationReference.stream()
					.filter(p -> p.getToDate().equals(m1.getDate())).collect(Collectors.toList());

			if (currentM1CalculationRef.size() > 0)
				tempGdpCalculation = currentM1CalculationRef.get(0);

			while (queueItr.hasNext()) {
				M1 gdpVal = queueItr.next();
				rollingAvg += gdpVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			m1.setRollAverageFlag(true);
			tempGdpCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			m1CalculationList.add(tempGdpCalculation);

		}

		m1CalculationReference = m1CalculationRepository.saveAll(m1CalculationList);
		m1List = m1Repository.saveAll(m1List);
		return m1CalculationReference;
	}

	public List<M1Calculation> calculateRocRollingAnnualAvg() {

		List<M1Calculation> m1CalculationReference = m1CalculationRepository.findAll();
		Queue<M1Calculation> m1CalculationPriorityQueue = new LinkedList<>();
		for (M1Calculation m1Calculation : m1CalculationReference) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (m1CalculationPriorityQueue.size() == 4) {
				m1CalculationPriorityQueue.poll();
			}
			m1CalculationPriorityQueue.add(m1Calculation);

			if (m1Calculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<M1Calculation> queueIterator = m1CalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				M1Calculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			m1Calculation.setRocAnnRollAvgFlag(true);
			m1Calculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(m1CalculationReference);
		m1CalculationReference = m1CalculationRepository.saveAll(m1CalculationReference);
		return m1CalculationReference;
	}

	public List<M1Calculation> updateRocChangeSign() {
		List<M1Calculation> m1CalculationList = m1CalculationRepository.findAllByRocIsNotNull();
		if(m1CalculationList.isEmpty()){
			return m1CalculationList;
		}
		List<M1Calculation> modifiedSignList = new ArrayList<>();
		M1Calculation m1CalculationPrev = new M1Calculation();

		for (M1Calculation m1Calculation : m1CalculationList) {
			M1Calculation modifiedSigndffCalc = m1Calculation;
			if (m1CalculationPrev.getToDate() == null) {
				modifiedSigndffCalc.setRocChangeSign(0);
			} else {
				if (m1CalculationPrev.getRoc() < modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(1);
				} else if (m1CalculationPrev.getRoc() > modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(-1);
				} else {
					modifiedSigndffCalc.setRocChangeSign(0);
				}
			}
			modifiedSignList.add(modifiedSigndffCalc);
			m1CalculationPrev = modifiedSigndffCalc;
		}
		m1CalculationList = m1CalculationRepository.saveAll(modifiedSignList);
		return m1CalculationList;
	}

}
