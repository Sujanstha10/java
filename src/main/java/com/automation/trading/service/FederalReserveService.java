package com.automation.trading.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.automation.trading.domain.calculation.*;
import com.automation.trading.domain.fred.*;
import com.automation.trading.domain.fred.interestrates.DGS10;
import com.automation.trading.domain.fred.interestrates.DGS30;
import com.automation.trading.domain.fred.interestrates.DGS5;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import com.automation.trading.common.FederalResponse;
import com.automation.trading.constants.FederalReserveEconomicDataConstants;

import com.automation.trading.domain.calculation.CPIAUCSLCalculation;
import com.automation.trading.domain.calculation.DffCalculation;
import com.automation.trading.domain.calculation.GDPC1Calculation;
import com.automation.trading.domain.calculation.GDPPOTCalculation;
import com.automation.trading.domain.calculation.GdpCalculation;
import com.automation.trading.domain.calculation.UnRateCalculation;

import com.automation.trading.repository.CPIAUCSLRepository;
import com.automation.trading.repository.CPILFESLRepository;
import com.automation.trading.repository.DFFRepository;
import com.automation.trading.repository.GDPC1Repository;
import com.automation.trading.repository.GDPPOTRepository;
import com.automation.trading.repository.GdpRepository;
import com.automation.trading.repository.UnRateRepostiory;
import com.automation.trading.utility.RestUtility;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FederalReserveService {

	@Autowired
    RestUtility restUtility;

	@Autowired
	private DFFRepository dffRepository;

	@Autowired
	private UnRateRepostiory unRateRepostiory;

	@Autowired
	private GdpRepository gdpRepository;

	@Autowired
	private GDPC1Repository gdpc1Repository;

	@Autowired
	private GDPPOTRepository gdppotRepository;

	@Autowired
	private CPIAUCSLRepository cpiaucslRepository;
	@Autowired
	private CPILFESLRepository cpilfeslRepository;

	@Value("${quandl.host.url}")
	private String QUANDL_HOST_URL;

	@Value("${quandl.api.key.value}")
	private String QUANDL_API_KEY_VALUE;

	@Value("${quandl.api.key.name}")
	private String QUANDL_API_KEY_NAME;

	@Value("${quandl.data.format}")
	private String QUANDL_DATA_FORMAT;

	private Logger logger = LoggerFactory.getLogger(FederalReserveService.class);

	@Async
	public void saveDFFData() {

		String urlToFetch = QUANDL_HOST_URL + FederalReserveEconomicDataConstants.FEDERAL_FRED + "/" + FederalReserveEconomicDataConstants.FEDERAL_DFF + "/"
				+ QUANDL_DATA_FORMAT;
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(urlToFetch).queryParam(QUANDL_API_KEY_NAME,
				QUANDL_API_KEY_VALUE);

		FederalResponse json = restUtility.consumeResponse(builder.toUriString());
		List<DFF> dffList = new ArrayList<>();
		json.getDataset_data().getData().stream().forEach(o -> {
			ArrayList temp = (ArrayList) o;
			try {
				Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
				Float currentValue = Float.parseFloat(temp.get(1).toString());
				dffList.add(new DFF(date, currentValue));
			} catch (ParseException e) {
				logger.error(e.getMessage());
			}
		});
		Collections.sort(dffList, new SortByDateDff());
		dffRepository.saveAll(dffList);

	}

	@Async
	public void saveUnRateData() {
		String urlToFetch = QUANDL_HOST_URL + FederalReserveEconomicDataConstants.FEDERAL_FRED + "/" + FederalReserveEconomicDataConstants.FEDERAL_UNRATE
				+ "/" + QUANDL_DATA_FORMAT;

		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(urlToFetch).queryParam(QUANDL_API_KEY_NAME,
				QUANDL_API_KEY_VALUE);
		FederalResponse json = restUtility.consumeResponse(builder.toUriString());

		List<UnRate> unRateList = new ArrayList<>();
		json.getDataset_data().getData().stream().forEach(o -> {
			ArrayList temp = (ArrayList) o;
			try {
				Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
				unRateList.add(new UnRate(date, Float.parseFloat(temp.get(1).toString())));
			} catch (ParseException e) {
				logger.error(e.getMessage());
			}
		});
		Collections.sort(unRateList, new SortByDateUnrate());
		unRateRepostiory.saveAll(unRateList);
	}

	@Async
	public void saveGdpData() {
		String urlToFetch = QUANDL_HOST_URL + FederalReserveEconomicDataConstants.FEDERAL_FRED + "/" + FederalReserveEconomicDataConstants.FEDERAL_GDP + "/"
				+ QUANDL_DATA_FORMAT;
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(urlToFetch).queryParam(QUANDL_API_KEY_NAME,
				QUANDL_API_KEY_VALUE);
		List<Gdp> gdpList = new ArrayList<>();
		FederalResponse json = restUtility.consumeResponse(builder.toUriString());

		json.getDataset_data().getData().stream().forEach(o -> {
			ArrayList temp = (ArrayList) o;
			try {
				Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
				gdpList.add(new Gdp(date, Float.parseFloat(temp.get(1).toString())));
			} catch (ParseException e) {
				logger.error(e.getMessage());
			}
		});
		Collections.sort(gdpList, new SortByDateGdp());
		gdpRepository.saveAll(gdpList);

	}

	@Async
	public void saveGDPC1Data() {
		String urlToFetch = QUANDL_HOST_URL + FederalReserveEconomicDataConstants.FEDERAL_FRED + "/" + FederalReserveEconomicDataConstants.FEDERAL_GDPC1 + "/"
				+ QUANDL_DATA_FORMAT;
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(urlToFetch).queryParam(QUANDL_API_KEY_NAME,
				QUANDL_API_KEY_VALUE);
		List<GDPC1> gdpc1List = new ArrayList<>();
		FederalResponse json = restUtility.consumeResponse(builder.toUriString());

		json.getDataset_data().getData().stream().forEach(o -> {
			ArrayList temp = (ArrayList) o;
			try {
				Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
				gdpc1List.add(new GDPC1(date, Float.parseFloat(temp.get(1).toString())));
			} catch (ParseException e) {
				logger.error(e.getMessage());
			}
		});
		Collections.sort(gdpc1List, new SortByDateGDPC1());
		gdpc1Repository.saveAll(gdpc1List);

	}

	@Async
	public void saveGDPPOTData() {
		String urlToFetch = QUANDL_HOST_URL + FederalReserveEconomicDataConstants.FEDERAL_FRED + "/" + FederalReserveEconomicDataConstants.FEDERAL_GDPPOT
				+ "/" + QUANDL_DATA_FORMAT;
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(urlToFetch).queryParam(QUANDL_API_KEY_NAME,
				QUANDL_API_KEY_VALUE);
		List<GDPPOT> gdppotList = new ArrayList<>();
		FederalResponse json = restUtility.consumeResponse(builder.toUriString());
		json.getDataset_data().getData().stream().forEach(o -> {
			ArrayList temp = (ArrayList) o;
			try {
				Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
				gdppotList.add(new GDPPOT(date, Float.parseFloat(temp.get(1).toString())));
			} catch (ParseException e) {
				logger.error(e.getMessage());
			}
		});
		Collections.sort(gdppotList, new SortByDateGDPPOT());
		gdppotRepository.saveAll(gdppotList);

	}

	@Async
	public void saveCPIAUCSLData() {
		String urlToFetch = QUANDL_HOST_URL + FederalReserveEconomicDataConstants.FEDERAL_FRED + "/" + FederalReserveEconomicDataConstants.FEDERAL_CPIAUCSL
				+ "/" + QUANDL_DATA_FORMAT;
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(urlToFetch).queryParam(QUANDL_API_KEY_NAME,
				QUANDL_API_KEY_VALUE);
		List<CPIAUCSL> cpiaucslList = new ArrayList<>();
		FederalResponse json = restUtility.consumeResponse(builder.toUriString());
		json.getDataset_data().getData().stream().forEach(o -> {
			ArrayList temp = (ArrayList) o;
			try {
				Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
				cpiaucslList.add(new CPIAUCSL(date, Float.parseFloat(temp.get(1).toString())));
			} catch (ParseException e) {
				logger.error(e.getMessage());
			}
		});
		Collections.sort(cpiaucslList, new SortByDateCPIAUCSL());
		cpiaucslRepository.saveAll(cpiaucslList);

	}

	@Async
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
				logger.error(e.getMessage());
			}
		});
		cpiaucslRepository.saveAll(cpiaucslList);

	}

	@Async
	public void saveCPILFESLData() {
		String urlToFetch = QUANDL_HOST_URL + FederalReserveEconomicDataConstants.FEDERAL_FRED + "/" + FederalReserveEconomicDataConstants.FEDERAL_CPILFESL
				+ "/" + QUANDL_DATA_FORMAT;

		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(urlToFetch).queryParam(QUANDL_API_KEY_NAME,
				QUANDL_API_KEY_VALUE);
		List<CPILFESL> cpilfeslList = new ArrayList<>();
		FederalResponse json = restUtility.consumeResponse(builder.toUriString());
		json.getDataset_data().getData().stream().forEach(o -> {
			ArrayList temp = (ArrayList) o;
			try {
				Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
				cpilfeslList.add(new CPILFESL(date, Float.parseFloat(temp.get(1).toString())));
			} catch (ParseException e) {
				logger.error(e.getMessage());
			}
		});

		Collections.sort(cpilfeslList, new SortByDateCPILFESL());
		cpilfeslRepository.saveAll(cpilfeslList);

	}

	public static class SortByDateDff implements Comparator<DFF> {
		@Override
		public int compare(DFF a, DFF b) {
			return a.getDate().compareTo(b.getDate());
		}
	}

	public static class SortByDateDffCalculation implements Comparator<DffCalculation> {
		@Override
		public int compare(DffCalculation a, DffCalculation b) {
			return a.getToDate().compareTo(b.getToDate());
		}
	}

	public static class SortByDateUnrate implements Comparator<UnRate> {
		@Override
		public int compare(UnRate a, UnRate b) {
			return a.getDate().compareTo(b.getDate());
		}
	}

	public static class SortByDateUnRateCalculation implements Comparator<UnRateCalculation> {
		@Override
		public int compare(UnRateCalculation a, UnRateCalculation b) {
			return a.getToDate().compareTo(b.getToDate());
		}
	}

	public static class SortByDateGdp implements Comparator<Gdp> {
		@Override
		public int compare(Gdp a, Gdp b) {
			return a.getDate().compareTo(b.getDate());
		}
	}

	public static class SortByDateGdpCalculation implements Comparator<GdpCalculation> {
		@Override
		public int compare(GdpCalculation a, GdpCalculation b) {
			return a.getToDate().compareTo(b.getToDate());
		}
	}

	public static class SortByDateGDPC1 implements Comparator<GDPC1> {
		@Override
		public int compare(GDPC1 a, GDPC1 b) {
			return a.getDate().compareTo(b.getDate());
		}
	}

	public static class SortByDateGDPC1Calculation implements Comparator<GDPC1Calculation> {
		@Override
		public int compare(GDPC1Calculation a, GDPC1Calculation b) {
			return a.getToDate().compareTo(b.getToDate());
		}
	}

	public static class SortByDateCPIAUCSL implements Comparator<CPIAUCSL> {
		@Override
		public int compare(CPIAUCSL a, CPIAUCSL b) {
			return a.getDate().compareTo(b.getDate());
		}
	}

	public static class SortByDateGDPPOT implements Comparator<GDPPOT> {
		@Override
		public int compare(GDPPOT a, GDPPOT b) {
			return a.getDate().compareTo(b.getDate());
		}
	}

	public static class SortByDateGDPPOTCalculation implements Comparator<GDPPOTCalculation> {
		@Override
		public int compare(GDPPOTCalculation a, GDPPOTCalculation b) {
			return a.getToDate().compareTo(b.getToDate());
		}
	}

	public static class SortByDateCPILFESL implements Comparator<CPILFESL> {
		@Override
		public int compare(CPILFESL a, CPILFESL b) {
			return a.getDate().compareTo(b.getDate());
		}
	}

	public static class SortByDateDGS5 implements Comparator<DGS5> {
		@Override
		public int compare(DGS5 a, DGS5 b) {
			return a.getDate().compareTo(b.getDate());
		}
	}

	public static class SortByDateDGS5Calculation implements Comparator<DGS5Calculation> {
		@Override
		public int compare(DGS5Calculation a, DGS5Calculation b) {
			return a.getToDate().compareTo(b.getToDate());
		}
	}

	public static class SortByDateDGS10 implements Comparator<DGS10> {
		@Override
		public int compare(DGS10 a, DGS10 b) {
			return a.getDate().compareTo(b.getDate());
		}
	}

	public static class SortByDateDGS10Calculation implements Comparator<DGS10Calculation> {
		@Override
		public int compare(DGS10Calculation a, DGS10Calculation b) {
			return a.getToDate().compareTo(b.getToDate());
		}
	}

	public static class SortByDateDGS30 implements Comparator<DGS30> {
		@Override
		public int compare(DGS30 a, DGS30 b) {
			return a.getDate().compareTo(b.getDate());
		}
	}

	public static class SortByDateDGS30Calculation implements Comparator<DGS30Calculation> {
		@Override
		public int compare(DGS30Calculation a, DGS30Calculation b) {
			return a.getToDate().compareTo(b.getToDate());
		}
	}

	public static class SortByDateCPIAUCSLCalculation implements Comparator<CPIAUCSLCalculation> {
		@Override
		public int compare(CPIAUCSLCalculation a, CPIAUCSLCalculation b) {
			return a.getToDate().compareTo(b.getToDate());
		}
	}

}
