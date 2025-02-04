package com.automation.trading.domain.fred;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Data
@NoArgsConstructor
public class RRSFS implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9120654365340811195L;

	/*
	 * @Id
	 * 
	 * @GeneratedValue(strategy = GenerationType.AUTO) private Long id;
	 */

	@Id
	private Date date;

	private Float value;
	
	public RRSFS(Date date, float parseFloat) {
		this.date = date;
		this.value = parseFloat;
	}
}
