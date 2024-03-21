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

/**
 * 
 * @author Niraj 
 * Entity For Real Gross Domestic Product
 *
 */
@Entity
@Data
@NoArgsConstructor
public class RGDP implements Serializable{

	/**
	 * 
	 */
		private static final long serialVersionUID = -6424133132128596389L;
	
	/*
	 * @Id
	 * 
	 * @GeneratedValue(strategy = GenerationType.AUTO) private Long id;
	 */	
		@Id
		private Date date;
	
		private Float value;
		
		public RGDP(Date date, float parseFloat) {
			this.date=date;
			this.value=parseFloat;
		}

}
