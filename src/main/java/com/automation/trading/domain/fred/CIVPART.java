package com.automation.trading.domain.fred;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Data
@NoArgsConstructor

@Setter
@Getter
public class CIVPART implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -48109727121672027L;

	@Id
	private Date date;
	private Float value;
	private Boolean rocFlag = false;
	private Boolean rollAverageFlag = false;

	public CIVPART(Date date, Float value) {
		this.date = date;
		this.value = value;
	}
	
	

}
