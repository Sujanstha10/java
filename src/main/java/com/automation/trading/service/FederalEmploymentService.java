package com.automation.trading.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.automation.trading.common.FederalResponse;
import com.automation.trading.constants.FederalReserveEconomicDataConstants;
import com.automation.trading.domain.calculation.CIVPARTCalculation;
import com.automation.trading.domain.calculation.EMRATIOCalculation;
import com.automation.trading.domain.calculation.IC4WSACalculation;
import com.automation.trading.domain.calculation.ICSACalculation;
import com.automation.trading.domain.calculation.MANEMPCalculation;
import com.automation.trading.domain.calculation.NROUCalculation;
import com.automation.trading.domain.calculation.NROUSTCalculation;
import com.automation.trading.domain.calculation.PAYEMSCalculation;
import com.automation.trading.domain.calculation.UNEMPLOYCalculation;
import com.automation.trading.domain.fred.CIVPART;
import com.automation.trading.domain.fred.EMRATIO;
import com.automation.trading.domain.fred.IC4WSA;
import com.automation.trading.domain.fred.ICSA;
import com.automation.trading.domain.fred.MANEMP;
import com.automation.trading.domain.fred.NROU;
import com.automation.trading.domain.fred.NROUST;
import com.automation.trading.domain.fred.PAYEMS;
import com.automation.trading.domain.fred.UNEMPLOY;
import com.automation.trading.repository.CIVPARTRepository;
import com.automation.trading.repository.EMRATIORepository;
import com.automation.trading.repository.IC4WSARepository;
import com.automation.trading.repository.ICSARepository;
import com.automation.trading.repository.MANEMPRepository;
import com.automation.trading.repository.NROURepostiory;
import com.automation.trading.repository.NROUSTRepository;
import com.automation.trading.repository.PAYEMSRepository;
import com.automation.trading.repository.UNEMPLOYRepository;
import com.automation.trading.utility.RestUtility;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FederalEmploymentService {

	@Autowired
	private RestUtility restUtility;

	@Autowired
	private NROURepostiory nrouRepostiory;

	@Autowired
	private NROUSTRepository nroustRepository;

	@Autowired
	private CIVPARTRepository civpartRepository;

	@Autowired
	private EMRATIORepository emratioRepository;

	@Autowired
	private UNEMPLOYRepository unemployRepository;

	@Autowired
	private PAYEMSRepository payemsRepository;

	@Autowired
	private MANEMPRepository manempRepository;

	@Autowired
	private ICSARepository icsaRepository;

	@Autowired
	private IC4WSARepository ic4wsaRepository;

	@Value("${quandl.host.url}")
	private String QUANDL_HOST_URL;

	@Value("${quandl.api.key.value}")
	private String QUANDL_API_KEY_VALUE;

	@Value("${quandl.api.key.name}")
	private String QUANDL_API_KEY_NAME;

	@Value("${quandl.data.format}")
	private String QUANDL_DATA_FORMAT;

	public void saveNROUData() {
		String urlToFetch = QUANDL_HOST_URL + FederalReserveEconomicDataConstants.FEDERAL_FRED + "/"
				+ FederalReserveEconomicDataConstants.FEDERAL_NROU + "/" + QUANDL_DATA_FORMAT;
		List<NROU> nrouList = new ArrayList<>();
		FederalResponse json = restUtility.consumeResponse(urlToFetch);
		json.getDataset_data().getData().stream().forEach(o -> {
			ArrayList temp = (ArrayList) o;
			try {
				Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
				nrouList.add(new NROU(date, Float.parseFloat(temp.get(1).toString())));
			} catch (ParseException e) {
				log.error(e.getMessage());
			}
		});
		Collections.sort(nrouList, new SortByDateNROU());
		nrouRepostiory.saveAll(nrouList);

	}

	public void saveNROUSTData() {
		String urlToFetch = QUANDL_HOST_URL + FederalReserveEconomicDataConstants.FEDERAL_FRED + "/"
				+ FederalReserveEconomicDataConstants.FEDERAL_NROUST + "/" + QUANDL_DATA_FORMAT;
		List<NROUST> nroustList = new ArrayList<>();
		FederalResponse json = restUtility.consumeResponse(urlToFetch);
		json.getDataset_data().getData().stream().forEach(o -> {
			ArrayList temp = (ArrayList) o;
			try {
				Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
				nroustList.add(new NROUST(date, Float.parseFloat(temp.get(1).toString())));
			} catch (ParseException e) {
				log.error(e.getMessage());
			}
		});
		Collections.sort(nroustList, new SortByDateNROUST());
		nroustRepository.saveAll(nroustList);

	}

	public void saveCIVPARTData() {
		String urlToFetch = QUANDL_HOST_URL + FederalReserveEconomicDataConstants.FEDERAL_FRED + "/"
				+ FederalReserveEconomicDataConstants.FEDERAL_CIVPART + "/" + QUANDL_DATA_FORMAT;
		List<CIVPART> civpartList = new ArrayList<>();
		FederalResponse json = restUtility.consumeResponse(urlToFetch);
		json.getDataset_data().getData().stream().forEach(o -> {
			ArrayList temp = (ArrayList) o;
			try {
				Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
				civpartList.add(new CIVPART(date, Float.parseFloat(temp.get(1).toString())));
			} catch (ParseException e) {
				log.error(e.getMessage());
			}
		});
		Collections.sort(civpartList, new SortByDateCIVPART());
		civpartRepository.saveAll(civpartList);

	}

	public void saveEMRATIOData() {
		String urlToFetch = QUANDL_HOST_URL + FederalReserveEconomicDataConstants.FEDERAL_FRED + "/"
				+ FederalReserveEconomicDataConstants.FEDERAL_EMRATIO + "/" + QUANDL_DATA_FORMAT;
		List<EMRATIO> emratioList = new ArrayList<>();
		FederalResponse json = restUtility.consumeResponse(urlToFetch);
		json.getDataset_data().getData().stream().forEach(o -> {
			ArrayList temp = (ArrayList) o;
			try {
				Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
				emratioList.add(new EMRATIO(date, Float.parseFloat(temp.get(1).toString())));
			} catch (ParseException e) {
				log.error(e.getMessage());
			}
		});
		Collections.sort(emratioList, new SortByDateEMRATIO());
		emratioRepository.saveAll(emratioList);

	}

	public void saveUNEMPLOYData() {
		String urlToFetch = QUANDL_HOST_URL + FederalReserveEconomicDataConstants.FEDERAL_FRED + "/"
				+ FederalReserveEconomicDataConstants.FEDERAL_UNEMPLOY + "/" + QUANDL_DATA_FORMAT;
		List<UNEMPLOY> unemployList = new ArrayList<>();
		FederalResponse json = restUtility.consumeResponse(urlToFetch);
		json.getDataset_data().getData().stream().forEach(o -> {
			ArrayList temp = (ArrayList) o;
			try {
				Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
				unemployList.add(new UNEMPLOY(date, Float.parseFloat(temp.get(1).toString())));
			} catch (ParseException e) {
				log.error(e.getMessage());
			}
		});

		Collections.sort(unemployList, new SortByDateUNEMPLOY());
		unemployRepository.saveAll(unemployList);

	}

	public void savePAYEMSData() {
		String urlToFetch = QUANDL_HOST_URL + FederalReserveEconomicDataConstants.FEDERAL_FRED + "/"
				+ FederalReserveEconomicDataConstants.FEDERAL_PAYEMS + "/" + QUANDL_DATA_FORMAT;
		List<PAYEMS> payemsList = new ArrayList<>();
		FederalResponse json = restUtility.consumeResponse(urlToFetch);
		json.getDataset_data().getData().stream().forEach(o -> {
			ArrayList temp = (ArrayList) o;
			try {
				Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
				payemsList.add(new PAYEMS(date, Float.parseFloat(temp.get(1).toString())));
			} catch (ParseException e) {
				log.error(e.getMessage());
			}
		});
		payemsRepository.saveAll(payemsList);

	}

	public void saveMANEMPData() {
		String urlToFetch = QUANDL_HOST_URL + FederalReserveEconomicDataConstants.FEDERAL_FRED + "/"
				+ FederalReserveEconomicDataConstants.FEDERAL_MANEMP + "/" + QUANDL_DATA_FORMAT;
		List<MANEMP> manempList = new ArrayList<>();
		FederalResponse json = restUtility.consumeResponse(urlToFetch);
		json.getDataset_data().getData().stream().forEach(o -> {
			ArrayList temp = (ArrayList) o;
			try {
				Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
				manempList.add(new MANEMP(date, Float.parseFloat(temp.get(1).toString())));
			} catch (ParseException e) {
				log.error(e.getMessage());
			}
		});
		manempRepository.saveAll(manempList);

	}

	public void saveICSAData() {
		String urlToFetch = QUANDL_HOST_URL + FederalReserveEconomicDataConstants.FEDERAL_FRED + "/"
				+ FederalReserveEconomicDataConstants.FEDERAL_ICSA + "/" + QUANDL_DATA_FORMAT;
		List<ICSA> icsaList = new ArrayList<>();
		FederalResponse json = restUtility.consumeResponse(urlToFetch);
		json.getDataset_data().getData().stream().forEach(o -> {
			ArrayList temp = (ArrayList) o;
			try {
				Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
				icsaList.add(new ICSA(date, Float.parseFloat(temp.get(1).toString())));
			} catch (ParseException e) {
				log.error(e.getMessage());
			}
		});

		Collections.sort(icsaList, new SortByDateICSA());
		icsaRepository.saveAll(icsaList);

	}

	public void saveIC4WSAData() {
		String urlToFetch = QUANDL_HOST_URL + FederalReserveEconomicDataConstants.FEDERAL_FRED + "/"
				+ FederalReserveEconomicDataConstants.FEDERAL_IC4WSA + "/" + QUANDL_DATA_FORMAT;
		List<IC4WSA> ic4wsaList = new ArrayList<>();
		FederalResponse json = restUtility.consumeResponse(urlToFetch);
		json.getDataset_data().getData().stream().forEach(o -> {
			ArrayList temp = (ArrayList) o;
			try {
				Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
				ic4wsaList.add(new IC4WSA(date, Float.parseFloat(temp.get(1).toString())));
			} catch (ParseException e) {
				log.error(e.getMessage());
			}
		});
		Collections.sort(ic4wsaList, new SortByDateIC4WSA());
		ic4wsaRepository.saveAll(ic4wsaList);

	}

	public static class SortByDateNROU implements Comparator<NROU> {
		@Override
		public int compare(NROU a, NROU b) {
			return a.getDate().compareTo(b.getDate());
		}
	}

	public static class SortByDateNROUCalculation implements Comparator<NROUCalculation> {

		@Override
		public int compare(NROUCalculation a, NROUCalculation b) {
			return a.getToDate().compareTo(b.getToDate());
		}

	}

	public static class SortByDateNROUST implements Comparator<NROUST> {
		@Override
		public int compare(NROUST a, NROUST b) {
			return a.getDate().compareTo(b.getDate());
		}
	}

	public static class SortByDateNROUSTCalculation implements Comparator<NROUSTCalculation> {

		@Override
		public int compare(NROUSTCalculation a, NROUSTCalculation b) {
			return a.getToDate().compareTo(b.getToDate());
		}

	}

	public static class SortByDatePAYEMS implements Comparator<PAYEMS> {
		@Override
		public int compare(PAYEMS a, PAYEMS b) {
			return a.getDate().compareTo(b.getDate());
		}
	}

	public static class SortByDatePAYEMSCalculation implements Comparator<PAYEMSCalculation> {

		@Override
		public int compare(PAYEMSCalculation a, PAYEMSCalculation b) {
			return a.getToDate().compareTo(b.getToDate());
		}

	}

	public static class SortByDateMANEMP implements Comparator<MANEMP> {
		@Override
		public int compare(MANEMP a, MANEMP b) {
			return a.getDate().compareTo(b.getDate());
		}
	}

	public static class SortByDateMANEMPCalculation implements Comparator<MANEMPCalculation> {

		@Override
		public int compare(MANEMPCalculation a, MANEMPCalculation b) {
			return a.getToDate().compareTo(b.getToDate());
		}

	}

	public static class SortByDateCIVPART implements Comparator<CIVPART> {
		@Override
		public int compare(CIVPART a, CIVPART b) {
			return a.getDate().compareTo(b.getDate());
		}
	}

	public static class SortByDateCIVPARTCalculation implements Comparator<CIVPARTCalculation> {

		@Override
		public int compare(CIVPARTCalculation a, CIVPARTCalculation b) {
			return a.getToDate().compareTo(b.getToDate());
		}

	}

	public static class SortByDateEMRATIO implements Comparator<EMRATIO> {
		@Override
		public int compare(EMRATIO a, EMRATIO b) {
			return a.getDate().compareTo(b.getDate());
		}
	}

	public static class SortByDateEMRATIOCalculation implements Comparator<EMRATIOCalculation> {

		@Override
		public int compare(EMRATIOCalculation a, EMRATIOCalculation b) {
			return a.getToDate().compareTo(b.getToDate());
		}

	}

	public static class SortByDateUNEMPLOY implements Comparator<UNEMPLOY> {
		@Override
		public int compare(UNEMPLOY a, UNEMPLOY b) {
			return a.getDate().compareTo(b.getDate());
		}
	}

	public static class SortByDateUNEMPLOYCalculation implements Comparator<UNEMPLOYCalculation> {

		@Override
		public int compare(UNEMPLOYCalculation a, UNEMPLOYCalculation b) {
			return a.getToDate().compareTo(b.getToDate());
		}

	}

	public static class SortByDateICSA implements Comparator<ICSA> {
		@Override
		public int compare(ICSA a, ICSA b) {
			return a.getDate().compareTo(b.getDate());
		}
	}

	public static class SortByDateICSACalculation implements Comparator<ICSACalculation> {

		@Override
		public int compare(ICSACalculation a, ICSACalculation b) {
			return a.getToDate().compareTo(b.getToDate());
		}

	}

	public static class SortByDateIC4WSA implements Comparator<IC4WSA> {
		@Override
		public int compare(IC4WSA a, IC4WSA b) {
			return a.getDate().compareTo(b.getDate());
		}
	}

	public static class SortByDateIC4WSACalculation implements Comparator<IC4WSACalculation> {

		@Override
		public int compare(IC4WSACalculation a, IC4WSACalculation b) {
			return a.getToDate().compareTo(b.getToDate());
		}

	}

}
