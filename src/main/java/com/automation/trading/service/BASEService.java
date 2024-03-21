package com.automation.trading.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.automation.trading.domain.calculation.BASECalculation;
import com.automation.trading.domain.fred.BASE;
import com.automation.trading.repository.BASECalculationRepository;
import com.automation.trading.repository.BASERepository;

@Service
public class BASEService {

	@Autowired
	private BASERepository baseRepository;

	@Autowired
	private BASECalculationRepository baseCalculationRepository;

	public void calculateRoc() {

		List<BASE> baseList = baseRepository.findAll();
		List<BASECalculation> baseCalculationList = baseCalculationRepository.findAll();
		List<BASECalculation> baseCalculationModified = new ArrayList<>();
		Queue<BASE> baseQueue = new LinkedList<>();
		for (BASE base : baseList) {
			if (Boolean.TRUE.equals(base.getRocFlag())) {
				continue;
			}
			Float roc = Float.valueOf(0);
			BASECalculation baseCalculation = new BASECalculation();
			if (baseQueue.size() == 2) {
				baseQueue.poll();
			}
			baseQueue.add(base);
			Iterator<BASE> queueIterator = baseQueue.iterator();
			while (queueIterator.hasNext()) {
				BASE temp = queueIterator.next();
				temp.setRocFlag(true);
			}

			List<BASECalculation> currentBASECalculationRef = baseCalculationList.stream()
					.filter(p -> p.getToDate().equals(base.getDate())).collect(Collectors.toList());

			if (currentBASECalculationRef.isEmpty())
				baseCalculation = currentBASECalculationRef.get(0);

			if (baseQueue.size() == 1) {
				roc = 0f;
				baseCalculation.setRoc(roc);
				baseCalculation.setToDate(base.getDate());
			} else {
				roc = (base.getValue() / ((LinkedList<BASE>) baseQueue).get(0).getValue()) - 1;
				baseCalculation.setRoc(roc);
				baseCalculation.setToDate(base.getDate());
			}
			baseCalculationModified.add(baseCalculation);
		}

		baseRepository.saveAll(baseList);
		baseCalculationRepository.saveAll(baseCalculationModified);

	}

	public List<BASECalculation> calculateRollAvgThreeMonth() {
		List<BASECalculation> baseCalculationList = new ArrayList<>();
		List<BASE> baseList = baseRepository.findAll();
		List<BASECalculation> baseCalculationReference = baseCalculationRepository.findAll();
		Queue<BASE> baseQueue = new LinkedList<>();

		for (BASE base : baseList) {

			if (baseQueue.size() == 3) {
				baseQueue.poll();
			}
			baseQueue.add(base);

			if (Boolean.TRUE.equals(base.getRollAverageFlag())) {
				continue;
			}
			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			Iterator<BASE> queueItr = baseQueue.iterator();

			BASECalculation tempGdpCalculation = new BASECalculation();
			List<BASECalculation> currentBASECalculationRef = baseCalculationReference.stream()
					.filter(p -> p.getToDate().equals(base.getDate())).collect(Collectors.toList());

			if (currentBASECalculationRef.isEmpty())
				tempGdpCalculation = currentBASECalculationRef.get(0);

			while (queueItr.hasNext()) {
				BASE gdpVal = queueItr.next();
				rollingAvg += gdpVal.getValue();
				period++;
			}

			if (period > 0) {
				rollingAvgThreeMon = rollingAvg / period;
			}

			base.setRollAverageFlag(true);
			tempGdpCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			baseCalculationList.add(tempGdpCalculation);

		}

		baseCalculationReference = baseCalculationRepository.saveAll(baseCalculationList);
		baseList = baseRepository.saveAll(baseList);
		return baseCalculationReference;
	}

	public List<BASECalculation> calculateRocRollingAnnualAvg() {

		List<BASECalculation> baseCalculationReference = baseCalculationRepository.findAll();
		Queue<BASECalculation> baseCalculationPriorityQueue = new LinkedList<>();
		for (BASECalculation baseCalculation : baseCalculationReference) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (baseCalculationPriorityQueue.size() == 4) {
				baseCalculationPriorityQueue.poll();
			}
			baseCalculationPriorityQueue.add(baseCalculation);

			if (Boolean.TRUE.equals(baseCalculation.getRocAnnRollAvgFlag())) {
				continue;
			}
			Iterator<BASECalculation> queueIterator = baseCalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				BASECalculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			baseCalculation.setRocAnnRollAvgFlag(true);
			baseCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}

		baseCalculationReference = baseCalculationRepository.saveAll(baseCalculationReference);
		return baseCalculationReference;
	}

	public List<BASECalculation> updateRocChangeSignDff() {

		List<BASECalculation> baseCalculationList = baseCalculationRepository.findAllByRocIsNotNull();
		if (baseCalculationList.isEmpty()) {
			return baseCalculationList;
		}
		List<BASECalculation> modifiedSignList = new ArrayList<>();
		BASECalculation baseCalculationPrev = new BASECalculation();

		for (BASECalculation baseCalculation : baseCalculationList) {
			BASECalculation modifiedSigndffCalc = baseCalculation;
			if (baseCalculationPrev.getToDate() == null) {
				modifiedSigndffCalc.setRocChangeSign(0);
			} else {
				if (baseCalculationPrev.getRoc() < modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(1);
				} else if (baseCalculationPrev.getRoc() > modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(-1);
				} else {
					modifiedSigndffCalc.setRocChangeSign(0);
				}
			}
			modifiedSignList.add(modifiedSigndffCalc);
			baseCalculationPrev = modifiedSigndffCalc;
		}
		baseCalculationList = baseCalculationRepository.saveAll(modifiedSignList);
		return baseCalculationList;
	}

}
