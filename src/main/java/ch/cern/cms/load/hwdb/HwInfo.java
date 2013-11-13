package ch.cern.cms.load.hwdb;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

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
import ch.cern.cms.esper.Trx;
import ch.cern.cms.load.Load;

public final class HwInfo implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = Logger.getLogger(HwInfo.class);

	private final static String hwcfgDbURL = "jdbc:oracle:thin:@localhost:10121/cms_omds_tunnel.cern.ch";
	private final static String hwcfgPassword = "mickey2mouse";
	private final static String hwcfgUser = "CMS_DAQ_HW_CONF_R";
	private static HwInfo instance = null;

	public static HwInfo getInstance() {
		if (instance == null) {
			synchronized (HwInfo.class) {
				if (instance == null) {
					if (Load.getInstance().getSettings().containsKey("HwInfo_load")) {
						String path = Load.getInstance().getSettings().getProperty("HwInfo_load");
						ObjectInputStream ois;
						try {
							ois = new ObjectInputStream(new FileInputStream(path));
							Object loaded = ois.readObject();
							ois.close();
							if (loaded instanceof HwInfo) {
								instance = (HwInfo) loaded;
							}
						} catch (Exception e) {
							throw new RuntimeException("I really wanted to read HwInfo dump", e);
						}
					} else {
						instance = new HwInfo();
						if (Load.getInstance().getSettings().containsKey("HwInfo_dump")) {
							String path = Load.getInstance().getSettings().getProperty("HwInfo_dump");
							try {
								ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path));
								oos.writeObject(instance);
								oos.close();
								System.out.println("HwInfo dumped at " + path);
							} catch (Exception e) {
								logger.error("Failed to dump HwInfo to a file", e);
							}
						}
					}
				}
			}
		}
		return instance;
	}

	private transient DBConnectorIF dbconn = null;
	private transient HWCfgDescriptor dpNode;
	private transient HWCfgConnector hwconn = null;

	private EquipmentSet eqs;

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

	private static String peelHostname(String hostname) {
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

	public static FED getFedForFmm(String hostname, int geoSlot, int io) throws DBConnectorException {
		HwInfo hi = getInstance();
		FED fed = null;
		FMMCrate fmmCrate = hi.eqs.getFMMCrateByHostName(peelHostname(hostname));
		if (fmmCrate != null) {
			FMM fmm = fmmCrate.getFMMbyGeoSlot(geoSlot);
			if (fmm != null) {
				fed = fmm.getFEDByIO(io);
			} else {
				System.err.println("fmm is null");
			}
		} else {
			System.err.println("crate is null");
		}
		return fed;
	}

	public static FMM getFMM(String context, int geoSlot) {
		HwInfo hi = getInstance();
		FMMCrate crate = hi.eqs.getFMMCrateByHostName(peelHostname(context));
		if (crate != null) {
			return crate.getFMMbyGeoSlot(geoSlot);
		}
		return null;
	}

	public static FED getFed(String context, int geoSlotOrSlot, int linkOrIo, CmsHw seenByHw) {
		HwInfo hi = getInstance();
		try {
			switch (seenByHw) {
			case FMM:
				return getFedForFmm(context, geoSlotOrSlot, linkOrIo);
			case FRL:
				return hi.getFedForFrl(context, geoSlotOrSlot, linkOrIo);
			default:
				return null;
			}
		} catch (DBConnectorException e) {
			System.out.println("wrong!");
			e.printStackTrace();
			throw new RuntimeException("time to get a logger", e);
		}
	}

	public static Integer getFedId(Object context, Object geoSlotOrSlot, Object linkOrIo, CmsHw seenByHw) {
		if (context instanceof String && geoSlotOrSlot instanceof Integer && linkOrIo instanceof Integer) {
			return getFedId((String) context, ((Integer) geoSlotOrSlot).intValue(), ((Integer) linkOrIo).intValue(), seenByHw);
		} else {
			return getFedId(context.toString(), Integer.parseInt(geoSlotOrSlot.toString()), Integer.parseInt(linkOrIo.toString()), seenByHw);
		}
	}

	public static Integer getFedId(String context, int geoSlotOrSlot, int linkOrIo, CmsHw seenByHw) {
		FED fed = getFed(context, geoSlotOrSlot, linkOrIo, seenByHw);
		return fed == null ? null : fed.getSrcId();
	}

	public static Integer getFedId(Object c, Object g, Object l, CmsHw s, String str) {
		System.out.println("whattahell!");
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

	public static FMM getSrcFMM(FMM fmm, int io) {
		HwInfo hi = getInstance();
		Set<FMMFMMLink> ffls = hi.eqs.getFMMFMMLinks();
		for (Iterator<FMMFMMLink> iter = ffls.iterator(); iter.hasNext();) {
			FMMFMMLink link = iter.next();
			if (link.getTargetFMMId() == fmm.getId() && link.getTargetFMMIO() == io) {
				return hi.eqs.getFMMs().get(link.getTargetFMMId());
			}
		}
		return null;
	}

	public static Integer[] getMainFedIds(Object context, Object geoslot, Object io) {
		Collection<Integer> ids = new HashSet<Integer>();
		String c = Trx.toText(context);
		int slot = Trx.toInt(geoslot);
		int i = Trx.toInt(io);
		FED fed = getFed(peelHostname(c), slot, i, CmsHw.FMM);
		if (fed != null) {
			for (FED mainFed : fed.getMainFEDs()) {
				ids.add(mainFed.getSrcId());
			}
		}
		return ids.toArray(new Integer[ids.size()]);
	}

	/** means: when deadtime observed on the triplet passed here, all the result fedIDs should be checked against backpressure fedIDs **/
	public static Integer[] getDeadtimeRelevantFedIds(Object context, Object geoslot, Object io) {

		Collection<Integer> ids = new HashSet<Integer>();
		String c = Trx.toText(context);
		int slot = Trx.toInt(geoslot);
		int i = Trx.toInt(io);
		FED fed = getFed(peelHostname(c), slot, i, CmsHw.FMM);
		if (fed != null) {
			ids.add(fed.getSrcId());
			for (FED mainFed : fed.getMainFEDs()) {
				ids.add(mainFed.getSrcId());
			}
		}
		return ids.toArray(new Integer[ids.size()]);
	}
}
