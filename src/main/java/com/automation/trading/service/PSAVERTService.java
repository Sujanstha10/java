package com.automation.trading.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.automation.trading.domain.calculation.PSAVERTCalculation;
import com.automation.trading.domain.fred.PSAVERT;
import com.automation.trading.repository.PSAVERTCalculationRepository;
import com.automation.trading.repository.PSAVERTRepository;

@Service
public class PSAVERTService {
	
	@Autowired
	private PSAVERTRepository psavertRepository;

	@Autowired
	private PSAVERTCalculationRepository psavertCalculationRepository;

	public void calculateRoc() {

		List<PSAVERT> psavertList = psavertRepository.findAll();
		// List<PSAVERTCalculation> psavertCalculationList =
		// psavertCalculationRepository.findAll();
		List<PSAVERTCalculation> psavertCalculationModified = new ArrayList<>();
		Queue<PSAVERT> psavertQueue = new LinkedList<>();
		for (PSAVERT psavert : psavertList) {
			if (psavert.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;
			int period = 0;
			PSAVERTCalculation psavertCalculation = new PSAVERTCalculation();
			if (psavertQueue.size() == 2) {
				psavertQueue.poll();
			}
			psavertQueue.add(psavert);
			Iterator<PSAVERT> queueIterator = psavertQueue.iterator();
			while (queueIterator.hasNext()) {
				PSAVERT temp = queueIterator.next();
				temp.setRocFlag(true);
			}

//			List<PSAVERTCalculation> currentPSAVERTCalculationRef = psavertCalculationList.stream()
//					.filter(p -> p.getToDate().equals(psavert.getDate())).collect(Collectors.toList());
//
//			if (currentPSAVERTCalculationRef.size() > 0)
//				psavertCalculation = currentPSAVERTCalculationRef.get(0);

			if (psavertQueue.size() == 1) {
				roc = 0f;
				psavertCalculation.setRoc(roc);
				psavertCalculation.setToDate(psavert.getDate());
			} else {
				roc = (psavert.getValue() / ((LinkedList<PSAVERT>) psavertQueue).get(0).getValue()) - 1;
				psavertCalculation.setRoc(roc);
				psavertCalculation.setToDate(psavert.getDate());
			}
			psavertCalculationModified.add(psavertCalculation);
		}

		psavertRepository.saveAll(psavertList);
		psavertCalculationRepository.saveAll(psavertCalculationModified);

	}

	public List<PSAVERTCalculation> calculateRollAvgThreeMonth() {
		List<PSAVERTCalculation> psavertCalculationList = new ArrayList<>();
		List<PSAVERT> psavertList = psavertRepository.findAll();
		List<PSAVERTCalculation> psavertCalculationReference = psavertCalculationRepository.findAll();
		Queue<PSAVERT> psavertQueue = new LinkedList<>();

		for (PSAVERT psavert : psavertList) {

			if (psavertQueue.size() == 3) {
				psavertQueue.poll();
			}
			psavertQueue.add(psavert);

			if (psavert.getRollAverageFlag()) {
				continue;
			}
			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			Iterator<PSAVERT> queueItr = psavertQueue.iterator();

			PSAVERTCalculation tempGdpCalculation = new PSAVERTCalculation();
			List<PSAVERTCalculation> currentPSAVERTCalculationRef = psavertCalculationReference.stream()
					.filter(p -> p.getToDate().equals(psavert.getDate())).collect(Collectors.toList());

			if (currentPSAVERTCalculationRef.size() > 0)
				tempGdpCalculation = currentPSAVERTCalculationRef.get(0);

			while (queueItr.hasNext()) {
				PSAVERT gdpVal = queueItr.next();
				rollingAvg += gdpVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			psavert.setRollAverageFlag(true);
			tempGdpCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			psavertCalculationList.add(tempGdpCalculation);

		}

		psavertCalculationReference = psavertCalculationRepository.saveAll(psavertCalculationList);
		psavertList = psavertRepository.saveAll(psavertList);
		return psavertCalculationReference;
	}

	public List<PSAVERTCalculation> calculateRocRollingAnnualAvg() {

		List<PSAVERTCalculation> psavertCalculationReference = psavertCalculationRepository.findAll();
		Queue<PSAVERTCalculation> psavertCalculationPriorityQueue = new LinkedList<>();
		for (PSAVERTCalculation psavertCalculation : psavertCalculationReference) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (psavertCalculationPriorityQueue.size() == 4) {
				psavertCalculationPriorityQueue.poll();
			}
			psavertCalculationPriorityQueue.add(psavertCalculation);

			if (psavertCalculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<PSAVERTCalculation> queueIterator = psavertCalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				PSAVERTCalculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			psavertCalculation.setRocAnnRollAvgFlag(true);
			psavertCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(psavertCalculationReference);
		psavertCalculationReference = psavertCalculationRepository.saveAll(psavertCalculationReference);
		return psavertCalculationReference;
	}

	public List<PSAVERTCalculation> updateRocChangeSign() {
		List<PSAVERTCalculation> psavertCalculationList = psavertCalculationRepository.findAllByRocIsNotNull();
		if (psavertCalculationList.isEmpty()) {
			return psavertCalculationList;
		}
		List<PSAVERTCalculation> modifiedSignList = new ArrayList<>();
		PSAVERTCalculation psavertCalculationPrev = new PSAVERTCalculation();

		for (PSAVERTCalculation psavertCalculation : psavertCalculationList) {
			PSAVERTCalculation modifiedSigndffCalc = psavertCalculation;
			if (psavertCalculationPrev.getToDate() == null) {
				modifiedSigndffCalc.setRocChangeSign(0);
			} else {
				if (psavertCalculationPrev.getRoc() < modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(1);
				} else if (psavertCalculationPrev.getRoc() > modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(-1);
				} else {
					modifiedSigndffCalc.setRocChangeSign(0);
				}
			}
			modifiedSignList.add(modifiedSigndffCalc);
			psavertCalculationPrev = modifiedSigndffCalc;
		}
		psavertCalculationList = psavertCalculationRepository.saveAll(modifiedSignList);
		return psavertCalculationList;
	}

}
