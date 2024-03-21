package com.automation.trading.domain.fred;

import java.util.ArrayList;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Data
public class DFFHelper {
	
	private String start_date;
    
	private String end_date;
	
	private String frequency;
	
	ArrayList < Object > data = new ArrayList <> ();

}
