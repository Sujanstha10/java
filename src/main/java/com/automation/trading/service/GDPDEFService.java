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

import com.automation.trading.domain.calculation.GDPDEFCalculation;
import com.automation.trading.domain.fred.GDPDEF;
import com.automation.trading.repository.GDPDEFCalculationRepository;
import com.automation.trading.repository.GDPDEFRepository;

@Service
public class GDPDEFService {

	@Value("${quandl.host.url}")
	private String QUANDL_HOST_URL;

	@Value("${quandl.api.key.value}")
	private String QUANDL_API_KEY_VALUE;

	@Value("${quandl.api.key.name}")
	private String QUANDL_API_KEY_NAME;

	@Value("${quandl.data.format}")
	private String QUANDL_DATA_FORMAT;

	@Autowired
	private GDPDEFRepository gdpdefRepository;

	@Autowired
	private GDPDEFCalculationRepository gdpdefCalculationRepository;

	public void calculateRoc() {
		List<GDPDEF> gdpdefList = gdpdefRepository.findAll();
		List<GDPDEFCalculation> gdpdefCalculationList = gdpdefCalculationRepository.findAll();
		List<GDPDEFCalculation> gdpdefCalculationModified = new ArrayList<>();
		Queue<GDPDEF> gdpdefQueue = new LinkedList<>();
		for (GDPDEF gdpdef : gdpdefList) {
			if (gdpdef.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;
			int period = 0;
			GDPDEFCalculation gdpdefCalculation = new GDPDEFCalculation();
			if (gdpdefQueue.size() == 2) {
				gdpdefQueue.poll();
			}
			gdpdefQueue.add(gdpdef);
			Iterator<GDPDEF> queueIterator = gdpdefQueue.iterator();
			while (queueIterator.hasNext()) {
				GDPDEF temp = queueIterator.next();
				temp.setRocFlag(true);
			}

			List<GDPDEFCalculation> currentGDPDEFCalculationRef = gdpdefCalculationList.stream()
					.filter(p -> p.getToDate().equals(gdpdef.getDate())).collect(Collectors.toList());

			if (currentGDPDEFCalculationRef.size() > 0)
				gdpdefCalculation = currentGDPDEFCalculationRef.get(0);

			if (gdpdefQueue.size() == 1) {
				roc = 0f;
				gdpdefCalculation.setRoc(roc);
				gdpdefCalculation.setToDate(gdpdef.getDate());
			} else {
				roc = (gdpdef.getValue() / ((LinkedList<GDPDEF>) gdpdefQueue).get(0).getValue()) - 1;
				gdpdefCalculation.setRoc(roc);
				gdpdefCalculation.setToDate(gdpdef.getDate());
			}
			gdpdefCalculationModified.add(gdpdefCalculation);
		}

		gdpdefRepository.saveAll(gdpdefList);
		gdpdefCalculationRepository.saveAll(gdpdefCalculationModified);

	}

	public List<GDPDEFCalculation> calculateRollAvgThreeMonth() {
		List<GDPDEFCalculation> gdpdefCalculationList = new ArrayList<>();
		List<GDPDEF> gdpdefList = gdpdefRepository.findAll();
		List<GDPDEFCalculation> gdpdefCalculationReference = gdpdefCalculationRepository.findAll();
		Queue<GDPDEF> gdpdefQueue = new LinkedList<>();

		for (GDPDEF gdpdef : gdpdefList) {

			if (gdpdefQueue.size() == 3) {
				gdpdefQueue.poll();
			}
			gdpdefQueue.add(gdpdef);

			if (gdpdef.getRollAverageFlag()) {
				continue;
			}
			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			Iterator<GDPDEF> queueItr = gdpdefQueue.iterator();

			GDPDEFCalculation tempGdpCalculation = new GDPDEFCalculation();
			List<GDPDEFCalculation> currentGDPDEFCalculationRef = gdpdefCalculationReference.stream()
					.filter(p -> p.getToDate().equals(gdpdef.getDate())).collect(Collectors.toList());

			if (currentGDPDEFCalculationRef.size() > 0)
				tempGdpCalculation = currentGDPDEFCalculationRef.get(0);

			while (queueItr.hasNext()) {
				GDPDEF gdpVal = queueItr.next();
				rollingAvg += gdpVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			gdpdef.setRollAverageFlag(true);
			tempGdpCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			gdpdefCalculationList.add(tempGdpCalculation);

		}

		gdpdefCalculationReference = gdpdefCalculationRepository.saveAll(gdpdefCalculationList);
		gdpdefList = gdpdefRepository.saveAll(gdpdefList);
		return gdpdefCalculationReference;
	}

	public List<GDPDEFCalculation> calculateRocRollingAnnualAvg() {

		List<GDPDEFCalculation> gdpdefCalculationReference = gdpdefCalculationRepository.findAll();
		Queue<GDPDEFCalculation> gdpdefCalculationPriorityQueue = new LinkedList<>();
		for (GDPDEFCalculation gdpdefCalculation : gdpdefCalculationReference) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (gdpdefCalculationPriorityQueue.size() == 4) {
				gdpdefCalculationPriorityQueue.poll();
			}
			gdpdefCalculationPriorityQueue.add(gdpdefCalculation);

			if (gdpdefCalculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<GDPDEFCalculation> queueIterator = gdpdefCalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				GDPDEFCalculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			gdpdefCalculation.setRocAnnRollAvgFlag(true);
			gdpdefCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(gdpdefCalculationReference);
		gdpdefCalculationReference = gdpdefCalculationRepository.saveAll(gdpdefCalculationReference);
		return gdpdefCalculationReference;
	}

	public List<GDPDEFCalculation> updateRocChangeSign() {
		List<GDPDEFCalculation> gdpdefCalculationList = gdpdefCalculationRepository.findAllByRocIsNotNull();
		if(gdpdefCalculationList.isEmpty()){
			return gdpdefCalculationList;
		}
		List<GDPDEFCalculation> modifiedSignList = new ArrayList<>();
		GDPDEFCalculation gdpdefCalculationPrev = new GDPDEFCalculation();

		for (GDPDEFCalculation gdpdefCalculation : gdpdefCalculationList) {
			GDPDEFCalculation modifiedSigndffCalc = gdpdefCalculation;
			if (gdpdefCalculationPrev.getToDate() == null) {
				modifiedSigndffCalc.setRocChangeSign(0);
			} else {
				if (gdpdefCalculationPrev.getRoc() < modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(1);
				} else if (gdpdefCalculationPrev.getRoc() > modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(-1);
				} else {
					modifiedSigndffCalc.setRocChangeSign(0);
				}
			}
			modifiedSignList.add(modifiedSigndffCalc);
			gdpdefCalculationPrev = modifiedSigndffCalc;
		}
		gdpdefCalculationList = gdpdefCalculationRepository.saveAll(modifiedSignList);
		return gdpdefCalculationList;
	}

}
