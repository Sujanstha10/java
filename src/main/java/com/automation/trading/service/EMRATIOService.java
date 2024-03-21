package com.automation.trading.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.automation.trading.domain.calculation.EMRATIOCalculation;
import com.automation.trading.domain.fred.EMRATIO;
import com.automation.trading.repository.EMRATIOCalculationRepository;
import com.automation.trading.repository.EMRATIORepository;

@Service
public class EMRATIOService {

	@Autowired
	private EMRATIORepository emratioRepository;

	@Autowired
	private EMRATIOCalculationRepository emratioCalculationRepository;

	public void calculateRoc() {

		List<EMRATIO> emratioList = emratioRepository.findAll();
		// List<EMRATIOCalculation> emratioCalculationList =
		// emratioCalculationRepository.findAll();
		List<EMRATIOCalculation> emratioCalculationModified = new ArrayList<>();
		Queue<EMRATIO> emratioQueue = new LinkedList<>();
		for (EMRATIO emratio : emratioList) {
			if (emratio.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;
			int period = 0;
			EMRATIOCalculation emratioCalculation = new EMRATIOCalculation();
			if (emratioQueue.size() == 2) {
				emratioQueue.poll();
			}
			emratioQueue.add(emratio);
			Iterator<EMRATIO> queueIterator = emratioQueue.iterator();
			while (queueIterator.hasNext()) {
				EMRATIO temp = queueIterator.next();
				temp.setRocFlag(true);
			}

//			List<EMRATIOCalculation> currentEMRATIOCalculationRef = emratioCalculationList.stream()
//					.filter(p -> p.getToDate().equals(emratio.getDate())).collect(Collectors.toList());
//
//			if (currentEMRATIOCalculationRef.size() > 0)
//				emratioCalculation = currentEMRATIOCalculationRef.get(0);

			if (emratioQueue.size() == 1) {
				roc = 0f;
				emratioCalculation.setRoc(roc);
				emratioCalculation.setToDate(emratio.getDate());
			} else {
				roc = (emratio.getValue() / ((LinkedList<EMRATIO>) emratioQueue).get(0).getValue()) - 1;
				emratioCalculation.setRoc(roc);
				emratioCalculation.setToDate(emratio.getDate());
			}
			emratioCalculationModified.add(emratioCalculation);
		}

		emratioRepository.saveAll(emratioList);
		emratioCalculationRepository.saveAll(emratioCalculationModified);

	}

	public List<EMRATIOCalculation> calculateRollAvgThreeMonth() {
		List<EMRATIOCalculation> emratioCalculationList = new ArrayList<>();
		List<EMRATIO> emratioList = emratioRepository.findAll();
		List<EMRATIOCalculation> emratioCalculationReference = emratioCalculationRepository.findAll();
		Queue<EMRATIO> emratioQueue = new LinkedList<>();

		for (EMRATIO emratio : emratioList) {

			if (emratioQueue.size() == 3) {
				emratioQueue.poll();
			}
			emratioQueue.add(emratio);

			if (emratio.getRollAverageFlag()) {
				continue;
			}
			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			Iterator<EMRATIO> queueItr = emratioQueue.iterator();

			EMRATIOCalculation tempGdpCalculation = new EMRATIOCalculation();
			List<EMRATIOCalculation> currentEMRATIOCalculationRef = emratioCalculationReference.stream()
					.filter(p -> p.getToDate().equals(emratio.getDate())).collect(Collectors.toList());

			if (currentEMRATIOCalculationRef.size() > 0)
				tempGdpCalculation = currentEMRATIOCalculationRef.get(0);

			while (queueItr.hasNext()) {
				EMRATIO gdpVal = queueItr.next();
				rollingAvg += gdpVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			emratio.setRollAverageFlag(true);
			tempGdpCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			emratioCalculationList.add(tempGdpCalculation);

		}

		emratioCalculationReference = emratioCalculationRepository.saveAll(emratioCalculationList);
		emratioList = emratioRepository.saveAll(emratioList);
		return emratioCalculationReference;
	}

	public List<EMRATIOCalculation> calculateRocRollingAnnualAvg() {

		List<EMRATIOCalculation> emratioCalculationReference = emratioCalculationRepository.findAll();
		Queue<EMRATIOCalculation> emratioCalculationPriorityQueue = new LinkedList<>();
		for (EMRATIOCalculation emratioCalculation : emratioCalculationReference) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (emratioCalculationPriorityQueue.size() == 4) {
				emratioCalculationPriorityQueue.poll();
			}
			emratioCalculationPriorityQueue.add(emratioCalculation);

			if (emratioCalculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<EMRATIOCalculation> queueIterator = emratioCalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				EMRATIOCalculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			emratioCalculation.setRocAnnRollAvgFlag(true);
			emratioCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(emratioCalculationReference);
		emratioCalculationReference = emratioCalculationRepository.saveAll(emratioCalculationReference);
		return emratioCalculationReference;
	}

	public List<EMRATIOCalculation> updateRocChangeSign() {
		List<EMRATIOCalculation> emratioCalculationList = emratioCalculationRepository.findAllByRocIsNotNull();
		if (emratioCalculationList.isEmpty()) {
			return emratioCalculationList;
		}
		List<EMRATIOCalculation> modifiedSignList = new ArrayList<>();
		EMRATIOCalculation emratioCalculationPrev = new EMRATIOCalculation();

		for (EMRATIOCalculation emratioCalculation : emratioCalculationList) {
			EMRATIOCalculation modifiedSigndffCalc = emratioCalculation;
			if (emratioCalculationPrev.getToDate() == null) {
				modifiedSigndffCalc.setRocChangeSign(0);
			} else {
				if (emratioCalculationPrev.getRoc() < modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(1);
				} else if (emratioCalculationPrev.getRoc() > modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(-1);
				} else {
					modifiedSigndffCalc.setRocChangeSign(0);
				}
			}
			modifiedSignList.add(modifiedSigndffCalc);
			emratioCalculationPrev = modifiedSigndffCalc;
		}
		emratioCalculationList = emratioCalculationRepository.saveAll(modifiedSignList);
		return emratioCalculationList;
	}

}
