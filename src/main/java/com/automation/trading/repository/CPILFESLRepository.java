package com.automation.trading.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automation.trading.domain.fred.CPILFESL;

import java.util.Date;

public interface CPILFESLRepository extends JpaRepository<CPILFESL, Date> {

	@Query(value = "SELECT EXISTS (SELECT 1 FROM cpilfesl)", nativeQuery = true)
	Integer findAny();
}
