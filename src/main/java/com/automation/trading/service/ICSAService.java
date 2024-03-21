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

import com.automation.trading.domain.calculation.ICSACalculation;
import com.automation.trading.domain.fred.ICSA;
import com.automation.trading.repository.ICSACalculationRepository;
import com.automation.trading.repository.ICSARepository;

@Service
public class ICSAService {

	@Autowired
	ICSARepository icsaRepository;

	@Autowired
	private ICSACalculationRepository icsaCalculationRepository;

	@Value("${quandl.host.url}")
	private String QUANDL_HOST_URL;

	@Value("${quandl.api.key.value}")
	private String QUANDL_API_KEY_VALUE;

	@Value("${quandl.api.key.name}")
	private String QUANDL_API_KEY_NAME;

	@Value("${quandl.data.format}")
	private String QUANDL_DATA_FORMAT;

	private Logger logger = LoggerFactory.getLogger(ICSAService.class);

	/**
	 * Update Roc change sign , 1 for +ve , 0 for neutral and -1 for -ve
	 */

	public List<ICSACalculation> updateRocChangeSignICSA() {
		List<ICSACalculation> icsaCalculationList = icsaCalculationRepository.findAllByRocIsNotNull();
		if (icsaCalculationList.isEmpty()) {
			return icsaCalculationList;
		}
		List<ICSACalculation> modifiedSignList = new ArrayList<ICSACalculation>();
		ICSACalculation icsaCalculationPrev = new ICSACalculation();

		for (ICSACalculation icsaCalculation : icsaCalculationList) {
			ICSACalculation modifiedSignICSACalc = icsaCalculation;
			if (icsaCalculationPrev.getToDate() == null) {
				modifiedSignICSACalc.setRocChangeSign(0);
			} else {
				if (icsaCalculationPrev.getRoc() < modifiedSignICSACalc.getRoc()) {
					modifiedSignICSACalc.setRocChangeSign(1);
				} else if (icsaCalculationPrev.getRoc() > modifiedSignICSACalc.getRoc()) {
					modifiedSignICSACalc.setRocChangeSign(-1);
				} else {
					modifiedSignICSACalc.setRocChangeSign(0);
				}
			}
			modifiedSignList.add(modifiedSignICSACalc);
			icsaCalculationPrev = modifiedSignICSACalc;
		}
		icsaCalculationList = icsaCalculationRepository.saveAll(modifiedSignList);
		return icsaCalculationList;
	}

	/**
	 * Function to calculate roc roll average flag
	 */

	public List<ICSACalculation> calculateRocRollingAnnualAvgICSA() {

		List<ICSACalculation> icsaCalculationList = new ArrayList<>();
		List<ICSACalculation> icsaCalculationReference = icsaCalculationRepository.findAll();
		Queue<ICSACalculation> icsaCalculationPriorityQueue = new LinkedList<ICSACalculation>();
		for (ICSACalculation icsaCalculation : icsaCalculationReference) {
			Float rocFourMonth = 0.0f;
			Float rocFourMonthAvg = 0.0f;
			int period = 0;
			if (icsaCalculationPriorityQueue.size() == 4) {
				icsaCalculationPriorityQueue.poll();
			}
			icsaCalculationPriorityQueue.add(icsaCalculation);

			if (icsaCalculation.getRocAnnRollAvgFlag()) {
				continue;
			}
			Iterator<ICSACalculation> queueIterator = icsaCalculationPriorityQueue.iterator();
			while (queueIterator.hasNext()) {
				ICSACalculation temp = queueIterator.next();
				rocFourMonth += temp.getRoc();
				period++;
			}
			rocFourMonthAvg = rocFourMonth / period;
			icsaCalculation.setRocAnnRollAvgFlag(true);
			icsaCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
		}
		System.out.println(icsaCalculationReference);
		icsaCalculationReference = icsaCalculationRepository.saveAll(icsaCalculationReference);
		return icsaCalculationReference;

	}

	public List<ICSACalculation> calculateRoc() {
		List<ICSA> icsaList = icsaRepository.findAll();
		List<ICSACalculation> icsaCalculationReference = icsaCalculationRepository.findAll();
		List<ICSACalculation> icsaCalculationModified = new ArrayList<>();
		Queue<ICSA> icsaQueue = new LinkedList<>();
		for (ICSA icsa : icsaList) {
			if (icsaQueue.size() == 2) {
				icsaQueue.poll();
			}
			icsaQueue.add(icsa);

			if (icsa.getRocFlag()) {
				continue;
			}
			Float roc = 0.0f;
			int period = 0;
			ICSACalculation tempICSACalculation = new ICSACalculation();

			Iterator<ICSA> queueIterator = icsaQueue.iterator();

			while (queueIterator.hasNext()) {
				ICSA temp = queueIterator.next();
				temp.setRocFlag(true);

				List<ICSACalculation> currentICSACalculationRef = icsaCalculationReference.stream()
						.filter(p -> p.getToDate().equals(icsa.getDate())).collect(Collectors.toList());

				if (currentICSACalculationRef.size() > 0)
					tempICSACalculation = currentICSACalculationRef.get(0);

				if (icsaQueue.size() == 1) {
					roc = 0f;
					tempICSACalculation.setRoc(roc);
					tempICSACalculation.setToDate(icsa.getDate());
				} else {
					roc = (icsa.getValue() / ((LinkedList<ICSA>) icsaQueue).get(0).getValue()) - 1;
					tempICSACalculation.setRoc(roc);
					tempICSACalculation.setToDate(icsa.getDate());
				}
			}

			icsaCalculationModified.add(tempICSACalculation);
		}

		icsaList = icsaRepository.saveAll(icsaList);
		icsaCalculationModified = icsaCalculationRepository.saveAll(icsaCalculationModified);

		return icsaCalculationModified;
	}

	/**
	 * Calculates Rolling Average of Three Month GDP
	 *
	 * @return ICSACalculation , updated ICSACalculation Table
	 */
	public List<ICSACalculation> calculateRollAvgThreeMonth() {

		List<ICSACalculation> icsaCalculationList = new ArrayList<>();
		List<ICSA> icsaList = icsaRepository.findAll();
		List<ICSACalculation> icsaCalculationReference = icsaCalculationRepository.findAll();
		Queue<ICSA> icsaQueue = new LinkedList<ICSA>();

		for (ICSA icsa : icsaList) {

			if (icsaQueue.size() == 3) {
				icsaQueue.poll();
			}
			icsaQueue.add(icsa);

			if (icsa.getRollAverageFlag()) {
				continue;
			}
			Float rollingAvg = 0.0f;
			Float rollingAvgThreeMon = 0f;
			int period = 0;

			Iterator<ICSA> queueItr = icsaQueue.iterator();

			ICSACalculation tempICSACalculation = new ICSACalculation();
			List<ICSACalculation> currentICSACalculationRef = icsaCalculationReference.stream()
					.filter(p -> p.getToDate().equals(icsa.getDate())).collect(Collectors.toList());

			if (currentICSACalculationRef.size() > 0)
				tempICSACalculation = currentICSACalculationRef.get(0);

			while (queueItr.hasNext()) {
				ICSA icsaVal = queueItr.next();
				rollingAvg += icsaVal.getValue();
				period++;
			}

			rollingAvgThreeMon = rollingAvg / period;

			icsa.setRollAverageFlag(true);
			tempICSACalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
			icsaCalculationList.add(tempICSACalculation);

		}

		icsaCalculationReference = icsaCalculationRepository.saveAll(icsaCalculationList);
		icsaList = icsaRepository.saveAll(icsaList);
		return icsaCalculationReference;
	}

}
