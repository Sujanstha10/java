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
import com.automation.trading.domain.calculation.DPRIMECalculation;
import com.automation.trading.domain.calculation.T10YIECalculation;
import com.automation.trading.domain.calculation.T5YIECalculation;
import com.automation.trading.domain.calculation.TEDRATECalculation;
import com.automation.trading.domain.fred.DPRIME;
import com.automation.trading.domain.fred.interestrates.DGS10;
import com.automation.trading.domain.fred.interestrates.DGS30;
import com.automation.trading.domain.fred.interestrates.DGS5;
import com.automation.trading.domain.fred.interestrates.DTB3;
import com.automation.trading.domain.fred.interestrates.T10YIE;
import com.automation.trading.domain.fred.interestrates.T5YIE;
import com.automation.trading.domain.fred.interestrates.T5YIFR;
import com.automation.trading.domain.fred.interestrates.TEDRATE;
import com.automation.trading.repository.DGS10Repository;
import com.automation.trading.repository.DGS30Repository;
import com.automation.trading.repository.DGS5Repository;
import com.automation.trading.repository.DPRIMERepository;
import com.automation.trading.repository.DTBRepository;
import com.automation.trading.repository.T10YIERepository;
import com.automation.trading.repository.T5YIERepository;
import com.automation.trading.repository.T5YIFRRepository;
import com.automation.trading.repository.TEDRATERepository;
import com.automation.trading.utility.RestUtility;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FederalInterestRateService {

	@Autowired
	private RestUtility restUtility;

	@Autowired
	private DTBRepository dtbRepository;

	@Autowired
	private DGS5Repository dgs5Repository;

	@Autowired
	private DGS10Repository dgs10Repository;

	@Autowired
	private DGS30Repository dgs30Repository;

	@Autowired
	private T5YIERepository t5yieRepository;

	@Autowired
	private T10YIERepository t10yieRepository;

	@Autowired
	private T5YIFRRepository t5yifrRepository;

	@Autowired
	private TEDRATERepository tedrateRepository;

	@Autowired
	private DPRIMERepository dprimeRepository;

	@Value("${quandl.host.url}")
	private String QUANDL_HOST_URL;

	@Value("${quandl.api.key.value}")
	private String QUANDL_API_KEY_VALUE;

	@Value("${quandl.api.key.name}")
	private String QUANDL_API_KEY_NAME;

	@Value("${quandl.data.format}")
	private String QUANDL_DATA_FORMAT;

	public void saveDTB3Data() {
		String urlToFetch = QUANDL_HOST_URL + FederalReserveEconomicDataConstants.FEDERAL_FRED + "/" + FederalReserveEconomicDataConstants.FEDERAL_DTB3 + "/"
				+ QUANDL_DATA_FORMAT;
		List<DTB3> dtb3List = new ArrayList<>();
		FederalResponse json = restUtility.consumeResponse(urlToFetch);
		json.getDataset_data().getData().stream().forEach(o -> {
			ArrayList temp = (ArrayList) o;
			try {
				Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
				dtb3List.add(new DTB3(date, Float.parseFloat(temp.get(1).toString())));
			} catch (ParseException e) {
				log.error(e.getMessage());
			}
		});
		dtbRepository.saveAll(dtb3List);

	}

	public void saveDGS5Data() {
		String urlToFetch = QUANDL_HOST_URL + FederalReserveEconomicDataConstants.FEDERAL_FRED + "/" + FederalReserveEconomicDataConstants.FEDERAL_DGS5 + "/"
				+ QUANDL_DATA_FORMAT;
		List<DGS5> dgs5List = new ArrayList<>();
		FederalResponse json = restUtility.consumeResponse(urlToFetch);
		json.getDataset_data().getData().stream().forEach(o -> {
			ArrayList temp = (ArrayList) o;
			try {
				Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
				dgs5List.add(new DGS5(date, Float.parseFloat(temp.get(1).toString())));
			} catch (ParseException e) {
				log.error(e.getMessage());
			}
		});
		Collections.sort(dgs5List, new SortByDateDGS5());
		dgs5Repository.saveAll(dgs5List);

	}

	public void saveDGS10Data() {
		String urlToFetch = QUANDL_HOST_URL + FederalReserveEconomicDataConstants.FEDERAL_FRED + "/" + FederalReserveEconomicDataConstants.FEDERAL_DGS10 + "/"
				+ QUANDL_DATA_FORMAT;
		List<DGS10> dgs10List = new ArrayList<>();
		FederalResponse json = restUtility.consumeResponse(urlToFetch);
		json.getDataset_data().getData().stream().forEach(o -> {
			ArrayList temp = (ArrayList) o;
			try {
				Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
				dgs10List.add(new DGS10(date, Float.parseFloat(temp.get(1).toString())));
			} catch (ParseException e) {
				log.error(e.getMessage());
			}
		});
		Collections.sort(dgs10List, new FederalReserveService.SortByDateDGS10());
		dgs10Repository.saveAll(dgs10List);
		log.info("DGS 10 data saved");

	}

	public void saveDGS30Data() {
		String urlToFetch = QUANDL_HOST_URL + FederalReserveEconomicDataConstants.FEDERAL_FRED + "/" + FederalReserveEconomicDataConstants.FEDERAL_DGS30 + "/"
				+ QUANDL_DATA_FORMAT;

		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(urlToFetch).queryParam(QUANDL_API_KEY_NAME,
				QUANDL_API_KEY_VALUE);
		List<DGS30> dgs30List = new ArrayList<>();
		FederalResponse json = restUtility.consumeResponse(builder.toUriString());
		json.getDataset_data().getData().stream().forEach(o -> {
			ArrayList temp = (ArrayList) o;
			try {
				Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
				dgs30List.add(new DGS30(date, Float.parseFloat(temp.get(1).toString())));
			} catch (ParseException e) {
				log.error(e.getMessage());
			}
		});
		Collections.sort(dgs30List, new FederalReserveService.SortByDateDGS30());
		dgs30Repository.saveAll(dgs30List);
	}

	public void saveT5YIEData() {
		String urlToFetch = QUANDL_HOST_URL + FederalReserveEconomicDataConstants.FEDERAL_FRED + "/" + FederalReserveEconomicDataConstants.FEDERAL_T5YIE + "/"
				+ QUANDL_DATA_FORMAT;
		List<T5YIE> t5yieList = new ArrayList<>();
		FederalResponse json = restUtility.consumeResponse(urlToFetch);
		json.getDataset_data().getData().stream().forEach(o -> {
			ArrayList temp = (ArrayList) o;
			try {
				Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
				t5yieList.add(new T5YIE(date, Float.parseFloat(temp.get(1).toString())));
			} catch (ParseException e) {
				log.error(e.getMessage());
			}
		});
		Collections.sort(t5yieList, new SortByDateT5YIE());
		t5yieRepository.saveAll(t5yieList);
	}

	public void saveT10YIEData() {
		String urlToFetch = QUANDL_HOST_URL + FederalReserveEconomicDataConstants.FEDERAL_FRED + "/" + FederalReserveEconomicDataConstants.FEDERAL_T10YIE
				+ "/" + QUANDL_DATA_FORMAT;
		List<T10YIE> t10yieList = new ArrayList<>();
		FederalResponse json = restUtility.consumeResponse(urlToFetch);
		json.getDataset_data().getData().stream().forEach(o -> {
			ArrayList temp = (ArrayList) o;
			try {
				Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
				t10yieList.add(new T10YIE(date, Float.parseFloat(temp.get(1).toString())));
			} catch (ParseException e) {
				log.error(e.getMessage());
			}
		});
		Collections.sort(t10yieList, new SortByDateT10YIE());
		t10yieRepository.saveAll(t10yieList);
	}

	public void saveT5YIFRData() {
		String urlToFetch = QUANDL_HOST_URL + FederalReserveEconomicDataConstants.FEDERAL_FRED + "/" + FederalReserveEconomicDataConstants.FEDERAL_T5YIFR
				+ "/" + QUANDL_DATA_FORMAT;
		List<T5YIFR> t5yifrList = new ArrayList<>();
		FederalResponse json = restUtility.consumeResponse(urlToFetch);
		json.getDataset_data().getData().stream().forEach(o -> {
			ArrayList temp = (ArrayList) o;
			try {
				Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
				t5yifrList.add(new T5YIFR(date, Float.parseFloat(temp.get(1).toString())));
			} catch (ParseException e) {
				log.error(e.getMessage());
			}
		});
		t5yifrRepository.saveAll(t5yifrList);
	}

	public void saveTEDRATEData() {
		String urlToFetch = QUANDL_HOST_URL + FederalReserveEconomicDataConstants.FEDERAL_FRED + "/" + FederalReserveEconomicDataConstants.FEDERAL_TEDRATE
				+ "/" + QUANDL_DATA_FORMAT;
		List<TEDRATE> tedrateList = new ArrayList<>();
		FederalResponse json = restUtility.consumeResponse(urlToFetch);
		json.getDataset_data().getData().stream().forEach(o -> {
			ArrayList temp = (ArrayList) o;
			try {
				Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
				tedrateList.add(new TEDRATE(date, Float.parseFloat(temp.get(1).toString())));
			} catch (ParseException e) {
				log.error(e.getMessage());
			}
		});
		Collections.sort(tedrateList, new SortByDateTEDRATE());
		tedrateRepository.saveAll(tedrateList);
	}

	public void saveDPRIMEData() {
		String urlToFetch = QUANDL_HOST_URL + FederalReserveEconomicDataConstants.FEDERAL_FRED + "/" + FederalReserveEconomicDataConstants.FEDERAL_DPRIME
				+ "/" + QUANDL_DATA_FORMAT;
		List<DPRIME> dprimeList = new ArrayList<>();
		FederalResponse json = restUtility.consumeResponse(urlToFetch);
		json.getDataset_data().getData().stream().forEach(o -> {
			ArrayList temp = (ArrayList) o;
			try {
				Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
				dprimeList.add(new DPRIME(date, Float.parseFloat(temp.get(1).toString())));
			} catch (ParseException e) {
				log.error(e.getMessage());
			}
		});
		Collections.sort(dprimeList, new SortByDateDPRIME());
		dprimeRepository.saveAll(dprimeList);
	}

	public static class SortByDateDGS5 implements Comparator<DGS5> {
		@Override
		public int compare(DGS5 a, DGS5 b) {
			return a.getDate().compareTo(b.getDate());
		}
	}

	public static class SortByDateDGS10 implements Comparator<DGS10> {
		@Override
		public int compare(DGS10 a, DGS10 b) {
			return a.getDate().compareTo(b.getDate());
		}
	}

	public static class SortByDateT5YIE implements Comparator<T5YIE> {
		@Override
		public int compare(T5YIE a, T5YIE b) {
			return a.getDate().compareTo(b.getDate());
		}
	}

	public static class SortByDateT5YIECalculation implements Comparator<T5YIECalculation> {

		@Override
		public int compare(T5YIECalculation a, T5YIECalculation b) {
			return a.getToDate().compareTo(b.getToDate());
		}

	}

	public static class SortByDateT10YIE implements Comparator<T10YIE> {
		@Override
		public int compare(T10YIE a, T10YIE b) {
			return a.getDate().compareTo(b.getDate());
		}
	}

	public static class SortByDateT10YIECalculation implements Comparator<T10YIECalculation> {

		@Override
		public int compare(T10YIECalculation a, T10YIECalculation b) {
			return a.getToDate().compareTo(b.getToDate());
		}

	}

	public static class SortByDateTEDRATE implements Comparator<TEDRATE> {
		@Override
		public int compare(TEDRATE a, TEDRATE b) {
			return a.getDate().compareTo(b.getDate());
		}
	}

	public static class SortByDateTEDRATECalculation implements Comparator<TEDRATECalculation> {

		@Override
		public int compare(TEDRATECalculation a, TEDRATECalculation b) {
			return a.getToDate().compareTo(b.getToDate());
		}

	}

	public static class SortByDateDPRIME implements Comparator<DPRIME> {
		@Override
		public int compare(DPRIME a, DPRIME b) {
			return a.getDate().compareTo(b.getDate());
		}
	}

	public static class SortByDateDPRIMECalculation implements Comparator<DPRIMECalculation> {

		@Override
		public int compare(DPRIMECalculation a, DPRIMECalculation b) {
			return a.getToDate().compareTo(b.getToDate());
		}

	}

}
