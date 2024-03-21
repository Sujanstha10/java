package com.automation.trading.domain.fred;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 
 * @author niraj Entity class for St. Louis Adjusted Monetary Base
 *
 */

@Entity
@Data
@NoArgsConstructor
public class BASE implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1379521333425069817L;

	@Id
	private Date date;
	private Float value;
	private Boolean rocFlag = false;
	private Boolean rollAverageFlag = false;

	public BASE(Date date, Float value) {
		this.date = date;
		this.value = value;
	}

}
