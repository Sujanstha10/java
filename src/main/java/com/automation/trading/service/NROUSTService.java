package com.automation.trading.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.automation.trading.domain.calculation.NROUSTCalculation;
import com.automation.trading.domain.fred.NROUST;
import com.automation.trading.repository.NROUSTCalculationRepository;
import com.automation.trading.repository.NROUSTRepository;

@Service
public class NROUSTService {

	@Autowired
	private NROUSTRepository nroustRepository;

	@Autowired
	private NROUSTCalculationRepository nroustCalculationRepository;

	public void calculateRoc() {
		List<NROUST> nroustList = nroustRepository.findAll();
		List<NROUSTCalculation> nroustCalculationList = nroustCalculationRepository.findAll();
		List<NROUSTCalculation> nroustCalculationModified = new ArrayList<>();
		Queue<NROUST> nroustQueue = new LinkedList<>();
		for (NROUST nroust : nroustList) {
			if (nroust.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;
			int period = 0;
			NROUSTCalculation baseCalculation = new NROUSTCalculation();
			if (nroustQueue.size() == 2) {
				nroustQueue.poll();
			}
			nroustQueue.add(nroust);
			Iterator<NROUST> queueIterator = nroustQueue.iterator();
			while (queueIterator.hasNext()) {
				NROUST temp = queueIterator.next();
				temp.setRocFlag(true);
			}

			List<NROUSTCalculation> currentNROUSTCalculationRef = nroustCalculationList.stream()
					.filter(p -> p.getToDate().equals(nroust.getDate())).collect(Collectors.toList());

			if (currentNROUSTCalculationRef.size() > 0)
				baseCalculation = currentNROUSTCalculationRef.get(0);

			if (nroustQueue.size() == 1) {
				roc = 0f;
				baseCalculation.setRoc(roc);
				baseCalculation.setToDate(nroust.getDate());
			} else {
				roc = (nroust.getValue() / ((LinkedList<NROUST>) nroustQueue).get(0).getValue()) - 1;
				baseCalculation.setRoc(roc);
				baseCalculation.setToDate(nroust.getDate());
			}
			nroustCalculationModified.add(baseCalculation);
		}

		nroustRepository.saveAll(nroustList);
		nroustCalculationRepository.saveAll(nroustCalculationModified);

	}

	public List<NROUSTCalculation> calculateRollAvgThreeMonth() {
		List<NROUSTCalculation> nroustCalculationList = new ArrayList<>();
		List<NROUST> nroustList = nroustRepository.findAll();
		List<NROUSTCalculation> nroustCalculationReference = nroustCalculationRepository.findAll();
		Queue<NROUST> nroustQueue = new LinkedList<>();

		for (NROUST nroust : nroustList) {

			if (nroustQueue.size() == 3) {
				nroustQueue.poll();
			}
			nroustQueue.add(nroust);

			if (nroust.getRollAverageFlag()) {
				continue;
			}
			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			Iterator<NROUST> queueItr = nroustQueue.iterator();

			NROUSTCalculation tempGdpCalculation = new NROUSTCalculation();
			List<NROUSTCalculation> currentNROUSTCalculationRef = nroustCalculationReference.stream()
					.filter(p -> p.getToDate().equals(nroust.getDate())).collect(Collectors.toList());

			if (currentNROUSTCalculationRef.size() > 0)
				tempGdpCalculation = currentNROUSTCalculationRef.get(0);

			while (queueItr.hasNext()) {
				NROUST gdpVal = queueItr.next();
				rollingAvg += gdpVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			nroust.setRollAverageFlag(true);
			tempGdpCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			nroustCalculationList.add(tempGdpCalculation);

		}

		nroustCalculationReference = nroustCalculationRepository.saveAll(nroustCalculationList);
		nroustList = nroustRepository.saveAll(nroustList);
		return nroustCalculationReference;
	}

	public List<NROUSTCalculation> calculateRocRollingAnnualAvg() {

		List<NROUSTCalculation> nroustCalculationReference = nroustCalculationRepository.findAll();
		Queue<NROUSTCalculation> nroustCalculationPriorityQueue = new LinkedList<>();
		for (NROUSTCalculation nroustCalculation : nroustCalculationReference) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (nroustCalculationPriorityQueue.size() == 4) {
				nroustCalculationPriorityQueue.poll();
			}
			nroustCalculationPriorityQueue.add(nroustCalculation);

			if (nroustCalculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<NROUSTCalculation> queueIterator = nroustCalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				NROUSTCalculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			nroustCalculation.setRocAnnRollAvgFlag(true);
			nroustCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(nroustCalculationReference);
		nroustCalculationReference = nroustCalculationRepository.saveAll(nroustCalculationReference);
		return nroustCalculationReference;
	}

	public List<NROUSTCalculation> updateRocChangeSignNROUST() {
		List<NROUSTCalculation> nroustCalculationList = nroustCalculationRepository.findAll();
		List<NROUSTCalculation> modifiedSignList = new ArrayList<>();
		NROUSTCalculation nroustCalculationPrev = new NROUSTCalculation();

		for (NROUSTCalculation nroustCalculation : nroustCalculationList) {
			NROUSTCalculation modifiedSigndffCalc = nroustCalculation;
			if (nroustCalculationPrev.getToDate() == null) {
				modifiedSigndffCalc.setRocChangeSign(0);
			} else {
				if (nroustCalculationPrev.getRoc() < modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(1);
				} else if (nroustCalculationPrev.getRoc() > modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(-1);
				} else {
					modifiedSigndffCalc.setRocChangeSign(0);
				}
			}
			modifiedSignList.add(modifiedSigndffCalc);
			nroustCalculationPrev = modifiedSigndffCalc;
		}
		nroustCalculationList = nroustCalculationRepository.saveAll(modifiedSignList);
		return nroustCalculationList;
	}

}
