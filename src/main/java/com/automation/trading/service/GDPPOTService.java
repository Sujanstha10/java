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

import com.automation.trading.domain.calculation.GDPPOTCalculation;
import com.automation.trading.domain.fred.GDPPOT;
import com.automation.trading.repository.GDPPOTCalculationRepository;
import com.automation.trading.repository.GDPPOTRepository;
import com.automation.trading.utility.RestUtility;

@Service
public class GDPPOTService {

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
	private GDPPOTRepository gdppotRepository;

	@Autowired
	private GDPPOTCalculationRepository gdppotCalculationRepository;

	public void calculateRoc() {
		List<GDPPOT> gdppotList = gdppotRepository.findAll();
		List<GDPPOTCalculation> gdppotCalculationList = gdppotCalculationRepository.findAll();
		List<GDPPOTCalculation> gdppotCalculationModified = new ArrayList<>();
		Queue<GDPPOT> gdppotQueue = new LinkedList<>();
		for (GDPPOT gdppot : gdppotList) {
			if (gdppot.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;
			int period = 0;
			GDPPOTCalculation gdppotCalculation = new GDPPOTCalculation();
			if (gdppotQueue.size() == 2) {
				gdppotQueue.poll();
			}
			gdppotQueue.add(gdppot);
			Iterator<GDPPOT> queueIterator = gdppotQueue.iterator();
			while (queueIterator.hasNext()) {
				GDPPOT temp = queueIterator.next();
				temp.setRocFlag(true);
			}

			List<GDPPOTCalculation> currentGDPPOTCalculationRef = gdppotCalculationList.stream()
					.filter(p -> p.getToDate().equals(gdppot.getDate())).collect(Collectors.toList());

			if (currentGDPPOTCalculationRef.size() > 0)
				gdppotCalculation = currentGDPPOTCalculationRef.get(0);

			if (gdppotQueue.size() == 1) {
				roc = 0f;
				gdppotCalculation.setRoc(roc);
				gdppotCalculation.setToDate(gdppot.getDate());
			} else {
				roc = (gdppot.getValue() / ((LinkedList<GDPPOT>) gdppotQueue).get(0).getValue()) - 1;
				gdppotCalculation.setRoc(roc);
				gdppotCalculation.setToDate(gdppot.getDate());
			}
			gdppotCalculationModified.add(gdppotCalculation);
		}

		gdppotRepository.saveAll(gdppotList);
		gdppotCalculationRepository.saveAll(gdppotCalculationModified);

	}

	public List<GDPPOTCalculation> calculateRollAvgThreeMonth() {
		List<GDPPOTCalculation> gdppotCalculationList = new ArrayList<>();
		List<GDPPOT> gdppotList = gdppotRepository.findAll();
		List<GDPPOTCalculation> gdppotCalculationReference = gdppotCalculationRepository.findAll();
		Queue<GDPPOT> gdppotQueue = new LinkedList<>();

		for (GDPPOT gdppot : gdppotList) {

			if (gdppotQueue.size() == 3) {
				gdppotQueue.poll();
			}
			gdppotQueue.add(gdppot);

			if (gdppot.getRollAverageFlag()) {
				continue;
			}
			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			Iterator<GDPPOT> queueItr = gdppotQueue.iterator();

			GDPPOTCalculation tempGdpCalculation = new GDPPOTCalculation();
			List<GDPPOTCalculation> currentGDPPOTCalculationRef = gdppotCalculationReference.stream()
					.filter(p -> p.getToDate().equals(gdppot.getDate())).collect(Collectors.toList());

			if (currentGDPPOTCalculationRef.size() > 0)
				tempGdpCalculation = currentGDPPOTCalculationRef.get(0);

			while (queueItr.hasNext()) {
				GDPPOT gdpVal = queueItr.next();
				rollingAvg += gdpVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			gdppot.setRollAverageFlag(true);
			tempGdpCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			gdppotCalculationList.add(tempGdpCalculation);

		}

		gdppotCalculationReference = gdppotCalculationRepository.saveAll(gdppotCalculationList);
		gdppotList = gdppotRepository.saveAll(gdppotList);
		return gdppotCalculationReference;
	}

	public List<GDPPOTCalculation> calculateRocRollingAnnualAvg() {

		List<GDPPOTCalculation> gdppotCalculationReference = gdppotCalculationRepository.findAll();
		Queue<GDPPOTCalculation> gdppotCalculationPriorityQueue = new LinkedList<>();
		for (GDPPOTCalculation gdppotCalculation : gdppotCalculationReference) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (gdppotCalculationPriorityQueue.size() == 4) {
				gdppotCalculationPriorityQueue.poll();
			}
			gdppotCalculationPriorityQueue.add(gdppotCalculation);

			if (gdppotCalculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<GDPPOTCalculation> queueIterator = gdppotCalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				GDPPOTCalculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			gdppotCalculation.setRocAnnRollAvgFlag(true);
			gdppotCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(gdppotCalculationReference);
		gdppotCalculationReference = gdppotCalculationRepository.saveAll(gdppotCalculationReference);
		return gdppotCalculationReference;
	}

	public List<GDPPOTCalculation> updateRocChangeSign() {
		List<GDPPOTCalculation> gdppotCalculationList = gdppotCalculationRepository.findAllByRocIsNotNull();
		if(gdppotCalculationList.isEmpty()){
			return gdppotCalculationList;
		}
		List<GDPPOTCalculation> modifiedSignList = new ArrayList<>();
		GDPPOTCalculation gdppotCalculationPrev = new GDPPOTCalculation();

		for (GDPPOTCalculation gdppotCalculation : gdppotCalculationList) {
			GDPPOTCalculation modifiedSigndffCalc = gdppotCalculation;
			if (gdppotCalculationPrev.getToDate() == null) {
				modifiedSigndffCalc.setRocChangeSign(0);
			} else {
				if (gdppotCalculationPrev.getRoc() < modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(1);
				} else if (gdppotCalculationPrev.getRoc() > modifiedSigndffCalc.getRoc()) {
					modifiedSigndffCalc.setRocChangeSign(-1);
				} else {
					modifiedSigndffCalc.setRocChangeSign(0);
				}
			}
			modifiedSignList.add(modifiedSigndffCalc);
			gdppotCalculationPrev = modifiedSigndffCalc;
		}
		gdppotCalculationList = gdppotCalculationRepository.saveAll(modifiedSignList);
		return gdppotCalculationList;
	}

}
