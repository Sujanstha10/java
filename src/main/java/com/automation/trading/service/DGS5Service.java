package com.automation.trading.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.automation.trading.domain.calculation.DGS5Calculation;
import com.automation.trading.domain.fred.interestrates.DGS5;
import com.automation.trading.repository.DGS5CalculationRepository;
import com.automation.trading.repository.DGS5Repository;

@Service
public class DGS5Service {

	@Autowired
	private DGS5Repository dgs5Repository;

	@Autowired
	private DGS5CalculationRepository dgs5CalculationRepository;

	public void calculateRoc() {

		List<DGS5> dgs5List = dgs5Repository.findAll();
		//List<DGS5Calculation> dgs5CalculationList = dgs5CalculationRepository.findAll();
		List<DGS5Calculation> dgs5CalculationModified = new ArrayList<>();
		Queue<DGS5> dgs5Queue = new LinkedList<>();
		for (DGS5 dgs5 : dgs5List) {
			if (dgs5.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;
			int period = 0;
			DGS5Calculation dgs5Calculation = new DGS5Calculation();
			if (dgs5Queue.size() == 2) {
				dgs5Queue.poll();
			}
			dgs5Queue.add(dgs5);
			Iterator<DGS5> queueIterator = dgs5Queue.iterator();
			while (queueIterator.hasNext()) {
				DGS5 temp = queueIterator.next();
				temp.setRocFlag(true);
			}

//			List<DGS5Calculation> currentDGS5CalculationRef = dgs5CalculationList.stream()
//					.filter(p -> p.getToDate().equals(dgs5.getDate())).collect(Collectors.toList());
//
//			if (currentDGS5CalculationRef.size() > 0)
//				dgs5Calculation = currentDGS5CalculationRef.get(0);

			if (dgs5Queue.size() == 1) {
				roc = 0f;
				dgs5Calculation.setRoc(roc);
				dgs5Calculation.setToDate(dgs5.getDate());
			} else {
				roc = (dgs5.getValue() / ((LinkedList<DGS5>) dgs5Queue).get(0).getValue()) - 1;
				dgs5Calculation.setRoc(roc);
				dgs5Calculation.setToDate(dgs5.getDate());
			}
			dgs5CalculationModified.add(dgs5Calculation);
		}

		dgs5Repository.saveAll(dgs5List);
		dgs5CalculationRepository.saveAll(dgs5CalculationModified);

	}

	public List<DGS5Calculation> calculateRollAvgThreeMonth() {
		List<DGS5Calculation> dgs5CalculationList = new ArrayList<>();
		List<DGS5> dgs5List = dgs5Repository.findAll();
		List<DGS5Calculation> dgs5CalculationReference = dgs5CalculationRepository.findAll();
		Queue<DGS5> dgs5Queue = new LinkedList<>();

		for (DGS5 dgs5 : dgs5List) {

			if (dgs5Queue.size() == 3) {
				dgs5Queue.poll();
			}
			dgs5Queue.add(dgs5);

			if (dgs5.getRollAverageFlag()) {
				continue;
			}
			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			Iterator<DGS5> queueItr = dgs5Queue.iterator();

			DGS5Calculation tempGdpCalculation = new DGS5Calculation();
			List<DGS5Calculation> currentDGS5CalculationRef = dgs5CalculationReference.stream()
					.filter(p -> p.getToDate().equals(dgs5.getDate())).collect(Collectors.toList());

			if (currentDGS5CalculationRef.size() > 0)
				tempGdpCalculation = currentDGS5CalculationRef.get(0);

			while (queueItr.hasNext()) {
				DGS5 gdpVal = queueItr.next();
				rollingAvg += gdpVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			dgs5.setRollAverageFlag(true);
			tempGdpCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			dgs5CalculationList.add(tempGdpCalculation);

		}

		dgs5CalculationReference = dgs5CalculationRepository.saveAll(dgs5CalculationList);
		dgs5List = dgs5Repository.saveAll(dgs5List);
		return dgs5CalculationReference;
	}

	public List<DGS5Calculation> calculateRocRollingAnnualAvg() {

		List<DGS5Calculation> dgs5CalculationReference = dgs5CalculationRepository.findAll();
		Queue<DGS5Calculation> dgs5CalculationPriorityQueue = new LinkedList<>();
		for (DGS5Calculation dgs5Calculation : dgs5CalculationReference) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (dgs5CalculationPriorityQueue.size() == 4) {
				dgs5CalculationPriorityQueue.poll();
			}
			dgs5CalculationPriorityQueue.add(dgs5Calculation);

			if (dgs5Calculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<DGS5Calculation> queueIterator = dgs5CalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				DGS5Calculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			dgs5Calculation.setRocAnnRollAvgFlag(true);
			dgs5Calculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(dgs5CalculationReference);
		dgs5CalculationReference = dgs5CalculationRepository.saveAll(dgs5CalculationReference);
		return dgs5CalculationReference;
	}

	public List<DGS5Calculation> updateRocChangeSignDgs5() {
		List<DGS5Calculation> dgs5CalculationList = dgs5CalculationRepository.findAllByRocIsNotNull();
		if(dgs5CalculationList.isEmpty()){
			return dgs5CalculationList;
		}
		List<DGS5Calculation> modifiedSignList = new ArrayList<>();
		DGS5Calculation dgs5CalculationPrev = new DGS5Calculation();

		for (DGS5Calculation dgs5Calculation : dgs5CalculationList) {
			DGS5Calculation modifiedSigndffCalc = dgs5Calculation;
			if (dgs5CalculationPrev.getToDate() == null) {
				modifiedSigndffCalc.setRocChangeSign(0);
			} else {
				if (dgs5CalculationPrev.getRoc() < modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(1);
				} else if (dgs5CalculationPrev.getRoc() > modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(-1);
				} else {
					modifiedSigndffCalc.setRocChangeSign(0);
				}
			}
			modifiedSignList.add(modifiedSigndffCalc);
			dgs5CalculationPrev = modifiedSigndffCalc;
		}
		dgs5CalculationList = dgs5CalculationRepository.saveAll(modifiedSignList);
		return dgs5CalculationList;
	}

}
