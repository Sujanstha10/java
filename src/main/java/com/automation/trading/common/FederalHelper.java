package com.automation.trading.common;

import java.util.ArrayList;

import lombok.Data;

@Data
public class FederalHelper {

	private String start_date;

	private String end_date;

	private String frequency;

	private ArrayList<Object> data = new ArrayList<>();

}
