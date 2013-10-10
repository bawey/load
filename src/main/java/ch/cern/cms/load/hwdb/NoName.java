package ch.cern.cms.load.hwdb;

import java.util.Map;

import rcms.common.db.DBConnectorException;
import rcms.common.db.DBConnectorIF;
import rcms.common.db.DBConnectorOracle;
import rcms.utilities.hwcfg.HWCfgConnector;
import rcms.utilities.hwcfg.HWCfgDescriptor;
import rcms.utilities.hwcfg.eq.EquipmentSet;
import rcms.utilities.hwcfg.eq.FED;
import rcms.utilities.hwcfg.eq.FMM;
import rcms.utilities.hwcfg.eq.FMMCrate;
import rcms.utilities.hwcfg.eq.FRL;
import rcms.utilities.hwcfg.eq.FRLCrate;

public class NoName {

	private final static String hwcfgDbURL = "jdbc:oracle:thin:@localhost:10121/cms_omds_tunnel.cern.ch";
	private final static String hwcfgPassword = "mickey2mouse";
	private final static String hwcfgUser = "CMS_DAQ_HW_CONF_R";
	private static NoName instance = null;

	public static NoName getInstance() {
		if (instance == null) {
			synchronized (NoName.class) {
				if (instance == null) {
					instance = new NoName();
				}
			}
		}
		return instance;
	}

	private DBConnectorIF dbconn = null;
	private HWCfgDescriptor dpNode;

	private EquipmentSet eqs;

	private HWCfgConnector hwconn = null;

	private NoName() {
		try {
			dbconn = new DBConnectorOracle(hwcfgDbURL, hwcfgUser, hwcfgPassword);

			hwconn = new HWCfgConnector(dbconn);
			dpNode = hwconn.getNode("/cms/eq_120503/RUN_2012/fb_all_2012routing2_rev120320_SplitCSC/dp_4SL0f_bl688_158BU_4SMr06");
			eqs = hwconn.retrieveDPSet(dpNode).getEquipmentSet();
			dbconn.closeConnection();
		} catch (Exception r) {
			System.err.println("die die die my darling");
			r.printStackTrace();
		}
	}

	private String peelHostname(String hostname) {
		hostname = hostname.trim();
		if (hostname.startsWith("http://")) {
			hostname = hostname.substring(7);
		}
		if (hostname.contains(":")) {
			hostname = hostname.substring(0, hostname.lastIndexOf(":"));
		}
		return hostname;
	}

	public FED getFedIdForFrl(String hostname, int geoSlot, int link) throws DBConnectorException {
		dbconn.openConnection();
		FED fed = null;
		FRLCrate frlCrate = eqs.getFRLCrateByHostName(peelHostname(hostname));
		if (frlCrate != null) {
			FRL frl = frlCrate.getFRLbyGeoSlot(geoSlot);
			if (frl != null) {
				fed = frl.getFEDByLink(link);
			}
		}
		dbconn.closeConnection();
		return fed;
	}

	public FED getFedIdForFmm(String hostname, int geoSlot, int io) throws DBConnectorException {
		dbconn.openConnection();
		FED fed = null;
		FMMCrate fmmCrate = eqs.getFMMCrateByHostName(hostname);
		if (fmmCrate != null) {
			FMM fmm = fmmCrate.getFMMbyGeoSlot(geoSlot);
			if (fmm != null) {
				fed = fmm.getFEDByIO(io);
			}
		}
		dbconn.closeConnection();
		return fed;
	}

	public void getFRLCratesAndPrintStuff() {
		Map<Long, FRLCrate> crates = eqs.getFRLCrates();
		for (Long l : crates.keySet()) {
			System.out.println(l + ": " + crates.get(l).getHostName());
		}
	}
}
