package ch.cern.cms.load.hwdb;

import java.util.Collection;
import java.util.HashSet;
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
import rcms.utilities.hwcfg.eq.FMMFMMLink;
import rcms.utilities.hwcfg.eq.FRL;
import rcms.utilities.hwcfg.eq.FRLCrate;

public final class HwInfo {

	private final static String hwcfgDbURL = "jdbc:oracle:thin:@localhost:10121/cms_omds_tunnel.cern.ch";
	private final static String hwcfgPassword = "mickey2mouse";
	private final static String hwcfgUser = "CMS_DAQ_HW_CONF_R";
	private static HwInfo instance = null;

	public static HwInfo getInstance() {
		if (instance == null) {
			synchronized (HwInfo.class) {
				if (instance == null) {
					instance = new HwInfo();
				}
			}
		}
		return instance;
	}

	private DBConnectorIF dbconn = null;
	private HWCfgDescriptor dpNode;

	private EquipmentSet eqs;

	private HWCfgConnector hwconn = null;

	private HwInfo() {
		try {
			dbconn = new DBConnectorOracle(hwcfgDbURL, hwcfgUser, hwcfgPassword);

			hwconn = new HWCfgConnector(dbconn);
			dpNode = hwconn.getNode("/cms/eq_120503/RUN_2012/fb_all_2012routing2_rev120320_SplitCSC/dp_4SL0f_bl688_158BU_4SMr06");
			eqs = hwconn.retrieveDPSet(dpNode).getEquipmentSet();
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

	public FED getFedForFrl(String hostname, int geoSlot, int link) throws DBConnectorException {
		FED fed = null;
		FRLCrate frlCrate = eqs.getFRLCrateByHostName(peelHostname(hostname));
		if (frlCrate != null) {
			FRL frl = frlCrate.getFRLbyGeoSlot(geoSlot);
			if (frl != null) {
				fed = frl.getFEDByLink(link);
			}
		}
		return fed;
	}

	public FED getFedForFmm(String hostname, int geoSlot, int io) throws DBConnectorException {
		FED fed = null;
		FMMCrate fmmCrate = eqs.getFMMCrateByHostName(peelHostname(hostname));
		return fed;
	}

	public FED getFed(String context, int geoSlotOrSlot, int linkOrIo, CmsHw seenByHw) {
		try {
			switch (seenByHw) {
			case FMM:
				return getFedForFmm(context, geoSlotOrSlot, linkOrIo);
			case FRL:
				return getFedForFrl(context, geoSlotOrSlot, linkOrIo);
			default:
				return null;
			}
		} catch (DBConnectorException e) {
			System.out.println("wrong!");
			e.printStackTrace();
			throw new RuntimeException("time to get a logger", e);
		}
	}

	public Integer getFedId(Object context, Object geoSlotOrSlot, Object linkOrIo, CmsHw seenByHw) {
		if (context instanceof String && geoSlotOrSlot instanceof Integer && linkOrIo instanceof Integer) {
			return getFedId((String) context, (Integer) geoSlotOrSlot, (Integer) linkOrIo, seenByHw);
		} else {
			return getFedId(context.toString(), Integer.parseInt(geoSlotOrSlot.toString()), Integer.parseInt(linkOrIo.toString()), seenByHw);
		}
	}

	public Integer getFedId(String context, int geoSlotOrSlot, int linkOrIo, CmsHw seenByHw) {
		FED fed = getFed(context, geoSlotOrSlot, linkOrIo, seenByHw);
		return fed == null ? null : fed.getSrcId();
	}

	public Integer getFedId(Object c, Object g, Object l, CmsHw s, String str) {
		Integer v = getFedId(c, g, l, s);
		System.out.println(str + "(" + c + ", " + g + ", " + l + ", " + s + ") source id: " + v);
		return v;
	}

	public void getFRLCratesAndPrintStuff() {
		Map<Long, FRLCrate> crates = eqs.getFRLCrates();
		for (Long l : crates.keySet()) {
			System.out.println(l + ": " + crates.get(l).getHostName());
		}
		// eqs.getFMMFMMLinks()
	}

	/** the fed connected directly to fmm or one of its main feds **/
	public Collection<Integer> getDeadtimeRelevantFedIds(Object context, Object geoslot, Object io) {
		Collection<Integer> ids = new HashSet<Integer>();
		FED fed = getFed(context instanceof String ? (String) context : context.toString(),
				geoslot instanceof Integer ? (Integer) geoslot : Integer.parseInt(geoslot.toString()),
				io instanceof Integer ? (Integer) io : Integer.parseInt(io.toString()), CmsHw.FMM);
		if (fed != null) {
			if (fed.getSrcId() != null) {
				ids.add(fed.getSrcId());
			}
			if (fed.getMainFEDs() != null) {
				for (FED mainFed : fed.getMainFEDs()) {
					ids.add(mainFed.getSrcId());
				}
			}
		} else {
			FMMCrate crate = eqs.getFMMCrateByHostName(peelHostname(context.toString()));
			if (crate != null) {
				FMM fmm = crate.getFMMbyGeoSlot((Integer) geoslot);
				for (FMMFMMLink link : eqs.getFMMFMMLinks()) {
					if (link.getSourceFMMIO() == (Integer) io) {
						System.out.println("is source for: " + link.toString());
					} else if (link.getTargetFMMIO() == (Integer) io) {
						System.out.println("is target for: " + link.toString());
					}
				}
			}

		}
		return ids;
	}

	public static final String esperCheck() {
		return "If you see this message than you have configured your class import correctly";
	}
}
