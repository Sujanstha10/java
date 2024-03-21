package com.automation.trading.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.automation.trading.domain.calculation.PCECalculation;
import com.automation.trading.domain.fred.PCE;
import com.automation.trading.repository.PCECalculationRepository;
import com.automation.trading.repository.PCERepository;

@Service
public class PCEService {

	@Autowired
	private PCERepository pceRepository;

	@Autowired
	private PCECalculationRepository pceCalculationRepository;

	public void calculateRoc() {

		List<PCE> pceList = pceRepository.findAll();
		// List<PCECalculation> pceCalculationList =
		// pceCalculationRepository.findAll();
		List<PCECalculation> pceCalculationModified = new ArrayList<>();
		Queue<PCE> pceQueue = new LinkedList<>();
		for (PCE pce : pceList) {
			if (pce.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;
			int period = 0;
			PCECalculation pceCalculation = new PCECalculation();
			if (pceQueue.size() == 2) {
				pceQueue.poll();
			}
			pceQueue.add(pce);
			Iterator<PCE> queueIterator = pceQueue.iterator();
			while (queueIterator.hasNext()) {
				PCE temp = queueIterator.next();
				temp.setRocFlag(true);
			}

//			List<PCECalculation> currentPCECalculationRef = pceCalculationList.stream()
//					.filter(p -> p.getToDate().equals(pce.getDate())).collect(Collectors.toList());
//
//			if (currentPCECalculationRef.size() > 0)
//				pceCalculation = currentPCECalculationRef.get(0);

			if (pceQueue.size() == 1) {
				roc = 0f;
				pceCalculation.setRoc(roc);
				pceCalculation.setToDate(pce.getDate());
			} else {
				roc = (pce.getValue() / ((LinkedList<PCE>) pceQueue).get(0).getValue()) - 1;
				pceCalculation.setRoc(roc);
				pceCalculation.setToDate(pce.getDate());
			}
			pceCalculationModified.add(pceCalculation);
		}

		pceRepository.saveAll(pceList);
		pceCalculationRepository.saveAll(pceCalculationModified);

	}

	public List<PCECalculation> calculateRollAvgThreeMonth() {
		List<PCECalculation> pceCalculationList = new ArrayList<>();
		List<PCE> pceList = pceRepository.findAll();
		List<PCECalculation> pceCalculationReference = pceCalculationRepository.findAll();
		Queue<PCE> pceQueue = new LinkedList<>();

		for (PCE pce : pceList) {

			if (pceQueue.size() == 3) {
				pceQueue.poll();
			}
			pceQueue.add(pce);

			if (pce.getRollAverageFlag()) {
				continue;
			}
			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			Iterator<PCE> queueItr = pceQueue.iterator();

			PCECalculation tempGdpCalculation = new PCECalculation();
			List<PCECalculation> currentPCECalculationRef = pceCalculationReference.stream()
					.filter(p -> p.getToDate().equals(pce.getDate())).collect(Collectors.toList());

			if (currentPCECalculationRef.size() > 0)
				tempGdpCalculation = currentPCECalculationRef.get(0);

			while (queueItr.hasNext()) {
				PCE gdpVal = queueItr.next();
				rollingAvg += gdpVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			pce.setRollAverageFlag(true);
			tempGdpCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			pceCalculationList.add(tempGdpCalculation);

		}

		pceCalculationReference = pceCalculationRepository.saveAll(pceCalculationList);
		pceList = pceRepository.saveAll(pceList);
		return pceCalculationReference;
	}

	public List<PCECalculation> calculateRocRollingAnnualAvg() {

		List<PCECalculation> pceCalculationReference = pceCalculationRepository.findAll();
		Queue<PCECalculation> pceCalculationPriorityQueue = new LinkedList<>();
		for (PCECalculation pceCalculation : pceCalculationReference) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (pceCalculationPriorityQueue.size() == 4) {
				pceCalculationPriorityQueue.poll();
			}
			pceCalculationPriorityQueue.add(pceCalculation);

			if (pceCalculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<PCECalculation> queueIterator = pceCalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				PCECalculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			pceCalculation.setRocAnnRollAvgFlag(true);
			pceCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(pceCalculationReference);
		pceCalculationReference = pceCalculationRepository.saveAll(pceCalculationReference);
		return pceCalculationReference;
	}

	public List<PCECalculation> updateRocChangeSign() {
		List<PCECalculation> pceCalculationList = pceCalculationRepository.findAllByRocIsNotNull();
		if (pceCalculationList.isEmpty()) {
			return pceCalculationList;
		}
		List<PCECalculation> modifiedSignList = new ArrayList<>();
		PCECalculation pceCalculationPrev = new PCECalculation();

		for (PCECalculation pceCalculation : pceCalculationList) {
			PCECalculation modifiedSigndffCalc = pceCalculation;
			if (pceCalculationPrev.getToDate() == null) {
				modifiedSigndffCalc.setRocChangeSign(0);
			} else {
				if (pceCalculationPrev.getRoc() < modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(1);
				} else if (pceCalculationPrev.getRoc() > modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(-1);
				} else {
					modifiedSigndffCalc.setRocChangeSign(0);
				}
			}
			modifiedSignList.add(modifiedSigndffCalc);
			pceCalculationPrev = modifiedSigndffCalc;
		}
		pceCalculationList = pceCalculationRepository.saveAll(modifiedSignList);
		return pceCalculationList;
	}
}
