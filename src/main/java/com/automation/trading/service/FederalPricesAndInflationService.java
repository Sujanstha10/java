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
import com.automation.trading.domain.calculation.GDPDEFCalculation;
import com.automation.trading.domain.fred.CPIAUCSL;
import com.automation.trading.domain.fred.CPILFESL;
import com.automation.trading.domain.fred.GDPDEF;
import com.automation.trading.repository.CPIAUCSLRepository;
import com.automation.trading.repository.CPILFESLRepository;
import com.automation.trading.repository.GDPDEFRepository;
import com.automation.trading.utility.RestUtility;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FederalPricesAndInflationService {

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
	private CPIAUCSLRepository cpiaucslRepository;

	@Autowired
	private CPILFESLRepository cpilfeslRepository;

	@Autowired
	private RestUtility restUtility;

	public void saveCPIFAUCData() {
		String urlToFetch = QUANDL_HOST_URL + FederalReserveEconomicDataConstants.FEDERAL_FRED + "/" + FederalReserveEconomicDataConstants.FEDERAL_CPIAUCSL
				+ "/" + QUANDL_DATA_FORMAT;
		List<CPIAUCSL> cpiaucslList = new ArrayList<>();
		FederalResponse json = restUtility.consumeResponse(urlToFetch);
		json.getDataset_data().getData().stream().forEach(o -> {
			ArrayList temp = (ArrayList) o;
			try {
				Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
				cpiaucslList.add(new CPIAUCSL(date, Float.parseFloat(temp.get(1).toString())));
			} catch (ParseException e) {
				log.error(e.getMessage());
			}
		});
		cpiaucslRepository.saveAll(cpiaucslList);

	}

	public void saveCPILFESLData() {
		String urlToFetch = QUANDL_HOST_URL + FederalReserveEconomicDataConstants.FEDERAL_FRED + "/" + FederalReserveEconomicDataConstants.FEDERAL_CPILFESL
				+ "/" + QUANDL_DATA_FORMAT;
		List<CPILFESL> cpilfeslList = new ArrayList<>();
		FederalResponse json = restUtility.consumeResponse(urlToFetch);
		json.getDataset_data().getData().stream().forEach(o -> {
			ArrayList temp = (ArrayList) o;
			try {
				Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
				cpilfeslList.add(new CPILFESL(date, Float.parseFloat(temp.get(1).toString())));
			} catch (ParseException e) {
				log.error(e.getMessage());
			}
		});
		cpilfeslRepository.saveAll(cpilfeslList);
	}

	public void saveGDPDEFData() {
		String urlToFetch = QUANDL_HOST_URL + FederalReserveEconomicDataConstants.FEDERAL_FRED + "/" + FederalReserveEconomicDataConstants.FEDERAL_GDPDEF
				+ "/" + QUANDL_DATA_FORMAT;
		
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(urlToFetch).queryParam(QUANDL_API_KEY_NAME,
				QUANDL_API_KEY_VALUE);
		List<GDPDEF> gdpdefList = new ArrayList<>();

		FederalResponse json = restUtility.consumeResponse(builder.toUriString());
		json.getDataset_data().getData().stream().forEach(o -> {
			ArrayList temp = (ArrayList) o;
			try {
				Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
				gdpdefList.add(new GDPDEF(date, Float.parseFloat(temp.get(1).toString())));
			} catch (ParseException e) {
				log.error(e.getMessage());
			}
		});
		
		Collections.sort(gdpdefList, new SortByDateGDPDEF());
		gdpdefRepository.saveAll(gdpdefList);
	}

	public static class SortByDateGDPDEF implements Comparator<GDPDEF> {
		@Override
		public int compare(GDPDEF a, GDPDEF b) {
			return a.getDate().compareTo(b.getDate());
		}
	}

	public static class SortByDateGDPDEFCalculation implements Comparator<GDPDEFCalculation> {

		@Override
		public int compare(GDPDEFCalculation a, GDPDEFCalculation b) {
			return a.getToDate().compareTo(b.getToDate());
		}
	}

}
