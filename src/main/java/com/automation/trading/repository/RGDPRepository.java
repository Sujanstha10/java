package com.automation.trading.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.fred.RGDP;

public interface RGDPRepository extends JpaRepository<RGDP, Long>{
	
	@Query(value = "SELECT EXISTS (SELECT 1 FROM rgdp)", nativeQuery = true)
	Integer findAny();

}
