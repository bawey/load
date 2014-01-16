package ch.cern.cms.load.eventData;

import java.util.HashMap;
import java.util.Map;

public class FedMask {

	private static Map<Integer, FedMask> registry = new HashMap<Integer, FedMask>();

	private final int fedId;

	private boolean slinkMasked;

	private boolean hasSlink;

	private boolean hasTts;

	private boolean ttsMasked;

	// REFERENCE
	// private static int getFEDValueAllIN(FEDWithConnectivity fed) {
	// int value = 0;
	//
	// // SLINK
	// if (!fed.isSLINKMasked() && fed.hasSLINK()) {
	// value += 1;
	// } else if (fed.isSLINKMasked() && fed.hasSLINK()) {
	// value += 4;
	// } else if (!fed.hasSLINK()) {
	// value += (4 + 1);
	// }
	//
	// // TTS
	// if (!fed.isTTSMasked() && fed.hasTTS()) {
	// value += 2;
	// } else if (fed.isTTSMasked() && fed.hasTTS()) {
	// value += 8;
	// } else if (!fed.hasTTS()) {
	// value += (8 + 2);
	// }
	//
	// return value;
	// }

	public FedMask(String s) {
		String[] tkns = s.split("&");
		this.fedId = Integer.parseInt(tkns[0]);
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
				registry.put(fedMasks[i].getFedId(), fedMasks[i]);
			}
		}
		return fedMasks;
	}

	@Override
	public String toString() {
		return "FedMask [fedId=" + fedId + ", slinkMasked=" + slinkMasked + ", hasSlink=" + hasSlink + ", hasTts=" + hasTts + ", ttsMasked=" + ttsMasked + "]";
	}

	public int getFedId() {
		return fedId;
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
}
