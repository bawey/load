package ch.cern.cms.load.hwdb;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import rcms.common.db.DBConnectorException;
import rcms.common.db.DBConnectorIF;
import rcms.common.db.DBConnectorOracle;
import rcms.utilities.hwcfg.HWCfgConnector;
import rcms.utilities.hwcfg.HWCfgDescriptor;
import rcms.utilities.hwcfg.HardwareConfigurationException;
import rcms.utilities.hwcfg.eq.EquipmentSet;
import rcms.utilities.hwcfg.eq.FED;
import rcms.utilities.hwcfg.eq.FMM;
import rcms.utilities.hwcfg.eq.FMMCrate;
import rcms.utilities.hwcfg.eq.FMMFMMLink;
import rcms.utilities.hwcfg.eq.FMMTriggerLink;
import rcms.utilities.hwcfg.eq.FRL;
import rcms.utilities.hwcfg.eq.FRLCrate;
import rcms.utilities.hwcfg.eq.SubSystem;
import rcms.utilities.hwcfg.eq.TTCPartition;
import rcms.utilities.hwcfg.eq.Trigger;
import ch.cern.cms.esper.Trx;
import ch.cern.cms.load.Load;
import ch.cern.cms.load.eventData.FedMask;

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
								//System.out.println("HwInfo dumped at " + path);
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

	public static Integer getFedSrcId(Object context, Object geoSlotOrSlot, Object linkOrIo, CmsHw seenByHw) {
		if (context instanceof String && geoSlotOrSlot instanceof Integer && linkOrIo instanceof Integer) {
			return getFedSrcId((String) context, ((Integer) geoSlotOrSlot).intValue(), ((Integer) linkOrIo).intValue(), seenByHw);
		} else {
			return getFedSrcId(context.toString(), Integer.parseInt(geoSlotOrSlot.toString()), Integer.parseInt(linkOrIo.toString()), seenByHw);
		}
	}

	private static Map<FedDesc, Integer> fedSrcIdCache = new HashMap<FedDesc, Integer>();

	public static Integer getFedSrcId(String context, int geoSlotOrSlot, int linkOrIo, CmsHw seenByHw) {
		FedDesc desc = new FedDesc(linkOrIo, geoSlotOrSlot, context, seenByHw);
		Integer id = fedSrcIdCache.get(desc);
		if (id != null) {
			return id;
		}
		FED fed = getFed(context, geoSlotOrSlot, linkOrIo, seenByHw);
		if (fed != null) {
			fedSrcIdCache.put(desc, fed.getSrcId());
			return fed.getSrcId();
		}
		return null;
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

	public static Integer[] getMainFedSrcIds(Object context, Object geoslot, Object io) {
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

	public static Integer[] getMainFedSrcIds(int fedSrcId) {
		Collection<Integer> ids = new HashSet<Integer>();
		FED fed = getInstance().eqs.getFEDBySourceId(fedSrcId);
		if (fed != null) {
			for (FED mainFed : fed.getMainFEDs()) {
				ids.add(mainFed.getSrcId());
			}
		}
		return ids.toArray(new Integer[ids.size()]);
	}

	public static FRL getFRL(String context, Object slot) {
		HwInfo hi = getInstance();
		return hi.eqs.getFRLCrateByHostName(context).getFRLbyGeoSlot(slot instanceof Integer ? (Integer) slot : Integer.parseInt(slot.toString()));

	}

	public static void voidd() {
		HwInfo hi = getInstance();
		FRL frl = null;
		// frl.get

	}

	public static final String fedsInfoString(Map<Integer, String> rawData) {
		HwInfo hi = getInstance();

		TreeMap<String, TreeMap<String, TreeSet<String>>> eyjafjallajokull = new TreeMap<String, TreeMap<String, TreeSet<String>>>();

		for (Map.Entry<Integer, String> tuple : rawData.entrySet()) {
			long fedSourceId = tuple.getKey();
			String desc = tuple.getValue();
			FED fed = hi.eqs.getFEDBySourceId(fedSourceId);

			if (fed == null) {
				System.out.println("Null fed. remove such possibility after tests");
				continue;
			}
			TTCPartition partition = fed.getTTCPartition();
			SubSystem subsys = partition.getSubSystem();

			StringBuilder fedInfo = new StringBuilder("fed#").append(String.format("%03d", fedSourceId));
			if (desc != null && desc.length() > 0) {
				fedInfo.append(": ").append(desc);
			}

			if (!eyjafjallajokull.containsKey(subsys.getName())) {
				eyjafjallajokull.put(subsys.getName(), new TreeMap<String, TreeSet<String>>());
			}

			Map<String, TreeSet<String>> subMap = eyjafjallajokull.get(subsys.getName());

			if (!subMap.containsKey(partition.getName())) {
				subMap.put(partition.getName(), new TreeSet<String>());
			}

			subMap.get(partition.getName()).add(fedInfo.toString());
		}

		StringBuilder result = new StringBuilder();

		Iterator<Entry<String, TreeMap<String, TreeSet<String>>>> iter1 = eyjafjallajokull.entrySet().iterator();
		while (iter1.hasNext()) {
			Map.Entry<String, TreeMap<String, TreeSet<String>>> level1 = iter1.next();
			result.append(level1.getKey()).append(": [");
			Iterator<Entry<String, TreeSet<String>>> iter2 = level1.getValue().entrySet().iterator();
			while (iter2.hasNext()) {
				Map.Entry<String, TreeSet<String>> level2 = iter2.next();
				result.append(level2.getKey()).append(": (");
				Iterator<String> iter3 = level2.getValue().iterator();
				while (iter3.hasNext()) {
					result.append(iter3.next());
					if (iter3.hasNext()) {
						result.append(", ");
					}
				}
				result.append(")");
				if (iter2.hasNext()) {
					result.append(", ");
				}
			}
			result.append("]");
			if (iter1.hasNext()) {
				result.append(", ");
			}
		}
		return result.toString();
	}

	public static String getPartitionName(String hostName, int geoSlot, String ab) {
		//System.out.println("getting partition by hostname: " + hostName + ", AB: " + ab + ", geoSlot: " + geoSlot);
		HwInfo hi = getInstance();

		for (FMM fmm : hi.eqs.getFMMs().values()) {
			if (fmm.getFMMCrate().getHostName().equalsIgnoreCase(hostName) && fmm.getGeoSlot() == geoSlot)
				return getFMMPartition(fmm, ab);
		}

		return "";
	}

	/**
	 * @param fmm
	 * @param ab
	 *            "A" if checking partition for output A, or "B" otherwise
	 * 
	 * @return The partition name or null if the FMM belongs to multiple partitions
	 */
	public static String getFMMPartition(FMM fmm, String ab) {
		HwInfo hi = getInstance();
		//System.out.println("getting partition by fmm: " + fmm + ", AB: " + ab);
		long gtpId = -1;
		for (Trigger t : hi.eqs.getTriggers().values())
			if (t.getName().equals("GTP")) // FIXME make more generic
				gtpId = t.getId();

		for (FMMTriggerLink ftl : hi.eqs.getFMMTriggerLinks()) {
			if (ftl.getTriggerId() == gtpId && fmm.getId() == ftl.getFMMId()) {
				if (fmm.getDual() == false) {
					return getPartitionNameByNr(ftl.getTriggerIO());
				} else {
					if (ab.equals("A")) {
						if (ftl.getFMMIO() == 20 || ftl.getFMMIO() == 21)
							return getPartitionNameByNr(ftl.getTriggerIO());
					} else if (ab.equals("B")) {
						if (ftl.getFMMIO() == 22 || ftl.getFMMIO() == 23)
							return getPartitionNameByNr(ftl.getTriggerIO());
					}
				}
			}

		}
		return null;
	}

	public static String getPartitionNameByNr(int partNr) {
		//System.out.println("getting partition by number: " + partNr);
		HwInfo hi = getInstance();

		for (TTCPartition ttcp : hi.eqs.getTTCPartitions().values())
			if (ttcp.getTTCPNr() == partNr) {
				//System.out.println("partName=" + ttcp.getName());
				return ttcp.getName();
			}

		return null;
	}

	// public static final void main(String[] args) {
	// for(int )
	// }
}
