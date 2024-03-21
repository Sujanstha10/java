package com.automation.trading.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.automation.trading.domain.calculation.DPRIMECalculation;
import com.automation.trading.domain.fred.DPRIME;
import com.automation.trading.repository.DPRIMECalculationRepository;
import com.automation.trading.repository.DPRIMERepository;

@Service
public class DPRIMEService {

	@Autowired
	private DPRIMERepository dprimeRepository;

	@Autowired
	private DPRIMECalculationRepository dprimeCalculationRepository;

	public void calculateRoc() {

		List<DPRIME> dprimeList = dprimeRepository.findAll();
		// List<DPRIMECalculation> dprimeCalculationList =
		// dprimeCalculationRepository.findAll();
		List<DPRIMECalculation> dprimeCalculationModified = new ArrayList<>();
		Queue<DPRIME> dprimeQueue = new LinkedList<>();
		for (DPRIME dprime : dprimeList) {
			if (dprime.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;
			int period = 0;
			DPRIMECalculation dprimeCalculation = new DPRIMECalculation();
			if (dprimeQueue.size() == 2) {
				dprimeQueue.poll();
			}
			dprimeQueue.add(dprime);
			Iterator<DPRIME> queueIterator = dprimeQueue.iterator();
			while (queueIterator.hasNext()) {
				DPRIME temp = queueIterator.next();
				temp.setRocFlag(true);
			}

			if (dprimeQueue.size() == 1) {
				roc = 0f;
				dprimeCalculation.setRoc(roc);
				dprimeCalculation.setToDate(dprime.getDate());
			} else {
				roc = (dprime.getValue() / ((LinkedList<DPRIME>) dprimeQueue).get(0).getValue()) - 1;
				dprimeCalculation.setRoc(roc);
				dprimeCalculation.setToDate(dprime.getDate());
			}
			dprimeCalculationModified.add(dprimeCalculation);
		}

		dprimeList = dprimeRepository.saveAll(dprimeList);
		dprimeCalculationModified = dprimeCalculationRepository.saveAll(dprimeCalculationModified);

	}

	public List<DPRIMECalculation> calculateRollAvgThreeMonth() {
		List<DPRIMECalculation> dprimeCalculationList = new ArrayList<>();
		List<DPRIME> dprimeList = dprimeRepository.findAll();
		List<DPRIMECalculation> dprimeCalculationReference = dprimeCalculationRepository.findAll();
		Queue<DPRIME> dprimeQueue = new LinkedList<>();

		for (DPRIME dprime : dprimeList) {

			if (dprimeQueue.size() == 3) {
				dprimeQueue.poll();
			}
			dprimeQueue.add(dprime);

			if (dprime.getRollAverageFlag()) {
				continue;
			}
			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			Iterator<DPRIME> queueItr = dprimeQueue.iterator();

			DPRIMECalculation tempGdpCalculation = new DPRIMECalculation();
			List<DPRIMECalculation> currentDPRIMECalculationRef = dprimeCalculationReference.stream()
					.filter(p -> p.getToDate().equals(dprime.getDate())).collect(Collectors.toList());

			if (currentDPRIMECalculationRef.size() > 0)
				tempGdpCalculation = currentDPRIMECalculationRef.get(0);

			while (queueItr.hasNext()) {
				DPRIME gdpVal = queueItr.next();
				rollingAvg += gdpVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			dprime.setRollAverageFlag(true);
			tempGdpCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			dprimeCalculationList.add(tempGdpCalculation);

		}

		dprimeCalculationReference = dprimeCalculationRepository.saveAll(dprimeCalculationList);
		dprimeList = dprimeRepository.saveAll(dprimeList);
		return dprimeCalculationReference;
	}

	public List<DPRIMECalculation> calculateRocRollingAnnualAvg() {

		List<DPRIMECalculation> dprimeCalculationReference = dprimeCalculationRepository.findAll();
		Queue<DPRIMECalculation> dprimeCalculationPriorityQueue = new LinkedList<>();
		for (DPRIMECalculation dprimeCalculation : dprimeCalculationReference) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (dprimeCalculationPriorityQueue.size() == 4) {
				dprimeCalculationPriorityQueue.poll();
			}
			dprimeCalculationPriorityQueue.add(dprimeCalculation);

			if (dprimeCalculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<DPRIMECalculation> queueIterator = dprimeCalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				DPRIMECalculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			dprimeCalculation.setRocAnnRollAvgFlag(true);
			dprimeCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(dprimeCalculationReference);
		dprimeCalculationReference = dprimeCalculationRepository.saveAll(dprimeCalculationReference);
		return dprimeCalculationReference;
	}

	public List<DPRIMECalculation> updateRocChangeSignDPRIME() {
		List<DPRIMECalculation> dprimeCalculationList = dprimeCalculationRepository.findAllByRocIsNotNull();

		if (dprimeCalculationList.isEmpty()) {
			return dprimeCalculationList;
		}
		List<DPRIMECalculation> modifiedSignList = new ArrayList<>();
		DPRIMECalculation dprimeCalculationPrev = new DPRIMECalculation();

		for (DPRIMECalculation dprimeCalculation : dprimeCalculationList) {
			DPRIMECalculation modifiedSigndffCalc = dprimeCalculation;
			if (dprimeCalculationPrev.getToDate() == null) {
				modifiedSigndffCalc.setRocChangeSign(0);
			} else {
				if (dprimeCalculationPrev.getRoc() < modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(1);
				} else if (dprimeCalculationPrev.getRoc() > modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(-1);
				} else {
					modifiedSigndffCalc.setRocChangeSign(0);
				}
			}
			modifiedSignList.add(modifiedSigndffCalc);
			dprimeCalculationPrev = modifiedSigndffCalc;
		}
		dprimeCalculationList = dprimeCalculationRepository.saveAll(modifiedSignList);
		return dprimeCalculationList;
	}
}
