package com.automation.trading.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.automation.trading.domain.calculation.DGS30Calculation;
import com.automation.trading.domain.fred.interestrates.DGS30;
import com.automation.trading.repository.DGS30CalculationRepository;
import com.automation.trading.repository.DGS30Repository;

@Service
public class DGS30Service {
	
	@Autowired
	private DGS30Repository dgs30Repository;

	@Autowired
	private DGS30CalculationRepository dgs30CalculationRepository;

	public void calculateRoc() {

		List<DGS30> dgs30List = dgs30Repository.findAll();
		List<DGS30Calculation> dgs30CalculationList = dgs30CalculationRepository.findAll();
		List<DGS30Calculation> dgs30CalculationModified = new ArrayList<>();
		Queue<DGS30> dgs30Queue = new LinkedList<>();
		for (DGS30 dgs30 : dgs30List) {
			if (dgs30.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;
			int period = 0;
			DGS30Calculation dgs30Calculation = new DGS30Calculation();
			if (dgs30Queue.size() == 2) {
				dgs30Queue.poll();
			}
			dgs30Queue.add(dgs30);
			Iterator<DGS30> queueIterator = dgs30Queue.iterator();
			while (queueIterator.hasNext()) {
				DGS30 temp = queueIterator.next();
				temp.setRocFlag(true);
			}

			List<DGS30Calculation> currentDGS30CalculationRef = dgs30CalculationList.stream()
					.filter(p -> p.getToDate().equals(dgs30.getDate())).collect(Collectors.toList());

			if (currentDGS30CalculationRef.size() > 0)
				dgs30Calculation = currentDGS30CalculationRef.get(0);

			if (dgs30Queue.size() == 1) {
				roc = 0f;
				dgs30Calculation.setRoc(roc);
				dgs30Calculation.setToDate(dgs30.getDate());
			} else {
				roc = (dgs30.getValue() / ((LinkedList<DGS30>) dgs30Queue).get(0).getValue()) - 1;
				dgs30Calculation.setRoc(roc);
				dgs30Calculation.setToDate(dgs30.getDate());
			}
			dgs30CalculationModified.add(dgs30Calculation);
		}

		dgs30Repository.saveAll(dgs30List);
		dgs30CalculationRepository.saveAll(dgs30CalculationModified);

	}

	public List<DGS30Calculation> calculateRollAvgThreeMonth() {
		List<DGS30Calculation> dgs30CalculationList = new ArrayList<>();
		List<DGS30> dgs30List = dgs30Repository.findAll();
		List<DGS30Calculation> dgs30CalculationReference = dgs30CalculationRepository.findAll();
		Queue<DGS30> dgs30Queue = new LinkedList<>();

		for (DGS30 dgs30 : dgs30List) {

			if (dgs30Queue.size() == 3) {
				dgs30Queue.poll();
			}
			dgs30Queue.add(dgs30);

			if (dgs30.getRollAverageFlag()) {
				continue;
			}
			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			Iterator<DGS30> queueItr = dgs30Queue.iterator();

			DGS30Calculation tempGdpCalculation = new DGS30Calculation();
			List<DGS30Calculation> currentDGS30CalculationRef = dgs30CalculationReference.stream()
					.filter(p -> p.getToDate().equals(dgs30.getDate())).collect(Collectors.toList());

			if (currentDGS30CalculationRef.size() > 0)
				tempGdpCalculation = currentDGS30CalculationRef.get(0);

			while (queueItr.hasNext()) {
				DGS30 gdpVal = queueItr.next();
				rollingAvg += gdpVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			dgs30.setRollAverageFlag(true);
			tempGdpCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			dgs30CalculationList.add(tempGdpCalculation);

		}

		dgs30CalculationReference = dgs30CalculationRepository.saveAll(dgs30CalculationList);
		dgs30List = dgs30Repository.saveAll(dgs30List);
		return dgs30CalculationReference;
	}

	public List<DGS30Calculation> calculateRocRollingAnnualAvg() {

		List<DGS30Calculation> dgs30CalculationReference = dgs30CalculationRepository.findAll();
		Queue<DGS30Calculation> dgs30CalculationPriorityQueue = new LinkedList<>();
		for (DGS30Calculation dgs30Calculation : dgs30CalculationReference) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (dgs30CalculationPriorityQueue.size() == 4) {
				dgs30CalculationPriorityQueue.poll();
			}
			dgs30CalculationPriorityQueue.add(dgs30Calculation);

			if (dgs30Calculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<DGS30Calculation> queueIterator = dgs30CalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				DGS30Calculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			dgs30Calculation.setRocAnnRollAvgFlag(true);
			dgs30Calculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(dgs30CalculationReference);
		dgs30CalculationReference = dgs30CalculationRepository.saveAll(dgs30CalculationReference);
		return dgs30CalculationReference;
	}


	public List<DGS30Calculation> updateRocChangeSign() {
		List<DGS30Calculation> dgs30CalculationList = dgs30CalculationRepository.findAllByRocIsNotNull();
		if(dgs30CalculationList.isEmpty()){
			return dgs30CalculationList;
		}
		List<DGS30Calculation> modifiedSignList = new ArrayList<>();
		DGS30Calculation dgs30CalculationPrev = new DGS30Calculation();

		for (DGS30Calculation dgs30Calculation : dgs30CalculationList) {
			DGS30Calculation modifiedSigndffCalc = dgs30Calculation;
			if (dgs30CalculationPrev.getToDate() == null) {
				modifiedSigndffCalc.setRocChangeSign(0);
			} else {
				if (dgs30CalculationPrev.getRoc() < modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(1);
				} else if (dgs30CalculationPrev.getRoc() > modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(-1);
				} else {
					modifiedSigndffCalc.setRocChangeSign(0);
				}
			}
			modifiedSignList.add(modifiedSigndffCalc);
			dgs30CalculationPrev = modifiedSigndffCalc;
		}
		dgs30CalculationList = dgs30CalculationRepository.saveAll(modifiedSignList);
		return dgs30CalculationList;
	}

}
