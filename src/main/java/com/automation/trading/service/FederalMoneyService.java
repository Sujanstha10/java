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
import com.automation.trading.domain.calculation.BASECalculation;
import com.automation.trading.domain.calculation.M1Calculation;
import com.automation.trading.domain.calculation.M1VCalculation;
import com.automation.trading.domain.calculation.M2Calculation;
import com.automation.trading.domain.calculation.M2VCalculation;
import com.automation.trading.domain.fred.BASE;
import com.automation.trading.domain.fred.M1;
import com.automation.trading.domain.fred.M1V;
import com.automation.trading.domain.fred.M2;
import com.automation.trading.domain.fred.M2V;
import com.automation.trading.repository.BASERepository;
import com.automation.trading.repository.M1Repository;
import com.automation.trading.repository.M1VRepository;
import com.automation.trading.repository.M2Repository;
import com.automation.trading.repository.M2VRepository;
import com.automation.trading.utility.RestUtility;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FederalMoneyService {

	@Value("${quandl.host.url}")
	private String QUANDL_HOST_URL;

	@Value("${quandl.api.key.value}")
	private String QUANDL_API_KEY_VALUE;

	@Value("${quandl.api.key.name}")
	private String QUANDL_API_KEY_NAME;

	@Value("${quandl.data.format}")
	private String QUANDL_DATA_FORMAT;

	@Autowired
	private M2Repository m2Repository;

	@Autowired
	private M1Repository m1Repository;

	@Autowired
	private BASERepository baseRepository;

	@Autowired
	private RestUtility restUtility;

	@Autowired
	private M1VRepository m1vRepository;

	@Autowired
	private M2VRepository m2vRepository;

	public void saveBASEData() {
		String urlToFetch = QUANDL_HOST_URL + FederalReserveEconomicDataConstants.FEDERAL_FRED + "/" + FederalReserveEconomicDataConstants.FEDERAL_BASE + "/"
				+ QUANDL_DATA_FORMAT;
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(urlToFetch).queryParam(QUANDL_API_KEY_NAME,
				QUANDL_API_KEY_VALUE);
		List<BASE> baseList = new ArrayList<>();
		FederalResponse json = restUtility.consumeResponse(builder.toUriString());
		json.getDataset_data().getData().stream().forEach(o -> {
			ArrayList temp = (ArrayList) o;
			try {
				Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
				baseList.add(new BASE(date, Float.parseFloat(temp.get(1).toString())));
			} catch (ParseException e) {
				log.error(e.getMessage());
			}
		});

		Collections.sort(baseList, new SortByDateBASE());
		baseRepository.saveAll(baseList);

	}

	public void saveM1Data() {
		String urlToFetch = QUANDL_HOST_URL + FederalReserveEconomicDataConstants.FEDERAL_FRED + "/" + FederalReserveEconomicDataConstants.FEDERAL_M1 + "/"
				+ QUANDL_DATA_FORMAT;
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(urlToFetch).queryParam(QUANDL_API_KEY_NAME,
				QUANDL_API_KEY_VALUE);
		List<M1> m1List = new ArrayList<>();
		FederalResponse json = restUtility.consumeResponse(builder.toUriString());
		json.getDataset_data().getData().stream().forEach(o -> {
			ArrayList temp = (ArrayList) o;
			try {
				Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
				m1List.add(new M1(date, Float.parseFloat(temp.get(1).toString())));
			} catch (ParseException e) {
				log.error(e.getMessage());
			}
		});
		Collections.sort(m1List, new SortByDateM1());
		m1Repository.saveAll(m1List);
	}

	public void saveM2Data() {
		String urlToFetch = QUANDL_HOST_URL + FederalReserveEconomicDataConstants.FEDERAL_FRED + "/" + FederalReserveEconomicDataConstants.FEDERAL_M2 + "/"
				+ QUANDL_DATA_FORMAT;
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(urlToFetch).queryParam(QUANDL_API_KEY_NAME,
				QUANDL_API_KEY_VALUE);
		List<M2> m2List = new ArrayList<>();
		FederalResponse json = restUtility.consumeResponse(builder.toUriString());
		json.getDataset_data().getData().stream().forEach(o -> {
			ArrayList temp = (ArrayList) o;
			try {
				Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
				m2List.add(new M2(date, Float.parseFloat(temp.get(1).toString())));
			} catch (ParseException e) {
				log.error(e.getMessage());
			}
		});
		Collections.sort(m2List, new SortByDateM2());
		m2Repository.saveAll(m2List);
	}

	public void saveM1VData() {
		String urlToFetch = QUANDL_HOST_URL + FederalReserveEconomicDataConstants.FEDERAL_FRED + "/" + FederalReserveEconomicDataConstants.FEDERAL_M1V + "/"
				+ QUANDL_DATA_FORMAT;
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(urlToFetch).queryParam(QUANDL_API_KEY_NAME,
				QUANDL_API_KEY_VALUE);
		List<M1V> m1vList = new ArrayList<>();
		FederalResponse json = restUtility.consumeResponse(builder.toUriString());
		json.getDataset_data().getData().stream().forEach(o -> {
			ArrayList temp = (ArrayList) o;
			try {
				Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
				m1vList.add(new M1V(date, Float.parseFloat(temp.get(1).toString())));
			} catch (ParseException e) {
				log.error(e.getMessage());
			}
		});
		Collections.sort(m1vList, new SortByDateM1V());
		m1vRepository.saveAll(m1vList);
	}

	public void saveM2VData() {
		String urlToFetch = QUANDL_HOST_URL + FederalReserveEconomicDataConstants.FEDERAL_FRED + "/" + FederalReserveEconomicDataConstants.FEDERAL_M2V + "/"
				+ QUANDL_DATA_FORMAT;
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(urlToFetch).queryParam(QUANDL_API_KEY_NAME,
				QUANDL_API_KEY_VALUE);
		List<M2V> m2vList = new ArrayList<>();
		FederalResponse json = restUtility.consumeResponse(builder.toUriString());
		json.getDataset_data().getData().stream().forEach(o -> {
			ArrayList temp = (ArrayList) o;
			try {
				Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
				m2vList.add(new M2V(date, Float.parseFloat(temp.get(1).toString())));
			} catch (ParseException e) {
				log.error(e.getMessage());
			}
		});
		Collections.sort(m2vList, new SortByDateM2V());
		m2vRepository.saveAll(m2vList);
	}

	public static class SortByDateBASE implements Comparator<BASE> {
		@Override
		public int compare(BASE a, BASE b) {
			return a.getDate().compareTo(b.getDate());
		}
	}

	public static class SortByDateBASECalculation implements Comparator<BASECalculation> {
		@Override
		public int compare(BASECalculation a, BASECalculation b) {
			return a.getToDate().compareTo(b.getToDate());
		}
	}

	public static class SortByDateM1 implements Comparator<M1> {
		@Override
		public int compare(M1 a, M1 b) {
			return a.getDate().compareTo(b.getDate());
		}
	}

	public static class SortByDateM1Calculation implements Comparator<M1Calculation> {
		@Override
		public int compare(M1Calculation a, M1Calculation b) {
			return a.getToDate().compareTo(b.getToDate());
		}
	}

	public static class SortByDateM2 implements Comparator<M2> {
		@Override
		public int compare(M2 a, M2 b) {
			return a.getDate().compareTo(b.getDate());
		}
	}

	public static class SortByDateM2Calculation implements Comparator<M2Calculation> {
		@Override
		public int compare(M2Calculation a, M2Calculation b) {
			return a.getToDate().compareTo(b.getToDate());
		}
	}

	public static class SortByDateM1V implements Comparator<M1V> {
		@Override
		public int compare(M1V a, M1V b) {
			return a.getDate().compareTo(b.getDate());
		}
	}

	public static class SortByDateM1VCalculation implements Comparator<M1VCalculation> {

		@Override
		public int compare(M1VCalculation a, M1VCalculation b) {
			return a.getToDate().compareTo(b.getToDate());
		}

	}

	public static class SortByDateM2V implements Comparator<M2V> {
		@Override
		public int compare(M2V a, M2V b) {
			return a.getDate().compareTo(b.getDate());
		}
	}

	public static class SortByDateM2VCalculation implements Comparator<M2VCalculation> {

		@Override
		public int compare(M2VCalculation a, M2VCalculation b) {
			return a.getToDate().compareTo(b.getToDate());
		}

	}

}
