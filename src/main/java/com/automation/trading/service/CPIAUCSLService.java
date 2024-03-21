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

import com.automation.trading.domain.calculation.CPIAUCSLCalculation;
import com.automation.trading.domain.fred.CPIAUCSL;
import com.automation.trading.repository.CPIAUCSLCalculationRepository;
import com.automation.trading.repository.CPIAUCSLRepository;
import com.automation.trading.utility.RestUtility;

@Service
public class CPIAUCSLService {

	@Value("${quandl.host.url}")
	private String QUANDL_HOST_URL;

	@Value("${quandl.api.key.value}")
	private String QUANDL_API_KEY_VALUE;

	@Value("${quandl.api.key.name}")
	private String QUANDL_API_KEY_NAME;

	@Value("${quandl.data.format}")
	private String QUANDL_DATA_FORMAT;

	@Autowired
	private RestUtility restUtility;

	@Autowired
	private CPIAUCSLRepository cpiaucslRepository;

	@Autowired
	private CPIAUCSLCalculationRepository cpiaucslCalculationRepository;

	public void calculateRoc() {
		List<CPIAUCSL> cpiaucslList = cpiaucslRepository.findAll();
		List<CPIAUCSLCalculation> cpiaucslCalculationList = cpiaucslCalculationRepository.findAll();
		List<CPIAUCSLCalculation> cpiaucslCalculationModified = new ArrayList<>();
		Queue<CPIAUCSL> cpiaucslQueue = new LinkedList<>();
		for (CPIAUCSL cpiaucsl : cpiaucslList) {
			if (cpiaucsl.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;
			int period = 0;
			CPIAUCSLCalculation cpiaucslCalculation = new CPIAUCSLCalculation();
			if (cpiaucslQueue.size() == 2) {
				cpiaucslQueue.poll();
			}
			cpiaucslQueue.add(cpiaucsl);
			Iterator<CPIAUCSL> queueIterator = cpiaucslQueue.iterator();
			while (queueIterator.hasNext()) {
				CPIAUCSL temp = queueIterator.next();
				temp.setRocFlag(true);
			}

			List<CPIAUCSLCalculation> currentCPIAUCSLCalculationRef = cpiaucslCalculationList.stream()
					.filter(p -> p.getToDate().equals(cpiaucsl.getDate())).collect(Collectors.toList());

			if (currentCPIAUCSLCalculationRef.size() > 0)
				cpiaucslCalculation = currentCPIAUCSLCalculationRef.get(0);

			if (cpiaucslQueue.size() == 1) {
				roc = 0f;
				cpiaucslCalculation.setRoc(roc);
				cpiaucslCalculation.setToDate(cpiaucsl.getDate());
			} else {
				roc = (cpiaucsl.getValue() / ((LinkedList<CPIAUCSL>) cpiaucslQueue).get(0).getValue()) - 1;
				cpiaucslCalculation.setRoc(roc);
				cpiaucslCalculation.setToDate(cpiaucsl.getDate());
			}
			cpiaucslCalculationModified.add(cpiaucslCalculation);
		}

		cpiaucslRepository.saveAll(cpiaucslList);
		cpiaucslCalculationRepository.saveAll(cpiaucslCalculationModified);

	}
	
	public List<CPIAUCSLCalculation> calculateRollAvgThreeMonth() {
		List<CPIAUCSLCalculation> cpiaucslCalculationList = new ArrayList<>();
		List<CPIAUCSL> cpiaucslList = cpiaucslRepository.findAll();
		List<CPIAUCSLCalculation> cpiaucslCalculationReference = cpiaucslCalculationRepository.findAll();
		Queue<CPIAUCSL> cpiaucslQueue = new LinkedList<>();

		for (CPIAUCSL cpiaucsl : cpiaucslList) {

			if (cpiaucslQueue.size() == 3) {
				cpiaucslQueue.poll();
			}
			cpiaucslQueue.add(cpiaucsl);

			if (cpiaucsl.getRollAverageFlag()) {
				continue;
			}
			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			Iterator<CPIAUCSL> queueItr = cpiaucslQueue.iterator();

			CPIAUCSLCalculation tempGdpCalculation = new CPIAUCSLCalculation();
			List<CPIAUCSLCalculation> currentCPIAUCSLCalculationRef = cpiaucslCalculationReference.stream()
					.filter(p -> p.getToDate().equals(cpiaucsl.getDate())).collect(Collectors.toList());

			if (currentCPIAUCSLCalculationRef.size() > 0)
				tempGdpCalculation = currentCPIAUCSLCalculationRef.get(0);

			while (queueItr.hasNext()) {
				CPIAUCSL gdpVal = queueItr.next();
				rollingAvg += gdpVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			cpiaucsl.setRollAverageFlag(true);
			tempGdpCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			cpiaucslCalculationList.add(tempGdpCalculation);

		}

		cpiaucslCalculationReference = cpiaucslCalculationRepository.saveAll(cpiaucslCalculationList);
		cpiaucslList = cpiaucslRepository.saveAll(cpiaucslList);
		return cpiaucslCalculationReference;
	}

	public List<CPIAUCSLCalculation> calculateRocRollingAnnualAvg() {

		List<CPIAUCSLCalculation> cpiaucslCalculationReference = cpiaucslCalculationRepository.findAll();
		Queue<CPIAUCSLCalculation> cpiaucslCalculationPriorityQueue = new LinkedList<>();
		for (CPIAUCSLCalculation cpiaucslCalculation : cpiaucslCalculationReference) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (cpiaucslCalculationPriorityQueue.size() == 4) {
				cpiaucslCalculationPriorityQueue.poll();
			}
			cpiaucslCalculationPriorityQueue.add(cpiaucslCalculation);

			if (cpiaucslCalculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<CPIAUCSLCalculation> queueIterator = cpiaucslCalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				CPIAUCSLCalculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			cpiaucslCalculation.setRocAnnRollAvgFlag(true);
			cpiaucslCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(cpiaucslCalculationReference);
		cpiaucslCalculationReference = cpiaucslCalculationRepository.saveAll(cpiaucslCalculationReference);
		return cpiaucslCalculationReference;
	}

	public List<CPIAUCSLCalculation> updateRocChangeSignDff() {
		List<CPIAUCSLCalculation> cpiaucslCalculationList = cpiaucslCalculationRepository.findAllByRocIsNotNull();
		if ((cpiaucslCalculationList.isEmpty())){
			return cpiaucslCalculationList;
		}
		List<CPIAUCSLCalculation> modifiedSignList = new ArrayList<>();
		CPIAUCSLCalculation cpiaucslCalculationPrev = new CPIAUCSLCalculation();

		for (CPIAUCSLCalculation cpiaucslCalculation : cpiaucslCalculationList) {
			CPIAUCSLCalculation modifiedSigndffCalc = cpiaucslCalculation;
			if (cpiaucslCalculationPrev.getToDate() == null) {
				modifiedSigndffCalc.setRocChangeSign(0);
			} else {
				if (cpiaucslCalculationPrev.getRoc() < modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(1);
				} else if (cpiaucslCalculationPrev.getRoc() > modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(-1);
				} else {
					modifiedSigndffCalc.setRocChangeSign(0);
				}
			}
			modifiedSignList.add(modifiedSigndffCalc);
			cpiaucslCalculationPrev = modifiedSigndffCalc;
		}
		cpiaucslCalculationList = cpiaucslCalculationRepository.saveAll(modifiedSignList);
		return cpiaucslCalculationList;
	}
	
	

}
