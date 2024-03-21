package com.automation.trading.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.automation.trading.domain.calculation.NROUCalculation;
import com.automation.trading.domain.fred.NROU;
import com.automation.trading.repository.NROUCalculationRepository;
import com.automation.trading.repository.NROURepostiory;

@Service
public class NROUService {

	@Autowired
	private NROURepostiory nrouRepository;

	@Autowired
	private NROUCalculationRepository nrouCalculationRepository;

	public void calculateRoc() {

		List<NROU> nrouList = nrouRepository.findAll();
		// List<NROUCalculation> nrouCalculationList =
		// nrouCalculationRepository.findAll();
		List<NROUCalculation> nrouCalculationModified = new ArrayList<>();
		Queue<NROU> nrouQueue = new LinkedList<>();
		for (NROU nrou : nrouList) {
			if (nrou.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;
			int period = 0;
			NROUCalculation nrouCalculation = new NROUCalculation();
			if (nrouQueue.size() == 2) {
				nrouQueue.poll();
			}
			nrouQueue.add(nrou);
			Iterator<NROU> queueIterator = nrouQueue.iterator();
			while (queueIterator.hasNext()) {
				NROU temp = queueIterator.next();
				temp.setRocFlag(true);
			}

//			List<NROUCalculation> currentNROUCalculationRef = nrouCalculationList.stream()
//					.filter(p -> p.getToDate().equals(nrou.getDate())).collect(Collectors.toList());
//
//			if (currentNROUCalculationRef.size() > 0)
//				nrouCalculation = currentNROUCalculationRef.get(0);

			if (nrouQueue.size() == 1) {
				roc = 0f;
				nrouCalculation.setRoc(roc);
				nrouCalculation.setToDate(nrou.getDate());
			} else {
				roc = (nrou.getValue() / ((LinkedList<NROU>) nrouQueue).get(0).getValue()) - 1;
				nrouCalculation.setRoc(roc);
				nrouCalculation.setToDate(nrou.getDate());
			}
			nrouCalculationModified.add(nrouCalculation);
		}

		nrouRepository.saveAll(nrouList);
		nrouCalculationRepository.saveAll(nrouCalculationModified);

	}

	public List<NROUCalculation> calculateRollAvgThreeMonth() {
		List<NROUCalculation> nrouCalculationList = new ArrayList<>();
		List<NROU> nrouList = nrouRepository.findAll();
		List<NROUCalculation> nrouCalculationReference = nrouCalculationRepository.findAll();
		Queue<NROU> nrouQueue = new LinkedList<>();

		for (NROU nrou : nrouList) {

			if (nrouQueue.size() == 3) {
				nrouQueue.poll();
			}
			nrouQueue.add(nrou);

			if (nrou.getRollAverageFlag()) {
				continue;
			}
			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			Iterator<NROU> queueItr = nrouQueue.iterator();

			NROUCalculation tempGdpCalculation = new NROUCalculation();
			List<NROUCalculation> currentNROUCalculationRef = nrouCalculationReference.stream()
					.filter(p -> p.getToDate().equals(nrou.getDate())).collect(Collectors.toList());

			if (currentNROUCalculationRef.size() > 0)
				tempGdpCalculation = currentNROUCalculationRef.get(0);

			while (queueItr.hasNext()) {
				NROU gdpVal = queueItr.next();
				rollingAvg += gdpVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			nrou.setRollAverageFlag(true);
			tempGdpCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			nrouCalculationList.add(tempGdpCalculation);

		}

		nrouCalculationReference = nrouCalculationRepository.saveAll(nrouCalculationList);
		nrouList = nrouRepository.saveAll(nrouList);
		return nrouCalculationReference;
	}

	public List<NROUCalculation> calculateRocRollingAnnualAvg() {

		List<NROUCalculation> nrouCalculationReference = nrouCalculationRepository.findAll();
		Queue<NROUCalculation> nrouCalculationPriorityQueue = new LinkedList<>();
		for (NROUCalculation nrouCalculation : nrouCalculationReference) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (nrouCalculationPriorityQueue.size() == 4) {
				nrouCalculationPriorityQueue.poll();
			}
			nrouCalculationPriorityQueue.add(nrouCalculation);

			if (nrouCalculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<NROUCalculation> queueIterator = nrouCalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				NROUCalculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			nrouCalculation.setRocAnnRollAvgFlag(true);
			nrouCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(nrouCalculationReference);
		nrouCalculationReference = nrouCalculationRepository.saveAll(nrouCalculationReference);
		return nrouCalculationReference;
	}

	public List<NROUCalculation> updateRocChangeSign() {
		List<NROUCalculation> nrouCalculationList = nrouCalculationRepository.findAllByRocIsNotNull();
		if (nrouCalculationList.isEmpty()) {
			return nrouCalculationList;
		}
		List<NROUCalculation> modifiedSignList = new ArrayList<>();
		NROUCalculation nrouCalculationPrev = new NROUCalculation();

		for (NROUCalculation nrouCalculation : nrouCalculationList) {
			NROUCalculation modifiedSigndffCalc = nrouCalculation;
			if (nrouCalculationPrev.getToDate() == null) {
				modifiedSigndffCalc.setRocChangeSign(0);
			} else {
				if (nrouCalculationPrev.getRoc() < modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(1);
				} else if (nrouCalculationPrev.getRoc() > modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(-1);
				} else {
					modifiedSigndffCalc.setRocChangeSign(0);
				}
			}
			modifiedSignList.add(modifiedSigndffCalc);
			nrouCalculationPrev = modifiedSigndffCalc;
		}
		nrouCalculationList = nrouCalculationRepository.saveAll(modifiedSignList);
		return nrouCalculationList;
	}

}
