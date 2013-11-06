package ch.cern.cms.esper;

public class Trx {
	public static final int toInt(Object o) {
		return Integer.parseInt(o.toString());
	}

	public static final long toLong(Object o) {
		return Long.parseLong(o.toString());
	}

	public static final double toDouble(Object o) {
		return Double.parseDouble(o.toString());
	}

	public static final boolean inIgnoreCase(String needle, String[] haystack) {
		for (String s : haystack) {
			if (s.equalsIgnoreCase(needle)) {
				return true;
			}
		}
		return false;
	}
}
