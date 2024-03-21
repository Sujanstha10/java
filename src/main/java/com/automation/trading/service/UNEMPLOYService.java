package com.automation.trading.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.automation.trading.domain.calculation.UNEMPLOYCalculation;
import com.automation.trading.domain.fred.UNEMPLOY;
import com.automation.trading.repository.UNEMPLOYCalculationRepository;
import com.automation.trading.repository.UNEMPLOYRepository;
@Service
public class UNEMPLOYService {

	
	@Autowired
	private UNEMPLOYRepository unemployRepository;

	@Autowired
	private UNEMPLOYCalculationRepository unemployCalculationRepository;

	public void calculateRoc() {
		List<UNEMPLOY> unemployList = unemployRepository.findAll();
		List<UNEMPLOYCalculation> unemployCalculationList = unemployCalculationRepository.findAll();
		List<UNEMPLOYCalculation> unemployCalculationModified = new ArrayList<>();
		Queue<UNEMPLOY> unemployQueue = new LinkedList<>();
		for (UNEMPLOY unemploy : unemployList) {
			if (unemploy.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;
			int period = 0;
			UNEMPLOYCalculation baseCalculation = new UNEMPLOYCalculation();
			if (unemployQueue.size() == 2) {
				unemployQueue.poll();
			}
			unemployQueue.add(unemploy);
			Iterator<UNEMPLOY> queueIterator = unemployQueue.iterator();
			while (queueIterator.hasNext()) {
				UNEMPLOY temp = queueIterator.next();
				temp.setRocFlag(true);
			}

			List<UNEMPLOYCalculation> currentUNEMPLOYCalculationRef = unemployCalculationList.stream()
					.filter(p -> p.getToDate().equals(unemploy.getDate())).collect(Collectors.toList());

			if (currentUNEMPLOYCalculationRef.size() > 0)
				baseCalculation = currentUNEMPLOYCalculationRef.get(0);

			if (unemployQueue.size() == 1) {
				roc = 0f;
				baseCalculation.setRoc(roc);
				baseCalculation.setToDate(unemploy.getDate());
			} else {
				roc = (unemploy.getValue() / ((LinkedList<UNEMPLOY>) unemployQueue).get(0).getValue()) - 1;
				baseCalculation.setRoc(roc);
				baseCalculation.setToDate(unemploy.getDate());
			}
			unemployCalculationModified.add(baseCalculation);
		}

		unemployRepository.saveAll(unemployList);
		unemployCalculationRepository.saveAll(unemployCalculationModified);

	}

	public List<UNEMPLOYCalculation> calculateRollAvgThreeMonth() {
		List<UNEMPLOYCalculation> unemployCalculationList = new ArrayList<>();
		List<UNEMPLOY> unemployList = unemployRepository.findAll();
		List<UNEMPLOYCalculation> unemployCalculationReference = unemployCalculationRepository.findAll();
		Queue<UNEMPLOY> unemployQueue = new LinkedList<>();

		for (UNEMPLOY unemploy : unemployList) {

			if (unemployQueue.size() == 3) {
				unemployQueue.poll();
			}
			unemployQueue.add(unemploy);

			if (unemploy.getRollAverageFlag()) {
				continue;
			}
			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			Iterator<UNEMPLOY> queueItr = unemployQueue.iterator();

			UNEMPLOYCalculation tempGdpCalculation = new UNEMPLOYCalculation();
			List<UNEMPLOYCalculation> currentUNEMPLOYCalculationRef = unemployCalculationReference.stream()
					.filter(p -> p.getToDate().equals(unemploy.getDate())).collect(Collectors.toList());

			if (currentUNEMPLOYCalculationRef.size() > 0)
				tempGdpCalculation = currentUNEMPLOYCalculationRef.get(0);

			while (queueItr.hasNext()) {
				UNEMPLOY gdpVal = queueItr.next();
				rollingAvg += gdpVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			unemploy.setRollAverageFlag(true);
			tempGdpCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			unemployCalculationList.add(tempGdpCalculation);

		}

		unemployCalculationReference = unemployCalculationRepository.saveAll(unemployCalculationList);
		unemployList = unemployRepository.saveAll(unemployList);
		return unemployCalculationReference;
	}

	public List<UNEMPLOYCalculation> calculateRocRollingAnnualAvg() {

		List<UNEMPLOYCalculation> unemployCalculationReference = unemployCalculationRepository.findAll();
		Queue<UNEMPLOYCalculation> unemployCalculationPriorityQueue = new LinkedList<>();
		for (UNEMPLOYCalculation unemployCalculation : unemployCalculationReference) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (unemployCalculationPriorityQueue.size() == 4) {
				unemployCalculationPriorityQueue.poll();
			}
			unemployCalculationPriorityQueue.add(unemployCalculation);

			if (unemployCalculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<UNEMPLOYCalculation> queueIterator = unemployCalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				UNEMPLOYCalculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			unemployCalculation.setRocAnnRollAvgFlag(true);
			unemployCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(unemployCalculationReference);
		unemployCalculationReference = unemployCalculationRepository.saveAll(unemployCalculationReference);
		return unemployCalculationReference;
	}

	public List<UNEMPLOYCalculation> updateRocChangeSignUNEMPLOY() {

		List<UNEMPLOYCalculation> unemployCalculationList = unemployCalculationRepository.findAllByRocIsNotNull();
		if (unemployCalculationList.isEmpty()) {
			return unemployCalculationList;
		}

		List<UNEMPLOYCalculation> modifiedSignList = new ArrayList<>();
		UNEMPLOYCalculation unemployCalculationPrev = new UNEMPLOYCalculation();

		for (UNEMPLOYCalculation unemployCalculation : unemployCalculationList) {
			UNEMPLOYCalculation modifiedSigndffCalc = unemployCalculation;
			if (unemployCalculationPrev.getToDate() == null) {
				modifiedSigndffCalc.setRocChangeSign(0);
			} else {
				if (unemployCalculationPrev.getRoc() < modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(1);
				} else if (unemployCalculationPrev.getRoc() > modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(-1);
				} else {
					modifiedSigndffCalc.setRocChangeSign(0);
				}
			}
			modifiedSignList.add(modifiedSigndffCalc);
			unemployCalculationPrev = modifiedSigndffCalc;
		}
		unemployCalculationList = unemployCalculationRepository.saveAll(modifiedSignList);
		return unemployCalculationList;
	}
}
