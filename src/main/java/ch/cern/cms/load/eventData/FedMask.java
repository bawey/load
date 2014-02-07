package ch.cern.cms.load.eventData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class FedMask {

	private static Map<Integer, FedMask> registry = new HashMap<Integer, FedMask>();

	private final int fedSrcId;

	private boolean slinkMasked;

	private boolean hasSlink;

	private boolean hasTts;

	private boolean ttsMasked;

	public FedMask(String s) {
		String[] tkns = s.split("&");
		this.fedSrcId = Integer.parseInt(tkns[0]);
		int i = Integer.parseInt(tkns[1]);
		switch ((i & 4) + (i & 1)) {
		case 4:
			this.slinkMasked = true;
		case 1:
			this.hasSlink = true;
		default:
			break;
		}
		switch ((i & 8) + (i & 2)) {
		case 8:
			this.ttsMasked = true;
		case 2:
			this.hasTts = true;
		default:
			break;
		}
	}

	public static final FedMask[] parse(String sliceMaskString) {
		String[] fedStrings = sliceMaskString.split("%");
		FedMask[] fedMasks = new FedMask[fedStrings.length];
		for (int i = 0; i < fedStrings.length; ++i) {
			fedMasks[i] = new FedMask(fedStrings[i]);
			synchronized (FedMask.class) {
				registry.put(fedMasks[i].getFedSrcId(), fedMasks[i]);
			}
		}
		return fedMasks;
	}

	@Override
	public String toString() {
		return "FedMask [fedSrcId=" + fedSrcId + ", slinkMasked=" + slinkMasked + ", hasSlink=" + hasSlink + ", hasTts=" + hasTts + ", ttsMasked=" + ttsMasked
				+ "]";
	}

	public int getFedSrcId() {
		return fedSrcId;
	}

	public boolean isSlinkMasked() {
		return slinkMasked;
	}

	public boolean getHasSlink() {
		return hasSlink;
	}

	public boolean getHasTts() {
		return hasTts;
	}

	public boolean isTtsMasked() {
		return ttsMasked;
	}

	public boolean isSlinkEnabled() {
		return hasSlink && !slinkMasked;
	}

	public boolean isTtsEnabled() {
		return hasTts && !ttsMasked;
	}

	public static boolean isFedActive(int fedId) {
		FedMask fedOfInterest = registry.get(fedId);
		return fedOfInterest != null && (fedOfInterest.isTtsEnabled() || fedOfInterest.isSlinkEnabled());
	}

	public static final Set<Integer> getActiveFedSrcIds() {
		return new HashSet<Integer>() {
			{
				for (Entry<Integer, FedMask> tuple : registry.entrySet()) {
					if (tuple.getValue().isTtsEnabled() || tuple.getValue().isSlinkEnabled()) {
						add(tuple.getKey());
					}
				}
			}
		};
	}
}
