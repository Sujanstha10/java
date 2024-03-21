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
 *         Entity Class for M2 Money Stock
 *
 */

@Entity
@Data
@NoArgsConstructor
public class M2 implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5620560618458058621L;

	@Id
	private Date date;
	private Float value;
	private Boolean rocFlag = false;
	private Boolean rollAverageFlag = false;

	public M2(Date date, Float value) {
		this.date = date;
		this.value = value;
	}

}
