package com.automation.trading.utility;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.automation.trading.repository.DFFRepository;

@Service
public class FederalCalculationService {
	
	@Autowired
	private DFFRepository dffRepository;
	
	public Long updateDFFRate()
	{
		Long minFalseId = dffRepository.findMinId();
		return minFalseId;
		
	}

}
