package ch.cern.cms.load.hwdb;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import rcms.common.db.DBConnectorException;
import rcms.common.db.DBConnectorIF;
import rcms.common.db.DBConnectorOracle;
import rcms.utilities.hwcfg.HWCfgConnector;
import rcms.utilities.hwcfg.HWCfgDescriptor;
import rcms.utilities.hwcfg.HardwareConfigurationException;
import rcms.utilities.hwcfg.dp.DAQPartition;
import rcms.utilities.hwcfg.dp.DAQPartitionSet;
import rcms.utilities.hwcfg.dp.DAQPartitionStructureExtractor;
import rcms.utilities.hwcfg.eq.EquipmentSet;
import rcms.utilities.hwcfg.eq.FED;
import rcms.utilities.hwcfg.eq.FMM;
import rcms.utilities.hwcfg.eq.FMMCrate;
import rcms.utilities.hwcfg.eq.FMMFMMLink;
import rcms.utilities.hwcfg.eq.FMMTriggerLink;
import rcms.utilities.hwcfg.eq.FRL;
import rcms.utilities.hwcfg.eq.FRLCrate;
import rcms.utilities.hwcfg.eq.TTCPartition;
import rcms.utilities.hwcfg.eq.Trigger;
import ch.cern.cms.esper.Trx;
import ch.cern.cms.load.Load;

public final class HwInfo implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = Logger.getLogger(HwInfo.class);

	private final static String hwcfgDbURL = "jdbc:oracle:thin:@localhost:10121/cms_omds_tunnel.cern.ch";
	//private final static String hwcfgDbURL = "jdbc:oracle:thin:@int2r1-v.cern.ch:10121/int2r_lb.cern.ch";
	private final static String hwcfgPassword = "mickey2mouse";
	private final static String hwcfgUser = "CMS_DAQ_HW_CONF_R";
	private static HwInfo instance = null;

	public static HwInfo getInstance() {
		if (instance == null) {
			synchronized (HwInfo.class) {
				if (instance == null) {
					/** During development it was possible to serialize HwDb  **/
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
								// System.out.println("HwInfo dumped at " + path);
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

	/**
	 * Forces reinitialization of HwInfo object. getInstance() will either call a constructor that should fill all the caches or reload the
	 * same instance form file (development mode).
	 **/
	synchronized public static final void reinitialize() {
		instance = null;
		getInstance();
	}

	private transient DBConnectorIF dbconn = null;
	private transient HWCfgDescriptor dpNode = null;
	private transient HWCfgConnector hwconn = null;

	private transient EquipmentSet eqs;
	private transient DAQPartitionSet dps;

	private Map<TTCPartition, SortedSet<FED>> daqConfFedsByPartition;
	private Set<FED> daqConfFeds;

	private HwInfo() {
		try {
			dbconn = new DBConnectorOracle(hwcfgDbURL, hwcfgUser, hwcfgPassword);

			hwconn = new HWCfgConnector(dbconn);
			dpNode = hwconn.getNode("/cms/eq_120503/RUN_2012/fb_all_2012routing2_rev120320_SplitCSC/dp_4SL0f_bl688_158BU_4SMr06");
			dps = hwconn.retrieveDPSet(dpNode);
			eqs = dps.getEquipmentSet();

			/** Initialize caches **/
			initDaqConfFeds();

		} catch (Exception r) {
			System.err.println("die die die my darling");
			r.printStackTrace();
		}
	}

	private void initDaqConfFeds(){
		daqConfFeds = new HashSet<FED>();
		for (DAQPartition daqPartition : dps.getDPs().values()) {
			DAQPartitionStructureExtractor structureExtractor = new DAQPartitionStructureExtractor(daqPartition);
			try {
				for (FED fed : structureExtractor.getFEDsInDP().values()) {
					daqConfFeds.add(fed);
				}
			} catch (HardwareConfigurationException e) {
				logger.error("Problem while accessing FEDs for DAQ partition", e);
			}
		}
	
		daqConfFedsByPartition = new HashMap<TTCPartition, SortedSet<FED>>();
		for (FED fed : daqConfFeds) {
			if (!daqConfFedsByPartition.containsKey(fed.getTTCPartition())) {
				daqConfFedsByPartition.put(fed.getTTCPartition(), new TreeSet<FED>(new FedComparator()));
			}
			daqConfFedsByPartition.get(fed.getTTCPartition()).add(fed);
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

	public static final String fedsInfoString(Map<Integer, String> fedsDescriptionMap) {
		HwInfo hi = getInstance();

		TreeMap<String, TreeMap<String, TreeSet<String>>> subsysPartitionsInfoMap = new TreeMap<String, TreeMap<String, TreeSet<String>>>();

		Map<String, String> partitionToSubsys = new HashMap<String, String>();
		Map<String, List<Integer>> fedSrcIdsByPartition = new TreeMap<String, List<Integer>>();
		Map<String, Integer> partitionSizes = new HashMap<String, Integer>();

		for (Integer fedSrcId : fedsDescriptionMap.keySet()) {
			TTCPartition partition = hi.eqs.getFEDBySourceId(fedSrcId).getTTCPartition();
			if (!fedSrcIdsByPartition.keySet().contains(partition.getName())) {
				fedSrcIdsByPartition.put(partition.getName(), new ArrayList<Integer>());
				partitionSizes.put(partition.getName(), hi.daqConfFedsByPartition.get(partition).size());
			}
			fedSrcIdsByPartition.get(partition.getName()).add(fedSrcId);
			partitionToSubsys.put(partition.getName(), partition.getSubSystem().getName());
		}
		// sort all the sub lists now
		for (String partitionName : fedSrcIdsByPartition.keySet()) {
			Collections.sort(fedSrcIdsByPartition.get(partitionName));
		}

		for (String partitionName : fedSrcIdsByPartition.keySet()) {
			String subsysName = partitionToSubsys.get(partitionName);
			List<Integer> fedSrcIds = fedSrcIdsByPartition.get(partitionName);
			for (int fedIndex = 0; fedIndex < fedSrcIds.size(); ++fedIndex) {
				int fedSrcId = fedSrcIds.get(fedIndex);
				while (fedIndex < fedSrcIds.size() - 1
						&& fedSrcIds.get(fedIndex) + 1 == fedSrcIds.get(fedIndex + 1)
						&& (fedsDescriptionMap.get(fedSrcIds.get(fedIndex)) == fedsDescriptionMap.get(fedSrcIds.get(fedIndex + 1)) || (fedsDescriptionMap
								.get(fedSrcIds.get(fedIndex)) != null && fedsDescriptionMap.get(fedSrcIds.get(fedIndex)).equals(
								fedsDescriptionMap.get(fedSrcIds.get(fedIndex + 1)))))) {
					++fedIndex;
				}

				StringBuilder fedInfo = new StringBuilder();
				if (fedSrcIds.get(fedIndex) != fedSrcId) {
					fedInfo.append("[");
				}
				fedInfo.append(String.format("%03d", fedSrcId));
				if (fedSrcIds.get(fedIndex) != fedSrcId) {
					fedInfo.append("...").append(String.format("%03d", fedSrcIds.get(fedIndex))).append("]");
				}
				if (fedsDescriptionMap.get(fedSrcId) != null && fedsDescriptionMap.get(fedSrcId).length() > 0) {
					fedInfo.append(": ").append(fedsDescriptionMap.get(fedSrcId));
				}

				FED fed = hi.eqs.getFEDBySourceId(fedSrcId);

				if (!subsysPartitionsInfoMap.containsKey(subsysName)) {
					subsysPartitionsInfoMap.put(subsysName, new TreeMap<String, TreeSet<String>>());
				}

				Map<String, TreeSet<String>> fedsPerPartitionMap = subsysPartitionsInfoMap.get(subsysName);

				if (!fedsPerPartitionMap.containsKey(partitionName)) {
					fedsPerPartitionMap.put(partitionName, new TreeSet<String>());
				}

				fedsPerPartitionMap.get(partitionName).add(fedInfo.toString());

			}
		}

		StringBuilder result = new StringBuilder();

		Iterator<Entry<String, TreeMap<String, TreeSet<String>>>> iter1 = subsysPartitionsInfoMap.entrySet().iterator();
		while (iter1.hasNext()) {
			Map.Entry<String, TreeMap<String, TreeSet<String>>> subsystemPartitionsMap = iter1.next();
			result.append("\t");
			result.append(subsystemPartitionsMap.getKey()).append(": ");
			Iterator<Entry<String, TreeSet<String>>> iter2 = subsystemPartitionsMap.getValue().entrySet().iterator();
			boolean twoTabs = false;
			while (iter2.hasNext()) {
				Map.Entry<String, TreeSet<String>> partitionFedsMap = iter2.next();
				result.append("\t");
				if (twoTabs) {
					result.append("\t");
				}
				twoTabs = true;
				String partitionName = partitionFedsMap.getKey();
				result.append(partitionName);
				result.append(" (").append(fedSrcIdsByPartition.get(partitionName).size()).append("/").append(partitionSizes.get(partitionName)).append(")");
				result.append(": ");

				Iterator<String> iter3 = partitionFedsMap.getValue().iterator();
				while (iter3.hasNext()) {
					result.append(iter3.next());
					if (iter3.hasNext()) {
						result.append(", ");
					}
				}
				if (iter2.hasNext()) {
					result.append("\n");
				}
			}
			if (iter1.hasNext()) {
				result.append("\n");
			}
		}
		return result.toString();
	}

	public static String getPartitionName(String hostName, int geoSlot, String ab) {
		// System.out.println("getting partition by hostname: " + hostName + ", AB: " + ab + ", geoSlot: " + geoSlot);
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
		// System.out.println("getting partition by fmm: " + fmm + ", AB: " + ab);
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
		// System.out.println("getting partition by number: " + partNr);
		HwInfo hi = getInstance();

		for (TTCPartition ttcp : hi.eqs.getTTCPartitions().values())
			if (ttcp.getTTCPNr() == partNr) {
				// System.out.println("partName=" + ttcp.getName());
				return ttcp.getName();
			}

		return null;
	}

	public static final String fedsHistogram(Map<Integer, ? extends Object> readouts, boolean majoritySkipDetails) {
		StringBuilder sb = new StringBuilder();

		final Map<Object, Set<Integer>> unsortedHistogram = new HashMap<Object, Set<Integer>>();
		Comparator<Object> cmprtr = new Comparator<Object>() {
			@Override
			public int compare(Object o1, Object o2) {
				if (unsortedHistogram.get(o1).size() > unsortedHistogram.get(o2).size()) {
					return -1;
				} else if (unsortedHistogram.get(o1).size() < unsortedHistogram.get(o2).size()) {
					return 1;
				} else {
					return (o1.toString().compareTo(o2.toString()));
				}
			}
		};
		SortedSet<Object> orderedKeys = new TreeSet<Object>(cmprtr);

		for (Map.Entry<Integer, ? extends Object> entry : readouts.entrySet()) {
			if (!unsortedHistogram.containsKey(entry.getValue())) {
				unsortedHistogram.put(entry.getValue(), new TreeSet<Integer>());
			}
			unsortedHistogram.get(entry.getValue()).add(entry.getKey());
		}
		orderedKeys.addAll(unsortedHistogram.keySet());

		int n = orderedKeys.size();
		if (n == 1) {
			sb.append("Observed one value only: ").append(unsortedHistogram.keySet().iterator().next().toString());
		} else {
			sb.append("Observed ").append(n).append(" values. ");
			for (final Object histKey : orderedKeys) {
				int count = unsortedHistogram.get(histKey).size();
				sb.append("\nValue '").append(histKey).append("' observed for ").append(count).append(" fed").append(count > 1 ? "s" : "");
				if (majoritySkipDetails) {
					majoritySkipDetails = false;
					continue;
				}
				sb.append(": ").append(fedsInfoString(new HashMap<Integer, String>() {
					{
						for (int i : unsortedHistogram.get(histKey)) {
							put(i, null);
						}
					}
				}));
			}
		}
		return sb.toString();
	}

	public static final Set<FED> getFedsInDaqConfiguration() {
		return getInstance().daqConfFeds;
	}

	public static final Map<TTCPartition, SortedSet<FED>> getFedsInDaqConfigurationPerPartition() {
		return getInstance().daqConfFedsByPartition;
	}

	public static final void main(String[] args) {
		Map<Integer, Integer> map = new HashMap<Integer, Integer>() {
			{
				for (int i = 100; i < 150; ++i) {
					put(i, (int) (Math.random() * 10));
				}
			}
		};

		long timeStart = System.currentTimeMillis();
		HwInfo.getInstance();
		for (Map.Entry<TTCPartition, SortedSet<FED>> entry : getFedsInDaqConfigurationPerPartition().entrySet()) {
			System.out.println(entry.getKey().getName());
			for (FED fed : entry.getValue()) {
				System.out.println("\t " + fed.getSrcId());
			}
		}
		;
		System.out.println("Retrieved Feds in DAQ configuration within: " + (System.currentTimeMillis() - timeStart) + " ms");

	}

	private class FedComparator implements Comparator<FED>, Serializable {
		private static final long serialVersionUID = 1L;

		@Override
		public int compare(FED arg0, FED arg1) {
			return (int) Math.signum(arg0.getSrcId() - arg1.getSrcId());
		}
	}
}
