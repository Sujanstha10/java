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
public class PCE implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4832050955767391878L;

	@Id
	private Date date;
	private Float value;
	private Boolean rocFlag = false;
	private Boolean rollAverageFlag = false;

	public PCE(Date date, float parseFloat) {
		this.date = date;
		this.value = parseFloat;
	}
}
