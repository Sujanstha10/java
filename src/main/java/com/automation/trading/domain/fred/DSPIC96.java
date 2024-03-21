package com.automation.trading.domain.fred;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class DSPIC96 implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -181826574065903594L;

	@Id
	private Date date;
	private Float value;
	private Boolean rocFlag = false;
	private Boolean rollAverageFlag = false;

	public DSPIC96(Date date, float parseFloat) {
		this.date = date;
		this.value = parseFloat;
	}
}
