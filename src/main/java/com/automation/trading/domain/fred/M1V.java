package com.automation.trading.domain.fred;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 
 * @author niraj
 * 
 *         Entity Class for Velocity of M1 Money Stock
 *
 */
@Entity
@Data
@NoArgsConstructor
public class M1V implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4214438695576859892L;

	@Id
	private Date date;
	private Float value;
	private Boolean rocFlag = false;
	private Boolean rollAverageFlag = false;

	public M1V(Date date, Float value) {
		this.date = date;
		this.value = value;
	}

}
