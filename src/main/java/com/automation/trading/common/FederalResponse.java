package com.automation.trading.common;

import com.automation.trading.domain.fred.DFFHelper;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class FederalResponse {

	@JsonProperty
	private DFFHelper dataset_data;

}
