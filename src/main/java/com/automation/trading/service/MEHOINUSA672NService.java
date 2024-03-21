package com.automation.trading.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.automation.trading.domain.calculation.MEHOINUSA672NCalculation;
import com.automation.trading.domain.fred.MEHOINUSA672N;
import com.automation.trading.repository.MEHOINUSA672NCalculationRepository;
import com.automation.trading.repository.MEHOINUSA672NRepository;

@Service
public class MEHOINUSA672NService {
	
	@Autowired
	private MEHOINUSA672NRepository mehoinusa672nRepository;

	@Autowired
	private MEHOINUSA672NCalculationRepository mehoinusa672nCalculationRepository;

	public void calculateRoc() {

		List<MEHOINUSA672N> mehoinusa672nList = mehoinusa672nRepository.findAll();
		// List<MEHOINUSA672NCalculation> mehoinusa672nCalculationList =
		// mehoinusa672nCalculationRepository.findAll();
		List<MEHOINUSA672NCalculation> mehoinusa672nCalculationModified = new ArrayList<>();
		Queue<MEHOINUSA672N> mehoinusa672nQueue = new LinkedList<>();
		for (MEHOINUSA672N mehoinusa672n : mehoinusa672nList) {
			if (mehoinusa672n.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;
			int period = 0;
			MEHOINUSA672NCalculation mehoinusa672nCalculation = new MEHOINUSA672NCalculation();
			if (mehoinusa672nQueue.size() == 2) {
				mehoinusa672nQueue.poll();
			}
			mehoinusa672nQueue.add(mehoinusa672n);
			Iterator<MEHOINUSA672N> queueIterator = mehoinusa672nQueue.iterator();
			while (queueIterator.hasNext()) {
				MEHOINUSA672N temp = queueIterator.next();
				temp.setRocFlag(true);
			}

//			List<MEHOINUSA672NCalculation> currentMEHOINUSA672NCalculationRef = mehoinusa672nCalculationList.stream()
//					.filter(p -> p.getToDate().equals(mehoinusa672n.getDate())).collect(Collectors.toList());
//
//			if (currentMEHOINUSA672NCalculationRef.size() > 0)
//				mehoinusa672nCalculation = currentMEHOINUSA672NCalculationRef.get(0);

			if (mehoinusa672nQueue.size() == 1) {
				roc = 0f;
				mehoinusa672nCalculation.setRoc(roc);
				mehoinusa672nCalculation.setToDate(mehoinusa672n.getDate());
			} else {
				roc = (mehoinusa672n.getValue() / ((LinkedList<MEHOINUSA672N>) mehoinusa672nQueue).get(0).getValue()) - 1;
				mehoinusa672nCalculation.setRoc(roc);
				mehoinusa672nCalculation.setToDate(mehoinusa672n.getDate());
			}
			mehoinusa672nCalculationModified.add(mehoinusa672nCalculation);
		}

		mehoinusa672nRepository.saveAll(mehoinusa672nList);
		mehoinusa672nCalculationRepository.saveAll(mehoinusa672nCalculationModified);

	}

	public List<MEHOINUSA672NCalculation> calculateRollAvgThreeMonth() {
		List<MEHOINUSA672NCalculation> mehoinusa672nCalculationList = new ArrayList<>();
		List<MEHOINUSA672N> mehoinusa672nList = mehoinusa672nRepository.findAll();
		List<MEHOINUSA672NCalculation> mehoinusa672nCalculationReference = mehoinusa672nCalculationRepository.findAll();
		Queue<MEHOINUSA672N> mehoinusa672nQueue = new LinkedList<>();

		for (MEHOINUSA672N mehoinusa672n : mehoinusa672nList) {

			if (mehoinusa672nQueue.size() == 3) {
				mehoinusa672nQueue.poll();
			}
			mehoinusa672nQueue.add(mehoinusa672n);

			if (mehoinusa672n.getRollAverageFlag()) {
				continue;
			}
			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			Iterator<MEHOINUSA672N> queueItr = mehoinusa672nQueue.iterator();

			MEHOINUSA672NCalculation tempGdpCalculation = new MEHOINUSA672NCalculation();
			List<MEHOINUSA672NCalculation> currentMEHOINUSA672NCalculationRef = mehoinusa672nCalculationReference.stream()
					.filter(p -> p.getToDate().equals(mehoinusa672n.getDate())).collect(Collectors.toList());

			if (currentMEHOINUSA672NCalculationRef.size() > 0)
				tempGdpCalculation = currentMEHOINUSA672NCalculationRef.get(0);

			while (queueItr.hasNext()) {
				MEHOINUSA672N gdpVal = queueItr.next();
				rollingAvg += gdpVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			mehoinusa672n.setRollAverageFlag(true);
			tempGdpCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			mehoinusa672nCalculationList.add(tempGdpCalculation);

		}

		mehoinusa672nCalculationReference = mehoinusa672nCalculationRepository.saveAll(mehoinusa672nCalculationList);
		mehoinusa672nList = mehoinusa672nRepository.saveAll(mehoinusa672nList);
		return mehoinusa672nCalculationReference;
	}

	public List<MEHOINUSA672NCalculation> calculateRocRollingAnnualAvg() {

		List<MEHOINUSA672NCalculation> mehoinusa672nCalculationReference = mehoinusa672nCalculationRepository.findAll();
		Queue<MEHOINUSA672NCalculation> mehoinusa672nCalculationPriorityQueue = new LinkedList<>();
		for (MEHOINUSA672NCalculation mehoinusa672nCalculation : mehoinusa672nCalculationReference) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (mehoinusa672nCalculationPriorityQueue.size() == 4) {
				mehoinusa672nCalculationPriorityQueue.poll();
			}
			mehoinusa672nCalculationPriorityQueue.add(mehoinusa672nCalculation);

			if (mehoinusa672nCalculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<MEHOINUSA672NCalculation> queueIterator = mehoinusa672nCalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				MEHOINUSA672NCalculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			mehoinusa672nCalculation.setRocAnnRollAvgFlag(true);
			mehoinusa672nCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(mehoinusa672nCalculationReference);
		mehoinusa672nCalculationReference = mehoinusa672nCalculationRepository.saveAll(mehoinusa672nCalculationReference);
		return mehoinusa672nCalculationReference;
	}

	public List<MEHOINUSA672NCalculation> updateRocChangeSign() {
		List<MEHOINUSA672NCalculation> mehoinusa672nCalculationList = mehoinusa672nCalculationRepository.findAllByRocIsNotNull();
		if (mehoinusa672nCalculationList.isEmpty()) {
			return mehoinusa672nCalculationList;
		}
		List<MEHOINUSA672NCalculation> modifiedSignList = new ArrayList<>();
		MEHOINUSA672NCalculation mehoinusa672nCalculationPrev = new MEHOINUSA672NCalculation();

		for (MEHOINUSA672NCalculation mehoinusa672nCalculation : mehoinusa672nCalculationList) {
			MEHOINUSA672NCalculation modifiedSigndffCalc = mehoinusa672nCalculation;
			if (mehoinusa672nCalculationPrev.getToDate() == null) {
				modifiedSigndffCalc.setRocChangeSign(0);
			} else {
				if (mehoinusa672nCalculationPrev.getRoc() < modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(1);
				} else if (mehoinusa672nCalculationPrev.getRoc() > modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(-1);
				} else {
					modifiedSigndffCalc.setRocChangeSign(0);
				}
			}
			modifiedSignList.add(modifiedSigndffCalc);
			mehoinusa672nCalculationPrev = modifiedSigndffCalc;
		}
		mehoinusa672nCalculationList = mehoinusa672nCalculationRepository.saveAll(modifiedSignList);
		return mehoinusa672nCalculationList;
	}

}
