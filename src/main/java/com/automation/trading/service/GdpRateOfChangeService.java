package com.automation.trading.service;

import com.automation.trading.common.FederalResponse;
import com.automation.trading.domain.calculation.GdpCalculation;
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
public class GdpRateOfChangeService {

	@Autowired
	GdpRepository gdpRepository;
	@Autowired
	DFFRepository dffRepository;
	@Autowired
	UnRateRepostiory unRateRepostiory;
	@Autowired
	GdpCalculationRepository gdpCalculationRepository;
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

	private Logger logger = LoggerFactory.getLogger(GdpRateOfChangeService.class);

	/**
	 * Update Roc change sign , 1 for +ve , 0 for neutral and -1 for -ve
	 */

	public List<GdpCalculation> updateRocChangeSignGdp() {
		List<GdpCalculation> gdpCalculationList = gdpCalculationRepository.findAllByRocIsNotNull();
		if(gdpCalculationList.isEmpty()){
			return  gdpCalculationList;
		}
		List<GdpCalculation> modifiedSignList = new ArrayList<GdpCalculation>();
		GdpCalculation gdpCalculationPrev = new GdpCalculation();

		for (GdpCalculation gdpCalculation : gdpCalculationList) {
			GdpCalculation modifiedSignGdpCalc = gdpCalculation;
			if (gdpCalculationPrev.getToDate() == null) {
				modifiedSignGdpCalc.setRocChangeSign(0);
			} else {
				if (gdpCalculationPrev.getRoc() < modifiedSignGdpCalc.getRoc()) {
					modifiedSignGdpCalc.setRocChangeSign(1);
				} else if (gdpCalculationPrev.getRoc() > modifiedSignGdpCalc.getRoc()) {
					modifiedSignGdpCalc.setRocChangeSign(-1);
				} else {
					modifiedSignGdpCalc.setRocChangeSign(0);
				}
			}
			modifiedSignList.add(modifiedSignGdpCalc);
			gdpCalculationPrev = modifiedSignGdpCalc;
		}
		gdpCalculationList = gdpCalculationRepository.saveAll(modifiedSignList);
		return gdpCalculationList;
	}

	/**
	 * Function to calculate roc roll average flag
	 */

	public List<GdpCalculation> calculateRocRollingAnnualAvgGdp() {

		List<GdpCalculation> gdpCalculationList = new ArrayList<>();
		List<GdpCalculation> gdpCalculationReference = gdpCalculationRepository.findAll();
		Queue<GdpCalculation> gdpCalculationPriorityQueue = new LinkedList<GdpCalculation>();
		for (GdpCalculation gdpCalculation : gdpCalculationReference) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (gdpCalculationPriorityQueue.size() == 4) {
				gdpCalculationPriorityQueue.poll();
			}
			gdpCalculationPriorityQueue.add(gdpCalculation);

			if (gdpCalculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<GdpCalculation> queueIterator = gdpCalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				GdpCalculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			gdpCalculation.setRocAnnRollAvgFlag(true);
			gdpCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(gdpCalculationReference);
		gdpCalculationReference = gdpCalculationRepository.saveAll(gdpCalculationReference);
		return gdpCalculationReference;

	}

	public List<GdpCalculation> calculateRoc() {
		List<Gdp> gdpList = gdpRepository.findAll();
		List<GdpCalculation> gdpCalculationReference = gdpCalculationRepository.findAll();
		List<GdpCalculation> gdpCalculationModified = new ArrayList<>();
		Queue<Gdp> gdpQueue = new LinkedList<>();
		for (Gdp gdp : gdpList) {
			if (gdpQueue.size() == 2) {
				gdpQueue.poll();
			}
			gdpQueue.add(gdp);

			if (gdp.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;
			int period = 0;
			GdpCalculation tempGdpCalculation = new GdpCalculation();

			Iterator<Gdp> queueIterator = gdpQueue.iterator();

			while (queueIterator.hasNext()) {
				Gdp temp = queueIterator.next();
				temp.setRocFlag(true);

				List<GdpCalculation> currentGdpCalculationRef = gdpCalculationReference.stream()
						.filter(p -> p.getToDate().equals(gdp.getDate())).collect(Collectors.toList());

				if (currentGdpCalculationRef.size() > 0)
					tempGdpCalculation = currentGdpCalculationRef.get(0);

				if (gdpQueue.size() == 1) {
					roc = 0f;
					tempGdpCalculation.setRoc(roc);
					tempGdpCalculation.setToDate(gdp.getDate());
				} else {
					roc = (gdp.getValue() / ((LinkedList<Gdp>) gdpQueue).get(0).getValue()) - 1;
					tempGdpCalculation.setRoc(roc);
					tempGdpCalculation.setToDate(gdp.getDate());
				}
			}

			gdpCalculationModified.add(tempGdpCalculation);
		}

		gdpList = gdpRepository.saveAll(gdpList);
		gdpCalculationModified = gdpCalculationRepository.saveAll(gdpCalculationModified);

		return gdpCalculationModified;
	}

	/**
	 * Calculates Rolling Average of Three Month GDP
	 * 
	 * @return GdpCalculation , updated GdpCalculation Table
	 */
	public List<GdpCalculation> calculateRollAvgThreeMonth() {

		List<GdpCalculation> gdpCalculationList = new ArrayList<>();
		List<Gdp> gdpList = gdpRepository.findAll();
		List<GdpCalculation> gdpCalculationReference = gdpCalculationRepository.findAll();
		Queue<Gdp> gdpQueue = new LinkedList<Gdp>();

		for (Gdp gdp : gdpList) {

			if (gdpQueue.size() == 3) {
				gdpQueue.poll();
			}
			gdpQueue.add(gdp);

			if (gdp.getRollAverageFlag()) {
				continue;
			}
			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			Iterator<Gdp> queueItr = gdpQueue.iterator();

			GdpCalculation tempGdpCalculation = new GdpCalculation();
			List<GdpCalculation> currentGdpCalculationRef = gdpCalculationReference.stream()
					.filter(p -> p.getToDate().equals(gdp.getDate())).collect(Collectors.toList());

			if (currentGdpCalculationRef.size() > 0)
				tempGdpCalculation = currentGdpCalculationRef.get(0);

			while (queueItr.hasNext()) {
				Gdp gdpVal = queueItr.next();
				rollingAvg += gdpVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			gdp.setRollAverageFlag(true);
			tempGdpCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			gdpCalculationList.add(tempGdpCalculation);

		}

		gdpCalculationReference = gdpCalculationRepository.saveAll(gdpCalculationList);
		gdpList = gdpRepository.saveAll(gdpList);
		return gdpCalculationReference;
	}

}
