package com.automation.trading.domain.fred;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 
 * @author niraj Entity For Real Potential Gross Domestic Product
 *
 */
@Entity
@Data
@NoArgsConstructor
public class GDPPOT {

	@Id
	private Date date;
	private Float value;
	private Boolean rocFlag = false;
	private Boolean rollAverageFlag = false;

	public GDPPOT(Date date, Float value) {
		this.date = date;
		this.value = value;
	}

}
