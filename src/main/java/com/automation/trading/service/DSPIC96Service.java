package com.automation.trading.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.automation.trading.domain.calculation.DSPIC96Calculation;
import com.automation.trading.domain.fred.DSPIC96;
import com.automation.trading.repository.DSPIC96CalculationRepository;
import com.automation.trading.repository.DSPIC96Repository;

@Service
public class DSPIC96Service {
	
	@Autowired
	private DSPIC96Repository dspic96Repository;

	@Autowired
	private DSPIC96CalculationRepository dspic96CalculationRepository;

	public void calculateRoc() {

		List<DSPIC96> dspic96List = dspic96Repository.findAll();
		//List<DSPIC96Calculation> dspic96CalculationList = dspic96CalculationRepository.findAll();
		List<DSPIC96Calculation> dspic96CalculationModified = new ArrayList<>();
		Queue<DSPIC96> dspic96Queue = new LinkedList<>();
		for (DSPIC96 dspic96 : dspic96List) {
			if (dspic96.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;
			int period = 0;
			DSPIC96Calculation dspic96Calculation = new DSPIC96Calculation();
			if (dspic96Queue.size() == 2) {
				dspic96Queue.poll();
			}
			dspic96Queue.add(dspic96);
			Iterator<DSPIC96> queueIterator = dspic96Queue.iterator();
			while (queueIterator.hasNext()) {
				DSPIC96 temp = queueIterator.next();
				temp.setRocFlag(true);
			}

//			List<DSPIC96Calculation> currentDSPIC96CalculationRef = dspic96CalculationList.stream()
//					.filter(p -> p.getToDate().equals(dspic96.getDate())).collect(Collectors.toList());
//
//			if (currentDSPIC96CalculationRef.size() > 0)
//				dspic96Calculation = currentDSPIC96CalculationRef.get(0);

			if (dspic96Queue.size() == 1) {
				roc = 0f;
				dspic96Calculation.setRoc(roc);
				dspic96Calculation.setToDate(dspic96.getDate());
			} else {
				roc = (dspic96.getValue() / ((LinkedList<DSPIC96>) dspic96Queue).get(0).getValue()) - 1;
				dspic96Calculation.setRoc(roc);
				dspic96Calculation.setToDate(dspic96.getDate());
			}
			dspic96CalculationModified.add(dspic96Calculation);
		}

		dspic96Repository.saveAll(dspic96List);
		dspic96CalculationRepository.saveAll(dspic96CalculationModified);

	}

	public List<DSPIC96Calculation> calculateRollAvgThreeMonth() {
		List<DSPIC96Calculation> dspic96CalculationList = new ArrayList<>();
		List<DSPIC96> dspic96List = dspic96Repository.findAll();
		List<DSPIC96Calculation> dspic96CalculationReference = dspic96CalculationRepository.findAll();
		Queue<DSPIC96> dspic96Queue = new LinkedList<>();

		for (DSPIC96 dspic96 : dspic96List) {

			if (dspic96Queue.size() == 3) {
				dspic96Queue.poll();
			}
			dspic96Queue.add(dspic96);

			if (dspic96.getRollAverageFlag()) {
				continue;
			}
			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			Iterator<DSPIC96> queueItr = dspic96Queue.iterator();

			DSPIC96Calculation tempGdpCalculation = new DSPIC96Calculation();
			List<DSPIC96Calculation> currentDSPIC96CalculationRef = dspic96CalculationReference.stream()
					.filter(p -> p.getToDate().equals(dspic96.getDate())).collect(Collectors.toList());

			if (currentDSPIC96CalculationRef.size() > 0)
				tempGdpCalculation = currentDSPIC96CalculationRef.get(0);

			while (queueItr.hasNext()) {
				DSPIC96 gdpVal = queueItr.next();
				rollingAvg += gdpVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			dspic96.setRollAverageFlag(true);
			tempGdpCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			dspic96CalculationList.add(tempGdpCalculation);

		}

		dspic96CalculationReference = dspic96CalculationRepository.saveAll(dspic96CalculationList);
		dspic96List = dspic96Repository.saveAll(dspic96List);
		return dspic96CalculationReference;
	}

	public List<DSPIC96Calculation> calculateRocRollingAnnualAvg() {

		List<DSPIC96Calculation> dspic96CalculationReference = dspic96CalculationRepository.findAll();
		Queue<DSPIC96Calculation> dspic96CalculationPriorityQueue = new LinkedList<>();
		for (DSPIC96Calculation dspic96Calculation : dspic96CalculationReference) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (dspic96CalculationPriorityQueue.size() == 4) {
				dspic96CalculationPriorityQueue.poll();
			}
			dspic96CalculationPriorityQueue.add(dspic96Calculation);

			if (dspic96Calculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<DSPIC96Calculation> queueIterator = dspic96CalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				DSPIC96Calculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			dspic96Calculation.setRocAnnRollAvgFlag(true);
			dspic96Calculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(dspic96CalculationReference);
		dspic96CalculationReference = dspic96CalculationRepository.saveAll(dspic96CalculationReference);
		return dspic96CalculationReference;
	}

	public List<DSPIC96Calculation> updateRocChangeSign() {
		List<DSPIC96Calculation> dspic96CalculationList = dspic96CalculationRepository.findAllByRocIsNotNull();
		if(dspic96CalculationList.isEmpty()){
			return dspic96CalculationList;
		}
		List<DSPIC96Calculation> modifiedSignList = new ArrayList<>();
		DSPIC96Calculation dspic96CalculationPrev = new DSPIC96Calculation();

		for (DSPIC96Calculation dspic96Calculation : dspic96CalculationList) {
			DSPIC96Calculation modifiedSigndffCalc = dspic96Calculation;
			if (dspic96CalculationPrev.getToDate() == null) {
				modifiedSigndffCalc.setRocChangeSign(0);
			} else {
				if (dspic96CalculationPrev.getRoc() < modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(1);
				} else if (dspic96CalculationPrev.getRoc() > modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(-1);
				} else {
					modifiedSigndffCalc.setRocChangeSign(0);
				}
			}
			modifiedSignList.add(modifiedSigndffCalc);
			dspic96CalculationPrev = modifiedSigndffCalc;
		}
		dspic96CalculationList = dspic96CalculationRepository.saveAll(modifiedSignList);
		return dspic96CalculationList;
	}

}
