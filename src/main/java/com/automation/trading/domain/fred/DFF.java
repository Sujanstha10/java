package com.automation.trading.domain.fred;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 
 * @author niraj Entity class for Effective Federal Funds Rate
 *
 */

@Entity
@Data
@NoArgsConstructor
public class DFF implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5602720587773160258L;

	@Id
	private Date date;
	private Float value;
	private Boolean rocFlag = false;
	private Boolean rollAverageFlag = false;

	public DFF(Date date, float parseFloat) {
		this.date = date;
		this.value = parseFloat;
	}

}
