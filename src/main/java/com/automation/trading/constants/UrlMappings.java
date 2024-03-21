package com.automation.trading.constants;

public class UrlMappings {

	private UrlMappings() {
		throw new IllegalStateException("UrlMapping class");
	}

	// Base Url
	public static final String BASE_URL = "/api/v1";

	// FederalReserve
	public static final String FEDERAL_GDP = BASE_URL + "/gdp";
	public static final String FEDERAL_DFF = BASE_URL + "/dff";
	public static final String FEDERAL_UNRATE = BASE_URL + "/unrate";

	// RateOfChange
	public static final String RATE_OF_CHANGE_GDP = BASE_URL + "/roc-gdp";
	public static final String RATE_OF_CHANGE_DFF = BASE_URL + "/roc-dff";
	public static final String RATE_OF_CHANGE_UNRATE = BASE_URL + "/roc-unrate";
	public static final String ROLLING_AVG_THREE_MONTH = BASE_URL + "/roll-avg-three";
	public static final String ROLLING_AVG_THREE_MONTH_DFF = BASE_URL + "/roll-avg-three-dff";
	public static final String ROLLING_AVG_THREE_MONTH_UNRATE = BASE_URL + "/roll-avg-three-unrate";
	public static final String ROC_ANNUAL_ROLLING_AVG_GDP = BASE_URL + "/roc-ann-roll-avg-gdp";
	public static final String ROC_ANNUAL_ROLLING_AVG_DFF = BASE_URL + "/roc-ann-roll-avg-dff";
	public static final String ROC_ANNUAL_ROLLING_AVG_UNRATE = BASE_URL + "/roc-ann-roll-avg-unrate";

	// SignOfChnage
	public static final String RATE_OF_CHANGE_SIGN_GDP = BASE_URL + "/roc-sign-gdp";
	public static final String RATE_OF_CHANGE_SIGN_DFF = BASE_URL + "/roc-sign-dff";
	public static final String RATE_OF_CHANGE_SIGN_UNRATE = BASE_URL + "/roc-sign-unrate";

	// UPDATED ROW
	public static final String UPDATED_GDP = BASE_URL + "/updated-gdp";
	public static final String UPDATED_DFF = BASE_URL + "/updated-dff";
	public static final String UPDATED_UNRATE = BASE_URL + "/updated-unrate";

	// Url Mappings for GDPC1
	public static final String FEDERAL_GDPC1 = BASE_URL + "/gdpc1";
	public static final String RATE_OF_CHANGE_GDPC1 = FEDERAL_GDPC1 + "/roc";
	public static final String ROLLING_AVG_THREE_GDPC1 = FEDERAL_GDPC1 + "/roll-avg-three";
	public static final String ROC_ANNUAL_ROLLING_AVG_GDPC1 = FEDERAL_GDPC1 + "/roc-ann-roll-avg-gdpc1";
	public static final String RATE_OF_CHANGE_SIGN_GDPC1 = FEDERAL_GDPC1 + "/roc-sign-base";

	// Url Mapping for GDPPOT
	public static final String FEDERAL_GDPPOT = BASE_URL + "/gdppot";
	public static final String RATE_OF_CHANGE_GDPPOT = FEDERAL_GDPPOT + "/roc";
	public static final String ROLLING_AVG_THREE_GDPPOT = FEDERAL_GDPPOT + "/roll-avg-three";
	public static final String ROC_ANNUAL_ROLLING_AVG_GDPPOT = FEDERAL_GDPPOT + "/roc-ann-roll-avg-gdppot";
	public static final String RATE_OF_CHANGE_SIGN_GDPPOT = FEDERAL_GDPPOT + "/roc-sign-gdppot";

	// Url Mappings for Prices and Inflation
	public static final String FEDERAL_GDPDEF = BASE_URL + "/gdpdef";
	public static final String RATE_OF_CHANGE_GDPDEF = FEDERAL_GDPDEF + "/roc";
	public static final String ROLLING_AVG_THREE_GDPDEF = FEDERAL_GDPDEF + "/roll-avg-three";
	public static final String ROC_ANNUAL_ROLLING_AVG_GDPDEF = FEDERAL_GDPDEF + "/roc-ann-roll-avg-gdpdef";
	public static final String RATE_OF_CHANGE_SIGN_GDPDEF = FEDERAL_GDPDEF + "roc-sign-gdpdef";

	// Url Mappings for BASE
	public static final String FEDERAL_BASE = BASE_URL + "/base";
	public static final String RATE_OF_CHANGE_BASE = FEDERAL_BASE + "/roc";
	public static final String ROLLING_AVG_THREE_BASE = FEDERAL_BASE + "/roll-avg-three";
	public static final String ROC_ANNUAL_ROLLING_AVG_BASE = FEDERAL_BASE + "/roc-ann-roll-avg-base";
	public static final String RATE_OF_CHANGE_SIGN_BASE = FEDERAL_BASE + "/roc-sign-base";

	// Url Mappings for CPIAUCSL
	public static final String FEDERAL_CPIAUCSL = BASE_URL + "/cpiaucsl";
	public static final String RATE_OF_CHANGE_CPIAUCSL = FEDERAL_CPIAUCSL + "/roc";
	public static final String ROLLING_AVG_THREE_CPIAUCSL = FEDERAL_CPIAUCSL + "/roll-avg-three";
	public static final String ROC_ANNUAL_ROLLING_AVG_CPIAUCSL = FEDERAL_CPIAUCSL + "/roc-ann-roll-avg-cpiaucsl";
	public static final String RATE_OF_CHANGE_SIGN_CPIAUCSL = FEDERAL_CPIAUCSL + "/roc-sign-cpiaucsl";

	// Url Mappings for M1
	public static final String FEDERAL_M1 = BASE_URL + "/m1";
	public static final String RATE_OF_CHANGE_M1 = FEDERAL_M1 + "/roc";
	public static final String ROLLING_AVG_THREE_M1 = FEDERAL_M1 + "/roll-avg-three";
	public static final String ROC_ANNUAL_ROLLING_AVG_M1 = FEDERAL_M1 + "/roc-ann-roll-avg-m1";
	public static final String RATE_OF_CHANGE_SIGN_M1 = FEDERAL_M1 + "/roc-sign-m1";

	public static final String FEDERAL_M2 = BASE_URL + "/m2";
	public static final String RATE_OF_CHANGE_M2 = FEDERAL_M2 + "/roc";
	public static final String ROLLING_AVG_THREE_M2 = FEDERAL_M2 + "/roll-avg-three";
	public static final String ROC_ANNUAL_ROLLING_AVG_M2 = FEDERAL_M2 + "/roc-ann-roll-avg-m2";
	public static final String RATE_OF_CHANGE_SIGN_M2 = FEDERAL_M2 + "/roc-sign-m2";

	public static final String FEDERAL_M1V = BASE_URL + "/m1v";
	public static final String RATE_OF_CHANGE_M1V = FEDERAL_M1V + "/roc";
	public static final String ROLLING_AVG_THREE_M1V = FEDERAL_M1V + "/roll-avg-three";
	public static final String ROC_ANNUAL_ROLLING_AVG_M1V = FEDERAL_M1V + "/roc-ann-roll-avg-m1v";
	public static final String RATE_OF_CHANGE_SIGN_M1V = FEDERAL_M1V + "/roc-sign-m1v";

	public static final String FEDERAL_M2V = BASE_URL + "/m2v";
	public static final String RATE_OF_CHANGE_M2V = FEDERAL_M2V + "/roc";
	public static final String ROLLING_AVG_THREE_M2V = FEDERAL_M2V + "/roll-avg-three";
	public static final String ROC_ANNUAL_ROLLING_AVG_M2V = FEDERAL_M2V + "/roc-ann-roll-avg-m2v";
	public static final String RATE_OF_CHANGE_SIGN_M2V = FEDERAL_M2V + "/roc-sign-m2v";

	// DGS5

	public static final String FEDERAL_DGS5 = BASE_URL + "/dgs5";
	public static final String RATE_OF_CHANGE_DGS5 = FEDERAL_DGS5 + "/roc";
	public static final String ROLLING_AVG_THREE_DGS5 = FEDERAL_DGS5 + "/roll-avg-three";
	public static final String ROC_ANNUAL_ROLLING_AVG_DGS5 = FEDERAL_DGS5 + "/roc-ann-roll-avg";
	public static final String RATE_OF_CHANGE_SIGN_DGS5 = FEDERAL_DGS5 + "/roc-sign";

	// DGS10

	public static final String FEDERAL_DGS10 = BASE_URL + "/dgs10";
	public static final String RATE_OF_CHANGE_DGS10 = FEDERAL_DGS10 + "/roc";
	public static final String ROLLING_AVG_THREE_DGS10 = FEDERAL_DGS10 + "/roll-avg-three";
	public static final String ROC_ANNUAL_ROLLING_AVG_DGS10 = FEDERAL_DGS10 + "/roc-ann-roll-avg";
	public static final String RATE_OF_CHANGE_SIGN_DGS10 = FEDERAL_DGS10 + "/roc-sign";

	// DGS30
	public static final String FEDERAL_DGS30 = BASE_URL + "/dgs30";
	public static final String RATE_OF_CHANGE_DGS30 = FEDERAL_DGS30 + "/roc";
	public static final String ROLLING_AVG_THREE_DGS30 = FEDERAL_DGS30 + "/roll-avg-three";
	public static final String ROC_ANNUAL_ROLLING_AVG_DGS30 = FEDERAL_DGS30 + "/roc-ann-roll-avg";
	public static final String RATE_OF_CHANGE_SIGN_DGS30 = FEDERAL_DGS30 + "/roc-sign";

	// T5YIE
	public static final String FEDERAL_T5YIE = BASE_URL + "/t5yie";
	public static final String RATE_OF_CHANGE_T5YIE = FEDERAL_T5YIE + "/roc";
	public static final String ROLLING_AVG_THREE_T5YIE = FEDERAL_T5YIE + "/roll-avg-three";
	public static final String ROC_ANNUAL_ROLLING_AVG_T5YIE = FEDERAL_T5YIE + "/roc-ann-roll-avg";
	public static final String RATE_OF_CHANGE_SIGN_T5YIE = FEDERAL_T5YIE + "/roc-sign";

	// T10YIE
	public static final String FEDERAL_T10YIE = BASE_URL + "/t10yie";
	public static final String RATE_OF_CHANGE_T10YIE = FEDERAL_T10YIE + "/roc";
	public static final String ROLLING_AVG_THREE_T10YIE = FEDERAL_T10YIE + "/roll-avg-three";
	public static final String ROC_ANNUAL_ROLLING_AVG_T10YIE = FEDERAL_T10YIE + "/roc-ann-roll-avg";
	public static final String RATE_OF_CHANGE_SIGN_T10YIE = FEDERAL_T10YIE + "/roc-sign";

}
