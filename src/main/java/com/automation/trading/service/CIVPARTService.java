package com.automation.trading.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.automation.trading.domain.calculation.CIVPARTCalculation;
import com.automation.trading.domain.fred.CIVPART;
import com.automation.trading.repository.CIVPARTCalculationRepository;
import com.automation.trading.repository.CIVPARTRepository;

@Service
public class CIVPARTService {

	@Value("${quandl.host.url}")
	private String QUANDL_HOST_URL;

	@Value("${quandl.api.key.value}")
	private String QUANDL_API_KEY_VALUE;

	@Value("${quandl.api.key.name}")
	private String QUANDL_API_KEY_NAME;

	@Value("${quandl.data.format}")
	private String QUANDL_DATA_FORMAT;

	@Autowired
	private CIVPARTRepository civpartRepository;

	@Autowired
	private CIVPARTCalculationRepository civpartCalculationRepository;

	public void calculateRoc() {
		List<CIVPART> civpartList = civpartRepository.findAll();
		List<CIVPARTCalculation> civpartCalculationList = civpartCalculationRepository.findAll();
		List<CIVPARTCalculation> civpartCalculationModified = new ArrayList<>();
		Queue<CIVPART> civpartQueue = new LinkedList<>();
		for (CIVPART civpart : civpartList) {
			if (civpart.getRocFlag()) {
				continue;
			}
			Float roc = 0.0F;

			CIVPARTCalculation civpartCalculation = new CIVPARTCalculation();
			if (civpartQueue.size() == 2) {
				civpartQueue.poll();
			}
			civpartQueue.add(civpart);
			Iterator<CIVPART> queueIterator = civpartQueue.iterator();
			while (queueIterator.hasNext()) {
				CIVPART temp = queueIterator.next();
				temp.setRocFlag(true);
			}

			List<CIVPARTCalculation> currentCIVPARTCalculationRef = civpartCalculationList.stream()
					.filter(p -> p.getToDate().equals(civpart.getDate())).collect(Collectors.toList());

			if (currentCIVPARTCalculationRef.size() > 0)
				civpartCalculation = currentCIVPARTCalculationRef.get(0);

			if (civpartQueue.size() == 1) {
				roc = 0f;
				civpartCalculation.setRoc(roc);
				civpartCalculation.setToDate(civpart.getDate());
			} else {
				roc = (civpart.getValue() / ((LinkedList<CIVPART>) civpartQueue).get(0).getValue()) - 1;
				civpartCalculation.setRoc(roc);
				civpartCalculation.setToDate(civpart.getDate());
			}
			civpartCalculationModified.add(civpartCalculation);
		}

		civpartRepository.saveAll(civpartList);
		civpartCalculationRepository.saveAll(civpartCalculationModified);

	}

	public List<CIVPARTCalculation> calculateRollAvgThreeMonth() {
		List<CIVPARTCalculation> civpartCalculationList = new ArrayList<>();
		List<CIVPART> civpartList = civpartRepository.findAll();
		List<CIVPARTCalculation> civpartCalculationReference = civpartCalculationRepository.findAll();
		Queue<CIVPART> civpartQueue = new LinkedList<>();

		for (CIVPART civpart : civpartList) {

			if (civpartQueue.size() == 3) {
				civpartQueue.poll();
			}
			civpartQueue.add(civpart);

			if (civpart.getRollAverageFlag()) {
				continue;
			}
			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			Iterator<CIVPART> queueItr = civpartQueue.iterator();

			CIVPARTCalculation tempGdpCalculation = new CIVPARTCalculation();
			List<CIVPARTCalculation> currentCIVPARTCalculationRef = civpartCalculationReference.stream()
					.filter(p -> p.getToDate().equals(civpart.getDate())).collect(Collectors.toList());

			if (currentCIVPARTCalculationRef.size() > 0)
				tempGdpCalculation = currentCIVPARTCalculationRef.get(0);

			while (queueItr.hasNext()) {
				CIVPART gdpVal = queueItr.next();
				rollingAvg += gdpVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			civpart.setRollAverageFlag(true);
			tempGdpCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			civpartCalculationList.add(tempGdpCalculation);

		}

		civpartCalculationReference = civpartCalculationRepository.saveAll(civpartCalculationList);
		civpartList = civpartRepository.saveAll(civpartList);
		return civpartCalculationReference;
	}

	public List<CIVPARTCalculation> calculateRocRollingAnnualAvg() {

		List<CIVPARTCalculation> civpartCalculationReference = civpartCalculationRepository.findAll();
		Queue<CIVPARTCalculation> civpartCalculationPriorityQueue = new LinkedList<>();
		for (CIVPARTCalculation civpartCalculation : civpartCalculationReference) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (civpartCalculationPriorityQueue.size() == 4) {
				civpartCalculationPriorityQueue.poll();
			}
			civpartCalculationPriorityQueue.add(civpartCalculation);

			if (civpartCalculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<CIVPARTCalculation> queueIterator = civpartCalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				CIVPARTCalculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			civpartCalculation.setRocAnnRollAvgFlag(true);
			civpartCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(civpartCalculationReference);
		civpartCalculationReference = civpartCalculationRepository.saveAll(civpartCalculationReference);
		return civpartCalculationReference;
	}

	public List<CIVPARTCalculation> updateRocChangeSignDff() {
		List<CIVPARTCalculation> civpartCalculationList = civpartCalculationRepository.findAllByRocIsNotNull();
		if ((civpartCalculationList.isEmpty())) {
			return civpartCalculationList;
		}
		List<CIVPARTCalculation> modifiedSignList = new ArrayList<>();
		CIVPARTCalculation civpartCalculationPrev = new CIVPARTCalculation();

		for (CIVPARTCalculation civpartCalculation : civpartCalculationList) {
			CIVPARTCalculation modifiedSigndffCalc = civpartCalculation;
			if (civpartCalculationPrev.getToDate() == null) {
				modifiedSigndffCalc.setRocChangeSign(0);
			} else {
				if (civpartCalculationPrev.getRoc() < modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(1);
				} else if (civpartCalculationPrev.getRoc() > modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(-1);
				} else {
					modifiedSigndffCalc.setRocChangeSign(0);
				}
			}
			modifiedSignList.add(modifiedSigndffCalc);
			civpartCalculationPrev = modifiedSigndffCalc;
		}
		civpartCalculationList = civpartCalculationRepository.saveAll(modifiedSignList);
		return civpartCalculationList;
	}

}
