package com.automation.trading.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.automation.trading.domain.calculation.IC4WSACalculation;
import com.automation.trading.domain.fred.IC4WSA;
import com.automation.trading.repository.IC4WSACalculationRepository;
import com.automation.trading.repository.IC4WSARepository;

@Service
public class IC4WSAService {

	@Autowired
	IC4WSARepository ic4wsaRepository;

	@Autowired
	private IC4WSACalculationRepository ic4wsaCalculationRepository;

	@Value("${quandl.host.url}")
	private String QUANDL_HOST_URL;

	@Value("${quandl.api.key.value}")
	private String QUANDL_API_KEY_VALUE;

	@Value("${quandl.api.key.name}")
	private String QUANDL_API_KEY_NAME;

	@Value("${quandl.data.format}")
	private String QUANDL_DATA_FORMAT;

	private Logger logger = LoggerFactory.getLogger(IC4WSAService.class);

	/**
	 * Update Roc change sign , 1 for +ve , 0 for neutral and -1 for -ve
	 */

	public List<IC4WSACalculation> updateRocChangeSignIC4WSA() {
		List<IC4WSACalculation> ic4wsaCalculationList = ic4wsaCalculationRepository.findAllByRocIsNotNull();
		if (ic4wsaCalculationList.isEmpty()) {
			return ic4wsaCalculationList;
		}
		List<IC4WSACalculation> modifiedSignList = new ArrayList<IC4WSACalculation>();
		IC4WSACalculation ic4wsaCalculationPrev = new IC4WSACalculation();

		for (IC4WSACalculation ic4wsaCalculation : ic4wsaCalculationList) {
			IC4WSACalculation modifiedSignIC4WSACalc = ic4wsaCalculation;
			if (ic4wsaCalculationPrev.getToDate() == null) {
				modifiedSignIC4WSACalc.setRocChangeSign(0);
			} else {
				if (ic4wsaCalculationPrev.getRoc() < modifiedSignIC4WSACalc.getRoc()) {
					modifiedSignIC4WSACalc.setRocChangeSign(1);
				} else if (ic4wsaCalculationPrev.getRoc() > modifiedSignIC4WSACalc.getRoc()) {
					modifiedSignIC4WSACalc.setRocChangeSign(-1);
				} else {
					modifiedSignIC4WSACalc.setRocChangeSign(0);
				}
			}
			modifiedSignList.add(modifiedSignIC4WSACalc);
			ic4wsaCalculationPrev = modifiedSignIC4WSACalc;
		}
		ic4wsaCalculationList = ic4wsaCalculationRepository.saveAll(modifiedSignList);
		return ic4wsaCalculationList;
	}

	/**
	 * Function to calculate roc roll average flag
	 */

	public List<IC4WSACalculation> calculateRocRollingAnnualAvgIC4WSA() {

		List<IC4WSACalculation> ic4wsaCalculationList = new ArrayList<>();
		List<IC4WSACalculation> ic4wsaCalculationReference = ic4wsaCalculationRepository.findAll();
		Queue<IC4WSACalculation> ic4wsaCalculationPriorityQueue = new LinkedList<IC4WSACalculation>();
		for (IC4WSACalculation ic4wsaCalculation : ic4wsaCalculationReference) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (ic4wsaCalculationPriorityQueue.size() == 4) {
				ic4wsaCalculationPriorityQueue.poll();
			}
			ic4wsaCalculationPriorityQueue.add(ic4wsaCalculation);

			if (ic4wsaCalculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<IC4WSACalculation> queueIterator = ic4wsaCalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				IC4WSACalculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			ic4wsaCalculation.setRocAnnRollAvgFlag(true);
			ic4wsaCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(ic4wsaCalculationReference);
		ic4wsaCalculationReference = ic4wsaCalculationRepository.saveAll(ic4wsaCalculationReference);
		return ic4wsaCalculationReference;

	}

	public List<IC4WSACalculation> calculateRoc() {
		List<IC4WSA> ic4wsaList = ic4wsaRepository.findAll();
		List<IC4WSACalculation> ic4wsaCalculationReference = ic4wsaCalculationRepository.findAll();
		List<IC4WSACalculation> ic4wsaCalculationModified = new ArrayList<>();
		Queue<IC4WSA> ic4wsaQueue = new LinkedList<>();
		for (IC4WSA ic4wsa : ic4wsaList) {
			if (ic4wsaQueue.size() == 2) {
				ic4wsaQueue.poll();
			}
			ic4wsaQueue.add(ic4wsa);

			if (ic4wsa.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;
			int period = 0;
			IC4WSACalculation tempIC4WSACalculation = new IC4WSACalculation();

			Iterator<IC4WSA> queueIterator = ic4wsaQueue.iterator();

			while (queueIterator.hasNext()) {
				IC4WSA temp = queueIterator.next();
				temp.setRocFlag(true);

				List<IC4WSACalculation> currentIC4WSACalculationRef = ic4wsaCalculationReference.stream()
						.filter(p -> p.getToDate().equals(ic4wsa.getDate())).collect(Collectors.toList());

				if (currentIC4WSACalculationRef.size() > 0)
					tempIC4WSACalculation = currentIC4WSACalculationRef.get(0);

				if (ic4wsaQueue.size() == 1) {
					roc = 0f;
					tempIC4WSACalculation.setRoc(roc);
					tempIC4WSACalculation.setToDate(ic4wsa.getDate());
				} else {
					roc = (ic4wsa.getValue() / ((LinkedList<IC4WSA>) ic4wsaQueue).get(0).getValue()) - 1;
					tempIC4WSACalculation.setRoc(roc);
					tempIC4WSACalculation.setToDate(ic4wsa.getDate());
				}
			}

			ic4wsaCalculationModified.add(tempIC4WSACalculation);
		}

		ic4wsaList = ic4wsaRepository.saveAll(ic4wsaList);
		ic4wsaCalculationModified = ic4wsaCalculationRepository.saveAll(ic4wsaCalculationModified);

		return ic4wsaCalculationModified;
	}

	/**
	 * Calculates Rolling Average of Three Month GDP
	 *
	 * @return IC4WSACalculation , updated IC4WSACalculation Table
	 */
	public List<IC4WSACalculation> calculateRollAvgThreeMonth() {

		List<IC4WSACalculation> ic4wsaCalculationList = new ArrayList<>();
		List<IC4WSA> ic4wsaList = ic4wsaRepository.findAll();
		List<IC4WSACalculation> ic4wsaCalculationReference = ic4wsaCalculationRepository.findAll();
		Queue<IC4WSA> ic4wsaQueue = new LinkedList<IC4WSA>();

		for (IC4WSA ic4wsa : ic4wsaList) {

			if (ic4wsaQueue.size() == 3) {
				ic4wsaQueue.poll();
			}
			ic4wsaQueue.add(ic4wsa);

			if (ic4wsa.getRollAverageFlag()) {
				continue;
			}
			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			Iterator<IC4WSA> queueItr = ic4wsaQueue.iterator();

			IC4WSACalculation tempIC4WSACalculation = new IC4WSACalculation();
			List<IC4WSACalculation> currentIC4WSACalculationRef = ic4wsaCalculationReference.stream()
					.filter(p -> p.getToDate().equals(ic4wsa.getDate())).collect(Collectors.toList());

			if (currentIC4WSACalculationRef.size() > 0)
				tempIC4WSACalculation = currentIC4WSACalculationRef.get(0);

			while (queueItr.hasNext()) {
				IC4WSA ic4wsaVal = queueItr.next();
				rollingAvg += ic4wsaVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			ic4wsa.setRollAverageFlag(true);
			tempIC4WSACalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			ic4wsaCalculationList.add(tempIC4WSACalculation);

		}

		ic4wsaCalculationReference = ic4wsaCalculationRepository.saveAll(ic4wsaCalculationList);
		ic4wsaList = ic4wsaRepository.saveAll(ic4wsaList);
		return ic4wsaCalculationReference;
	}

}
