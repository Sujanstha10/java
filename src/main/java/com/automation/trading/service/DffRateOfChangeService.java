package com.automation.trading.service;

import com.automation.trading.common.FederalResponse;
import com.automation.trading.domain.calculation.DffCalculation;

import com.automation.trading.domain.fred.*;
import com.automation.trading.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DffRateOfChangeService {

	@Autowired
	DFFRepository dffRepository;
	@Autowired
	UnRateRepostiory unRateRepostiory;
	@Autowired
	DffCalculationRepository dffCalculationRepository;
	@Autowired
	UnRateCalculationRepository unRateCalculationRepository;
	@Autowired
	RestTemplate restTemplate;

	@Value("${quandl.host.url}")
	private String QUANDL_HOST_URL;

	@Value("${quandl.api.key.value}")
	private String QUANDL_API_KEY_VALUE;

	@Value("${quandl.api.key.name}")
	private String QUANDL_API_KEY_NAME;

	@Value("${quandl.data.format}")
	private String QUANDL_DATA_FORMAT;

	private Logger logger = LoggerFactory.getLogger(DffRateOfChangeService.class);

	/**
	 * Update Roc change sign , 1 for +ve , 0 for neutral and -1 for -ve
	 */

	public List<DffCalculation> updateRocChangeSignDFF() {
		List<DffCalculation> dffCalculationList = dffCalculationRepository.findAllByRocIsNotNull();
		if(dffCalculationList.isEmpty()){
			return dffCalculationList;
		}
		List<DffCalculation> modifiedSignList = new ArrayList<DffCalculation>();
		DffCalculation dffCalculationPrev = new DffCalculation();

		for (DffCalculation dffCalculation : dffCalculationList) {
			DffCalculation modifiedSignDFFCalc = dffCalculation;
			if (dffCalculationPrev.getToDate() == null) {
				modifiedSignDFFCalc.setRocChangeSign(0);
			} else {
				if (dffCalculationPrev.getRoc() < modifiedSignDFFCalc.getRoc()) {
					modifiedSignDFFCalc.setRocChangeSign(1);
				} else if (dffCalculationPrev.getRoc() > modifiedSignDFFCalc.getRoc()) {
					modifiedSignDFFCalc.setRocChangeSign(-1);
				} else {
					modifiedSignDFFCalc.setRocChangeSign(0);
				}
			}
			modifiedSignList.add(modifiedSignDFFCalc);
			dffCalculationPrev = modifiedSignDFFCalc;
		}
		dffCalculationList = dffCalculationRepository.saveAll(modifiedSignList);
		return dffCalculationList;
	}

	/**
	 * Function to calculate roc roll average flag
	 */

	public List<DffCalculation> calculateRocRollingAnnualAvgDFF() {

		List<DffCalculation> dffCalculationList = new ArrayList<>();
		List<DffCalculation> dffCalculationReference = dffCalculationRepository.findAll();
		Queue<DffCalculation> dffCalculationPriorityQueue = new LinkedList<DffCalculation>();
		for (DffCalculation dffCalculation : dffCalculationReference) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (dffCalculationPriorityQueue.size() == 4) {
				dffCalculationPriorityQueue.poll();
			}
			dffCalculationPriorityQueue.add(dffCalculation);

			if (dffCalculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<DffCalculation> queueIterator = dffCalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				DffCalculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			dffCalculation.setRocAnnRollAvgFlag(true);
			dffCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(dffCalculationReference);
		dffCalculationReference = dffCalculationRepository.saveAll(dffCalculationReference);
		return dffCalculationReference;

	}

	public List<DffCalculation> calculateRoc() {
		List<DFF> dffList = dffRepository.findAll();
		List<DffCalculation> dffCalculationReference = dffCalculationRepository.findAll();
		List<DffCalculation> dffCalculationModified = new ArrayList<>();
		Queue<DFF> dffQueue = new LinkedList<>();
		for (DFF dff : dffList) {
			if (dffQueue.size() == 2) {
				dffQueue.poll();
			}
			dffQueue.add(dff);

			if (dff.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;
			int period = 0;
			DffCalculation tempDffCalculation = new DffCalculation();

			Iterator<DFF> queueIterator = dffQueue.iterator();

			while (queueIterator.hasNext()) {
				DFF temp = queueIterator.next();
				temp.setRocFlag(true);

				List<DffCalculation> currentDffCalculationRef = dffCalculationReference.stream()
						.filter(p -> p.getToDate().equals(dff.getDate())).collect(Collectors.toList());

				if (currentDffCalculationRef.size() > 0)
					tempDffCalculation = currentDffCalculationRef.get(0);

				if (dffQueue.size() == 1) {
					roc = 0f;
					tempDffCalculation.setRoc(roc);
					tempDffCalculation.setToDate(dff.getDate());
				} else {
					roc = (dff.getValue() / ((LinkedList<DFF>) dffQueue).get(0).getValue()) - 1;
					tempDffCalculation.setRoc(roc);
					tempDffCalculation.setToDate(dff.getDate());
				}
			}

			dffCalculationModified.add(tempDffCalculation);
		}

		dffList = dffRepository.saveAll(dffList);
		dffCalculationModified = dffCalculationRepository.saveAll(dffCalculationModified);

		return dffCalculationModified;
	}


	/**
	 * Calculates Rolling Average of Three Month GDP
	 *
	 * @return DffCalculation , updated DffCalculation Table
	 */
	public List<DffCalculation> calculateRollAvgThreeMonth() {

		List<DffCalculation> dffCalculationList = new ArrayList<>();
		List<DFF> dffList = dffRepository.findAll();
		List<DffCalculation> dffCalculationReference = dffCalculationRepository.findAll();
		Queue<DFF> dffQueue = new LinkedList<DFF>();

		for (DFF dff : dffList) {

			if (dffQueue.size() == 3) {
				dffQueue.poll();
			}
			dffQueue.add(dff);

			if (dff.getRollAverageFlag()) {
				continue;
			}
			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			Iterator<DFF> queueItr = dffQueue.iterator();

			DffCalculation tempDffCalculation = new DffCalculation();
			List<DffCalculation> currentDffCalculationRef = dffCalculationReference.stream()
					.filter(p -> p.getToDate().equals(dff.getDate())).collect(Collectors.toList());

			if (currentDffCalculationRef.size() > 0)
				tempDffCalculation = currentDffCalculationRef.get(0);

			while (queueItr.hasNext()) {
				DFF dffVal = queueItr.next();
				rollingAvg += dffVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			dff.setRollAverageFlag(true);
			tempDffCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			dffCalculationList.add(tempDffCalculation);

		}

		dffCalculationReference = dffCalculationRepository.saveAll(dffCalculationList);
		dffList = dffRepository.saveAll(dffList);
		return dffCalculationReference;
	}

}
