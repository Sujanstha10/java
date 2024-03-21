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
import org.springframework.web.util.UriComponentsBuilder;

import com.automation.trading.common.FederalResponse;
import com.automation.trading.constants.FederalReserveEconomicDataConstants;
import com.automation.trading.domain.calculation.DSPIC96Calculation;
import com.automation.trading.domain.calculation.MEHOINUSA672NCalculation;
import com.automation.trading.domain.calculation.PCECalculation;
import com.automation.trading.domain.calculation.PCEDGCalculation;
import com.automation.trading.domain.calculation.PSAVERTCalculation;
import com.automation.trading.domain.fred.DSPIC96;
import com.automation.trading.domain.fred.MEHOINUSA672N;
import com.automation.trading.domain.fred.PCE;
import com.automation.trading.domain.fred.PCEDG;
import com.automation.trading.domain.fred.PSAVERT;
import com.automation.trading.repository.DSPIC96Repository;
import com.automation.trading.repository.MEHOINUSA672NRepository;
import com.automation.trading.repository.PCEDGRepository;
import com.automation.trading.repository.PCERepository;
import com.automation.trading.repository.PSAVERTRepository;
import com.automation.trading.utility.RestUtility;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FederalIncomeAndExpenditureService {

	@Autowired
	private RestUtility restUtility;

	@Autowired
	private MEHOINUSA672NRepository mehoinusa672nRepository;

	@Autowired
	private DSPIC96Repository dspic96Repository;

	@Autowired
	private PCERepository pceRepository;

	@Autowired
	private PCEDGRepository pcedgRepository;

	@Autowired
	private PSAVERTRepository psavertRepository;

	@Value("${quandl.host.url}")
	private String QUANDL_HOST_URL;

	@Value("${quandl.api.key.value}")
	private String QUANDL_API_KEY_VALUE;

	@Value("${quandl.api.key.name}")
	private String QUANDL_API_KEY_NAME;

	@Value("${quandl.data.format}")
	private String QUANDL_DATA_FORMAT;

	public void saveMEHOINUSA672NData() {
		String urlToFetch = QUANDL_HOST_URL + FederalReserveEconomicDataConstants.FEDERAL_FRED + "/"
				+ FederalReserveEconomicDataConstants.FEDERAL_MEHOINUSA672N + "/" + QUANDL_DATA_FORMAT;
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(urlToFetch).queryParam(QUANDL_API_KEY_NAME,
				QUANDL_API_KEY_VALUE);
		List<MEHOINUSA672N> mehoinusa672nList = new ArrayList<>();
		FederalResponse json = restUtility.consumeResponse(builder.toUriString());
		json.getDataset_data().getData().stream().forEach(o -> {
			ArrayList temp = (ArrayList) o;
			try {
				Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
				mehoinusa672nList.add(new MEHOINUSA672N(date, Float.parseFloat(temp.get(1).toString())));
			} catch (ParseException e) {
				log.error(e.getMessage());
			}
		});
		Collections.sort(mehoinusa672nList, new SortByDateMEHOINUSA672N());
		mehoinusa672nRepository.saveAll(mehoinusa672nList);
	}

	public void saveDSPIC96Data() {
		String urlToFetch = QUANDL_HOST_URL + FederalReserveEconomicDataConstants.FEDERAL_FRED + "/"
				+ FederalReserveEconomicDataConstants.FEDERAL_DSPIC96 + "/" + QUANDL_DATA_FORMAT;
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(urlToFetch).queryParam(QUANDL_API_KEY_NAME,
				QUANDL_API_KEY_VALUE);
		List<DSPIC96> dspic96List = new ArrayList<>();
		FederalResponse json = restUtility.consumeResponse(builder.toUriString());
		json.getDataset_data().getData().stream().forEach(o -> {
			ArrayList temp = (ArrayList) o;
			try {
				Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
				dspic96List.add(new DSPIC96(date, Float.parseFloat(temp.get(1).toString())));
			} catch (ParseException e) {
				log.error(e.getMessage());
			}
		});
		Collections.sort(dspic96List, new SortByDateDSPIC96());
		dspic96Repository.saveAll(dspic96List);
	}

	public void savePCEData() {
		String urlToFetch = QUANDL_HOST_URL + FederalReserveEconomicDataConstants.FEDERAL_FRED + "/"
				+ FederalReserveEconomicDataConstants.FEDERAL_PCE + "/" + QUANDL_DATA_FORMAT;
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(urlToFetch).queryParam(QUANDL_API_KEY_NAME,
				QUANDL_API_KEY_VALUE);
		List<PCE> pceList = new ArrayList<>();
		FederalResponse json = restUtility.consumeResponse(builder.toUriString());
		json.getDataset_data().getData().stream().forEach(o -> {
			ArrayList temp = (ArrayList) o;
			try {
				Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
				pceList.add(new PCE(date, Float.parseFloat(temp.get(1).toString())));
			} catch (ParseException e) {
				log.error(e.getMessage());
			}
		});
		Collections.sort(pceList, new SortByDatePCE());
		pceRepository.saveAll(pceList);
	}

	public void savePCEDGData() {
		String urlToFetch = QUANDL_HOST_URL + FederalReserveEconomicDataConstants.FEDERAL_FRED + "/"
				+ FederalReserveEconomicDataConstants.FEDERAL_PCEDG + "/" + QUANDL_DATA_FORMAT;
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(urlToFetch).queryParam(QUANDL_API_KEY_NAME,
				QUANDL_API_KEY_VALUE);
		List<PCEDG> pcedgList = new ArrayList<>();
		FederalResponse json = restUtility.consumeResponse(builder.toUriString());
		json.getDataset_data().getData().stream().forEach(o -> {
			ArrayList temp = (ArrayList) o;
			try {
				Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
				pcedgList.add(new PCEDG(date, Float.parseFloat(temp.get(1).toString())));
			} catch (ParseException e) {
				log.error(e.getMessage());
			}
		});
		Collections.sort(pcedgList, new SortByDatePCEDG());
		pcedgRepository.saveAll(pcedgList);
	}

	public void savePSAVERTData() {
		String urlToFetch = QUANDL_HOST_URL + FederalReserveEconomicDataConstants.FEDERAL_FRED + "/"
				+ FederalReserveEconomicDataConstants.FEDERAL_PSAVERT + "/" + QUANDL_DATA_FORMAT;
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(urlToFetch).queryParam(QUANDL_API_KEY_NAME,
				QUANDL_API_KEY_VALUE);
		List<PSAVERT> psavertList = new ArrayList<>();
		FederalResponse json = restUtility.consumeResponse(builder.toUriString());
		json.getDataset_data().getData().stream().forEach(o -> {
			ArrayList temp = (ArrayList) o;
			try {
				Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
				psavertList.add(new PSAVERT(date, Float.parseFloat(temp.get(1).toString())));
			} catch (ParseException e) {
				log.error(e.getMessage());
			}
		});
		Collections.sort(psavertList, new SortByDatePSAVERT());
		psavertRepository.saveAll(psavertList);
	}

	public static class SortByDateMEHOINUSA672N implements Comparator<MEHOINUSA672N> {
		@Override
		public int compare(MEHOINUSA672N a, MEHOINUSA672N b) {
			return a.getDate().compareTo(b.getDate());
		}
	}

	public static class SortByDateMEHOINUSA672NCalculation implements Comparator<MEHOINUSA672NCalculation> {

		@Override
		public int compare(MEHOINUSA672NCalculation a, MEHOINUSA672NCalculation b) {
			return a.getToDate().compareTo(b.getToDate());
		}

	}

	public static class SortByDateDSPIC96 implements Comparator<DSPIC96> {
		@Override
		public int compare(DSPIC96 a, DSPIC96 b) {
			return a.getDate().compareTo(b.getDate());
		}
	}

	public static class SortByDateDSPIC96Calculation implements Comparator<DSPIC96Calculation> {

		@Override
		public int compare(DSPIC96Calculation a, DSPIC96Calculation b) {
			return a.getToDate().compareTo(b.getToDate());
		}

	}

	public static class SortByDatePCE implements Comparator<PCE> {
		@Override
		public int compare(PCE a, PCE b) {
			return a.getDate().compareTo(b.getDate());
		}
	}

	public static class SortByDatePCECalculation implements Comparator<PCECalculation> {

		@Override
		public int compare(PCECalculation a, PCECalculation b) {
			return a.getToDate().compareTo(b.getToDate());
		}

	}

	public static class SortByDatePCEDG implements Comparator<PCEDG> {
		@Override
		public int compare(PCEDG a, PCEDG b) {
			return a.getDate().compareTo(b.getDate());
		}
	}

	public static class SortByDatePCEDGCalculation implements Comparator<PCEDGCalculation> {

		@Override
		public int compare(PCEDGCalculation a, PCEDGCalculation b) {
			return a.getToDate().compareTo(b.getToDate());
		}

	}

	public static class SortByDatePSAVERT implements Comparator<PSAVERT> {
		@Override
		public int compare(PSAVERT a, PSAVERT b) {
			return a.getDate().compareTo(b.getDate());
		}
	}

	public static class SortByDatePSAVERTCalculation implements Comparator<PSAVERTCalculation> {

		@Override
		public int compare(PSAVERTCalculation a, PSAVERTCalculation b) {
			return a.getToDate().compareTo(b.getToDate());
		}

	}
}
