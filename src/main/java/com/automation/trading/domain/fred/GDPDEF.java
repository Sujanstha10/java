package com.automation.trading.domain.fred;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 
 * @author niraj Entity Gross Domestic Product: Implicit Price Deflator
 */

@Entity
@Data
@NoArgsConstructor
public class GDPDEF implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4259213600972273654L;

	@Id
	private Date date;
	private Float value;
	private Boolean rocFlag = false;
	private Boolean rollAverageFlag = false;

	public GDPDEF(Date date, Float value) {
		this.date = date;
		this.value = value;
	}

}
