package com.automation.trading.domain.fred;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.Data;

/**
 * 
 * @author niraj Entity Class For Consumer Price Index for All Urban Consumers
 *
 */
@Entity
@Data
public class CPIAUCSL implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6808582157354231063L;

	@Id
	private Date date;
	private Float value;
	private Boolean rocFlag = false;
	private Boolean rollAverageFlag = false;

	public CPIAUCSL(Date date, Float value) {
		this.date = date;
		this.value = value;
	}

	public CPIAUCSL() {

	}

}
