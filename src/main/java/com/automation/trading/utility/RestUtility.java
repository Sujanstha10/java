package com.automation.trading.utility;

import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.automation.trading.common.FederalResponse;

@Component
public class RestUtility {

	@Autowired
	private RestTemplate restTemplate;

	@Value("${quandl.host.url}")
	private String QUANDL_HOST_URL;

	@Value("${quandl.api.key.value}")
	private String QUANDL_API_KEY_VALUE;

	@Value("${quandl.api.key.name}")
	private String QUANDL_API_KEY_NAME;

	@Value("${quandl.data.format}")
	private String QUANDL_DATA_FORMAT;

	public FederalResponse consumeResponse(String urlToFetch) {

		HashMap<String, String> apiKeyMap = new HashMap<>();
		HttpHeaders headers = new HttpHeaders();
		headers.add("Accept", MediaType.APPLICATION_JSON.toString());
		headers.add("Content-Type", MediaType.APPLICATION_JSON.toString());
		headers.add("Cache-Control", "no-cache");
		HttpEntity entity = new HttpEntity(apiKeyMap, headers);
		FederalResponse json = restTemplate.exchange(urlToFetch, HttpMethod.GET, entity, FederalResponse.class)
				.getBody();
		return json;

	}

}
