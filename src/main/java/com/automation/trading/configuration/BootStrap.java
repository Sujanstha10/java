package com.automation.trading.configuration;

import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;

import com.automation.trading.repository.BASERepository;
import com.automation.trading.repository.CIVPARTRepository;
import com.automation.trading.repository.CPIAUCSLRepository;
import com.automation.trading.repository.CPILFESLRepository;
import com.automation.trading.repository.DFFRepository;
import com.automation.trading.repository.DGS10Repository;
import com.automation.trading.repository.DGS30Repository;
import com.automation.trading.repository.DGS5Repository;
import com.automation.trading.repository.DPRIMERepository;
import com.automation.trading.repository.DSPIC96Repository;
import com.automation.trading.repository.EMRATIORepository;
import com.automation.trading.repository.GDPC1Repository;
import com.automation.trading.repository.GDPDEFRepository;
import com.automation.trading.repository.GDPPOTCalculationRepository;
import com.automation.trading.repository.GdpRepository;
import com.automation.trading.repository.IC4WSARepository;
import com.automation.trading.repository.ICSARepository;
import com.automation.trading.repository.M1Repository;
import com.automation.trading.repository.M1VRepository;
import com.automation.trading.repository.M2Repository;
import com.automation.trading.repository.M2VRepository;
import com.automation.trading.repository.MANEMPRepository;
import com.automation.trading.repository.MEHOINUSA672NRepository;
import com.automation.trading.repository.NROURepostiory;
import com.automation.trading.repository.NROUSTRepository;
import com.automation.trading.repository.PAYEMSRepository;
import com.automation.trading.repository.PCEDGRepository;
import com.automation.trading.repository.PCERepository;
import com.automation.trading.repository.PSAVERTRepository;
import com.automation.trading.repository.T10YIERepository;
import com.automation.trading.repository.T5YIERepository;
import com.automation.trading.repository.TEDRATERepository;
import com.automation.trading.repository.UNEMPLOYRepository;
import com.automation.trading.repository.UnRateRepostiory;
import com.automation.trading.service.FederalEmploymentService;
import com.automation.trading.service.FederalIncomeAndExpenditureService;
import com.automation.trading.service.FederalInterestRateService;
import com.automation.trading.service.FederalMoneyService;
import com.automation.trading.service.FederalReserveService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class BootStrap implements ApplicationListener<ContextRefreshedEvent> {

	@Autowired
	private DFFRepository dffRepository;

	@Autowired
	private UnRateRepostiory unRateRepository;

	@Autowired
	private GdpRepository gdpRepository;

	@Autowired
	private GDPDEFRepository gdpdefRepository;

	@Autowired
	private FederalReserveService federalCalculationService;

	@Autowired
	private GDPC1Repository gdpc1Repository;

	@Autowired
	private GDPPOTCalculationRepository gdppotCalculationRepository;

	@Autowired
	private CPIAUCSLRepository cpiaucslRepository;

	@Autowired
	private CPILFESLRepository cpilfeslRepository;

	@Autowired
	private BASERepository baseRepository;

	@Autowired
	private M1Repository m1Repository;

	@Autowired
	private M2Repository m2Repository;

	@Autowired
	private M1VRepository m1vRepository;

	@Autowired
	private M2VRepository m2vRepository;

	@Autowired
	private DGS5Repository dgs5Repository;

	@Autowired
	private DGS10Repository dgs10Repository;

	@Autowired
	private DGS30Repository dgs30Repository;

	@Autowired
	private T5YIERepository t5yieRepository;

	@Autowired
	private T10YIERepository t10yieRepository;

	@Autowired
	private TEDRATERepository tedrateRepository;

	@Autowired
	private NROURepostiory nrouRepostiory;

	@Autowired
	private NROUSTRepository nroustRepository;

	@Autowired
	private DPRIMERepository dprimeRepository;

	@Autowired
	private CIVPARTRepository civpartRepository;

	@Autowired
	private EMRATIORepository emratioRepository;

	@Autowired
	private UNEMPLOYRepository unemployRepository;

	@Autowired
	private ICSARepository icsaRepository;

	@Autowired
	private IC4WSARepository ic4wsaRepository;

	@Autowired
	private PAYEMSRepository payemsRepository;

	@Autowired
	private MANEMPRepository manempRepository;

	@Autowired
	private MEHOINUSA672NRepository mehoinusa672nRepository;

	@Autowired
	private DSPIC96Repository dspic96Repository;

	@Autowired
	private PCERepository pceRepository;

	@Autowired
	private PCEDGRepository pcedgRepository;

	@Autowired
	private PSAVERTRepository psavertRepository;

	@Autowired
	private FederalMoneyService federalMoneyService;

	@Autowired
	private FederalReserveService federalReserveService;

	@Autowired
	private FederalInterestRateService federalInterestRateService;

	@Autowired
	private FederalEmploymentService federalEmploymentService;

	@Autowired
	private FederalIncomeAndExpenditureService federalIncomeAndExpenditureService;

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {

		if (NumberUtils.INTEGER_ZERO.equals(unRateRepository.findAny())) {
			federalCalculationService.saveUnRateData();
		} else {
			log.info("UnRate Data Already Presents");
		}

		if (NumberUtils.INTEGER_ZERO.equals(dffRepository.findAny())) {
			federalCalculationService.saveDFFData();
		} else {
			log.info("DFF Data Already Presents");
		}

		if (NumberUtils.INTEGER_ZERO.equals(gdpRepository.findAny())) {
			federalCalculationService.saveGdpData();

		} else {
			log.info("GDP Date Already Presents");
		}

		if (NumberUtils.INTEGER_ZERO.equals(gdpc1Repository.findAny())) {
			federalCalculationService.saveGDPC1Data();
		} else {
			log.info("GDPC1 Date Already Presents");
		}

		if (NumberUtils.INTEGER_ZERO.equals(gdppotCalculationRepository.findAny())) {
			federalReserveService.saveGDPPOTData();

		} else {
			log.info("GDPPOT Date Already Presents");
		}

		if (NumberUtils.INTEGER_ZERO.equals(cpiaucslRepository.findAny())) {
			federalCalculationService.saveCPIAUCSLData();

		} else {
			log.info("CPIAUCSL Date Already Presents");
		}

		if (NumberUtils.INTEGER_ZERO.equals(cpilfeslRepository.findAny())) {
			federalCalculationService.saveCPILFESLData();

		} else {
			log.info("CPILFESL Date Already Presents");
		}

//		 if (NumberUtils.INTEGER_ZERO.equals(gdpdefRepository.findAny())) {
//		 	 saveGDPDEFData();
//
//		 } else { log.info("GDPDEF Date Already Presents"); }

		if (NumberUtils.INTEGER_ZERO.equals(baseRepository.findAny())) {
			federalMoneyService.saveBASEData();

		} else {
			log.info("Base Date Already Presents");
		}

		if (NumberUtils.INTEGER_ZERO.equals(m1Repository.findAny())) {
			federalMoneyService.saveM1Data();

		} else {
			log.info("M1 Date Already Presents");
		}

		if (NumberUtils.INTEGER_ZERO.equals(m2Repository.findAny())) {
			federalMoneyService.saveM2Data();

		} else {
			log.info("M2 Date Already Presents");
		}

		if (NumberUtils.INTEGER_ZERO.equals(m1vRepository.findAny())) {
			federalMoneyService.saveM1VData();

		} else {
			log.info("M1V Date Already Presents");
		}

		if (NumberUtils.INTEGER_ZERO.equals(m2vRepository.findAny())) {
			federalMoneyService.saveM2VData();

		} else {
			log.info("M2V Date Already Presents");
		}

		if (NumberUtils.INTEGER_ZERO.equals(dgs5Repository.findAny())) {
			federalInterestRateService.saveDGS5Data();

		} else {
			log.info("DGS5 Date Already Presents");
		}

		if (NumberUtils.INTEGER_ZERO.equals(dgs10Repository.findAny())) {
			federalInterestRateService.saveDGS10Data();

		} else {
			log.info("DGS10 Date Already Presents");
		}

		if (NumberUtils.INTEGER_ZERO.equals(dgs30Repository.findAny())) {
			federalInterestRateService.saveDGS30Data();

		} else {
			log.info("DGS30 Date Already Presents");
		}

		if (NumberUtils.INTEGER_ZERO.equals(t5yieRepository.findAny())) {
			federalInterestRateService.saveT5YIEData();

		} else {
			log.info("T5YIE Data Already Presents");
		}

		if (NumberUtils.INTEGER_ZERO.equals(t10yieRepository.findAny())) {
			federalInterestRateService.saveT10YIEData();

		} else {
			log.info("T10YIE Data Already Presents");
		}

		if (NumberUtils.INTEGER_ZERO.equals(tedrateRepository.findAny())) {
			federalInterestRateService.saveTEDRATEData();

		} else {
			log.info("TEDRATE Data Already Presents");
		}

		if (NumberUtils.INTEGER_ZERO.equals(tedrateRepository.findAny())) {
			federalInterestRateService.saveTEDRATEData();

		} else {
			log.info("TEDRATE Data Already Presents");
		}

		if (NumberUtils.INTEGER_ZERO.equals(dprimeRepository.findAny())) {
			federalInterestRateService.saveDPRIMEData();

		} else {
			log.info("Dprime Data Already Presents");
		}

		if (NumberUtils.INTEGER_ZERO.equals(nrouRepostiory.findAny())) {
			federalEmploymentService.saveNROUData();

		} else {
			log.info("NROU Data Already Presents");
		}

		if (NumberUtils.INTEGER_ZERO.equals(nroustRepository.findAny())) {
			federalEmploymentService.saveNROUSTData();

		} else {
			log.info("NROUST Data Already Presents");
		}

		if (NumberUtils.INTEGER_ZERO.equals(civpartRepository.findAny())) {
			federalEmploymentService.saveCIVPARTData();

		} else {
			log.info("CIVPART Data Already Presents");
		}

		if (NumberUtils.INTEGER_ZERO.equals(emratioRepository.findAny())) {
			federalEmploymentService.saveEMRATIOData();

		} else {
			log.info("EMRATIO Data Already Presents");
		}

		if (NumberUtils.INTEGER_ZERO.equals(unemployRepository.findAny())) {
			federalEmploymentService.saveUNEMPLOYData();
		} else {
			log.info("UNEMPLOY Data Already Presents");
		}

		if (NumberUtils.INTEGER_ZERO.equals(payemsRepository.findAny())) {
			federalEmploymentService.savePAYEMSData();

		} else {
			log.info("PAYEMS Data Already Presents");
		}

		if (NumberUtils.INTEGER_ZERO.equals(manempRepository.findAny())) {
			federalEmploymentService.saveMANEMPData();

		} else {
			log.info("MANEMP Data Already Presents");
		}
		if (NumberUtils.INTEGER_ZERO.equals(icsaRepository.findAny())) {
			federalEmploymentService.saveICSAData();

		} else {
			log.info("ICSA Data Already Presents");
		}

		if (NumberUtils.INTEGER_ZERO.equals(ic4wsaRepository.findAny())) {
			federalEmploymentService.saveIC4WSAData();

		} else {
			log.info("IC4WSA Data Already Presents");
		}

		if (NumberUtils.INTEGER_ZERO.equals(mehoinusa672nRepository.findAny())) {
			federalIncomeAndExpenditureService.saveMEHOINUSA672NData();

		} else {
			log.info("MEHOINUSA672N Data Already Presents");
		}
		if (NumberUtils.INTEGER_ZERO.equals(dspic96Repository.findAny())) {
			federalIncomeAndExpenditureService.saveDSPIC96Data();

		} else {
			log.info("MEHOINUSA672N Data Already Presents");
		}

		if (NumberUtils.INTEGER_ZERO.equals(pceRepository.findAny())) {
			federalIncomeAndExpenditureService.savePCEData();

		} else {
			log.info("PCE Data Already Presents");
		}

		if (NumberUtils.INTEGER_ZERO.equals(pcedgRepository.findAny())) {
			federalIncomeAndExpenditureService.savePCEDGData();

		} else {
			log.info("PCEDG Data Already Presents");
		}

		if (NumberUtils.INTEGER_ZERO.equals(psavertRepository.findAny())) {
			federalIncomeAndExpenditureService.savePSAVERTData();

		} else {
			log.info("PSAVERT Data Already Presents");
		}

	}
}
