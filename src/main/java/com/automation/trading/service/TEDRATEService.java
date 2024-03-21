package com.automation.trading.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.automation.trading.domain.calculation.TEDRATECalculation;
import com.automation.trading.domain.fred.interestrates.TEDRATE;
import com.automation.trading.repository.TEDRATECalculationRepository;
import com.automation.trading.repository.TEDRATERepository;

@Service
public class TEDRATEService {

	@Autowired
	private TEDRATERepository tedrateRepository;

	@Autowired
	private TEDRATECalculationRepository tedrateCalculationRepository;

	public void calculateRoc() {
		List<TEDRATE> tedrateList = tedrateRepository.findAll();
		List<TEDRATECalculation> tedrateCalculationList = tedrateCalculationRepository.findAll();
		List<TEDRATECalculation> tedrateCalculationModified = new ArrayList<>();
		Queue<TEDRATE> tedrateQueue = new LinkedList<>();
		for (TEDRATE tedrate : tedrateList) {
			if (tedrate.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;
			int period = 0;
			TEDRATECalculation baseCalculation = new TEDRATECalculation();
			if (tedrateQueue.size() == 2) {
				tedrateQueue.poll();
			}
			tedrateQueue.add(tedrate);
			Iterator<TEDRATE> queueIterator = tedrateQueue.iterator();
			while (queueIterator.hasNext()) {
				TEDRATE temp = queueIterator.next();
				temp.setRocFlag(true);
			}

			List<TEDRATECalculation> currentTEDRATECalculationRef = tedrateCalculationList.stream()
					.filter(p -> p.getToDate().equals(tedrate.getDate())).collect(Collectors.toList());

			if (currentTEDRATECalculationRef.size() > 0)
				baseCalculation = currentTEDRATECalculationRef.get(0);

			if (tedrateQueue.size() == 1) {
				roc = 0f;
				baseCalculation.setRoc(roc);
				baseCalculation.setToDate(tedrate.getDate());
			} else {
				roc = (tedrate.getValue() / ((LinkedList<TEDRATE>) tedrateQueue).get(0).getValue()) - 1;
				baseCalculation.setRoc(roc);
				baseCalculation.setToDate(tedrate.getDate());
			}
			tedrateCalculationModified.add(baseCalculation);
		}

		tedrateRepository.saveAll(tedrateList);
		tedrateCalculationRepository.saveAll(tedrateCalculationModified);

	}

	public List<TEDRATECalculation> calculateRollAvgThreeMonth() {
		List<TEDRATECalculation> tedrateCalculationList = new ArrayList<>();
		List<TEDRATE> tedrateList = tedrateRepository.findAll();
		List<TEDRATECalculation> tedrateCalculationReference = tedrateCalculationRepository.findAll();
		Queue<TEDRATE> tedrateQueue = new LinkedList<>();

		for (TEDRATE tedrate : tedrateList) {

			if (tedrateQueue.size() == 3) {
				tedrateQueue.poll();
			}
			tedrateQueue.add(tedrate);

			if (tedrate.getRollAverageFlag()) {
				continue;
			}
			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			Iterator<TEDRATE> queueItr = tedrateQueue.iterator();

			TEDRATECalculation tempGdpCalculation = new TEDRATECalculation();
			List<TEDRATECalculation> currentTEDRATECalculationRef = tedrateCalculationReference.stream()
					.filter(p -> p.getToDate().equals(tedrate.getDate())).collect(Collectors.toList());

			if (currentTEDRATECalculationRef.size() > 0)
				tempGdpCalculation = currentTEDRATECalculationRef.get(0);

			while (queueItr.hasNext()) {
				TEDRATE gdpVal = queueItr.next();
				rollingAvg += gdpVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			tedrate.setRollAverageFlag(true);
			tempGdpCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			tedrateCalculationList.add(tempGdpCalculation);

		}

		tedrateCalculationReference = tedrateCalculationRepository.saveAll(tedrateCalculationList);
		tedrateList = tedrateRepository.saveAll(tedrateList);
		return tedrateCalculationReference;
	}

	public List<TEDRATECalculation> calculateRocRollingAnnualAvg() {

		List<TEDRATECalculation> tedrateCalculationReference = tedrateCalculationRepository.findAll();
		Queue<TEDRATECalculation> tedrateCalculationPriorityQueue = new LinkedList<>();
		for (TEDRATECalculation tedrateCalculation : tedrateCalculationReference) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (tedrateCalculationPriorityQueue.size() == 4) {
				tedrateCalculationPriorityQueue.poll();
			}
			tedrateCalculationPriorityQueue.add(tedrateCalculation);

			if (tedrateCalculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<TEDRATECalculation> queueIterator = tedrateCalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				TEDRATECalculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			tedrateCalculation.setRocAnnRollAvgFlag(true);
			tedrateCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(tedrateCalculationReference);
		tedrateCalculationReference = tedrateCalculationRepository.saveAll(tedrateCalculationReference);
		return tedrateCalculationReference;
	}

	public List<TEDRATECalculation> updateRocChangeSignTEDRATE() {

		List<TEDRATECalculation> tedrateCalculationList = tedrateCalculationRepository.findAllByRocIsNotNull();
		if (tedrateCalculationList.isEmpty()) {
			return tedrateCalculationList;
		}

		List<TEDRATECalculation> modifiedSignList = new ArrayList<>();
		TEDRATECalculation tedrateCalculationPrev = new TEDRATECalculation();

		for (TEDRATECalculation tedrateCalculation : tedrateCalculationList) {
			TEDRATECalculation modifiedSigndffCalc = tedrateCalculation;
			if (tedrateCalculationPrev.getToDate() == null) {
				modifiedSigndffCalc.setRocChangeSign(0);
			} else {
				if (tedrateCalculationPrev.getRoc() < modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(1);
				} else if (tedrateCalculationPrev.getRoc() > modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(-1);
				} else {
					modifiedSigndffCalc.setRocChangeSign(0);
				}
			}
			modifiedSignList.add(modifiedSigndffCalc);
			tedrateCalculationPrev = modifiedSigndffCalc;
		}
		tedrateCalculationList = tedrateCalculationRepository.saveAll(modifiedSignList);
		return tedrateCalculationList;
	}

}
