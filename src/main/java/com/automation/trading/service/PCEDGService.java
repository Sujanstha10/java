package com.automation.trading.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.automation.trading.domain.calculation.PCEDGCalculation;
import com.automation.trading.domain.fred.PCEDG;
import com.automation.trading.repository.PCEDGCalculationRepository;
import com.automation.trading.repository.PCEDGRepository;

@Service
public class PCEDGService {
	
	@Autowired
	private PCEDGRepository pcedgRepository;

	@Autowired
	private PCEDGCalculationRepository pcedgCalculationRepository;

	public void calculateRoc() {

		List<PCEDG> pcedgList = pcedgRepository.findAll();
		// List<PCEDGCalculation> pcedgCalculationList =
		// pcedgCalculationRepository.findAll();
		List<PCEDGCalculation> pcedgCalculationModified = new ArrayList<>();
		Queue<PCEDG> pcedgQueue = new LinkedList<>();
		for (PCEDG pcedg : pcedgList) {
			if (pcedg.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;
			int period = 0;
			PCEDGCalculation pcedgCalculation = new PCEDGCalculation();
			if (pcedgQueue.size() == 2) {
				pcedgQueue.poll();
			}
			pcedgQueue.add(pcedg);
			Iterator<PCEDG> queueIterator = pcedgQueue.iterator();
			while (queueIterator.hasNext()) {
				PCEDG temp = queueIterator.next();
				temp.setRocFlag(true);
			}

//			List<PCEDGCalculation> currentPCEDGCalculationRef = pcedgCalculationList.stream()
//					.filter(p -> p.getToDate().equals(pcedg.getDate())).collect(Collectors.toList());
//
//			if (currentPCEDGCalculationRef.size() > 0)
//				pcedgCalculation = currentPCEDGCalculationRef.get(0);

			if (pcedgQueue.size() == 1) {
				roc = 0f;
				pcedgCalculation.setRoc(roc);
				pcedgCalculation.setToDate(pcedg.getDate());
			} else {
				roc = (pcedg.getValue() / ((LinkedList<PCEDG>) pcedgQueue).get(0).getValue()) - 1;
				pcedgCalculation.setRoc(roc);
				pcedgCalculation.setToDate(pcedg.getDate());
			}
			pcedgCalculationModified.add(pcedgCalculation);
		}

		pcedgRepository.saveAll(pcedgList);
		pcedgCalculationRepository.saveAll(pcedgCalculationModified);

	}

	public List<PCEDGCalculation> calculateRollAvgThreeMonth() {
		List<PCEDGCalculation> pcedgCalculationList = new ArrayList<>();
		List<PCEDG> pcedgList = pcedgRepository.findAll();
		List<PCEDGCalculation> pcedgCalculationReference = pcedgCalculationRepository.findAll();
		Queue<PCEDG> pcedgQueue = new LinkedList<>();

		for (PCEDG pcedg : pcedgList) {

			if (pcedgQueue.size() == 3) {
				pcedgQueue.poll();
			}
			pcedgQueue.add(pcedg);

			if (pcedg.getRollAverageFlag()) {
				continue;
			}
			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			Iterator<PCEDG> queueItr = pcedgQueue.iterator();

			PCEDGCalculation tempGdpCalculation = new PCEDGCalculation();
			List<PCEDGCalculation> currentPCEDGCalculationRef = pcedgCalculationReference.stream()
					.filter(p -> p.getToDate().equals(pcedg.getDate())).collect(Collectors.toList());

			if (currentPCEDGCalculationRef.size() > 0)
				tempGdpCalculation = currentPCEDGCalculationRef.get(0);

			while (queueItr.hasNext()) {
				PCEDG gdpVal = queueItr.next();
				rollingAvg += gdpVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			pcedg.setRollAverageFlag(true);
			tempGdpCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			pcedgCalculationList.add(tempGdpCalculation);

		}

		pcedgCalculationReference = pcedgCalculationRepository.saveAll(pcedgCalculationList);
		pcedgList = pcedgRepository.saveAll(pcedgList);
		return pcedgCalculationReference;
	}

	public List<PCEDGCalculation> calculateRocRollingAnnualAvg() {

		List<PCEDGCalculation> pcedgCalculationReference = pcedgCalculationRepository.findAll();
		Queue<PCEDGCalculation> pcedgCalculationPriorityQueue = new LinkedList<>();
		for (PCEDGCalculation pcedgCalculation : pcedgCalculationReference) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (pcedgCalculationPriorityQueue.size() == 4) {
				pcedgCalculationPriorityQueue.poll();
			}
			pcedgCalculationPriorityQueue.add(pcedgCalculation);

			if (pcedgCalculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<PCEDGCalculation> queueIterator = pcedgCalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				PCEDGCalculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			pcedgCalculation.setRocAnnRollAvgFlag(true);
			pcedgCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(pcedgCalculationReference);
		pcedgCalculationReference = pcedgCalculationRepository.saveAll(pcedgCalculationReference);
		return pcedgCalculationReference;
	}

	public List<PCEDGCalculation> updateRocChangeSign() {
		List<PCEDGCalculation> pcedgCalculationList = pcedgCalculationRepository.findAllByRocIsNotNull();
		if (pcedgCalculationList.isEmpty()) {
			return pcedgCalculationList;
		}
		List<PCEDGCalculation> modifiedSignList = new ArrayList<>();
		PCEDGCalculation pcedgCalculationPrev = new PCEDGCalculation();

		for (PCEDGCalculation pcedgCalculation : pcedgCalculationList) {
			PCEDGCalculation modifiedSigndffCalc = pcedgCalculation;
			if (pcedgCalculationPrev.getToDate() == null) {
				modifiedSigndffCalc.setRocChangeSign(0);
			} else {
				if (pcedgCalculationPrev.getRoc() < modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(1);
				} else if (pcedgCalculationPrev.getRoc() > modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(-1);
				} else {
					modifiedSigndffCalc.setRocChangeSign(0);
				}
			}
			modifiedSignList.add(modifiedSigndffCalc);
			pcedgCalculationPrev = modifiedSigndffCalc;
		}
		pcedgCalculationList = pcedgCalculationRepository.saveAll(modifiedSignList);
		return pcedgCalculationList;
	}

}
