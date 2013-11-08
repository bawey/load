package ch.cern.cms.esper;

import java.util.Date;

import org.apache.log4j.Logger;

public class Trx {
	private static final Logger logger = Logger.getLogger(Trx.class);

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

	public static final String toDate(long time) {
		return new Date(time).toString();
	}

	public static final long timeSpan(Date a, Date b) {
		logger.info("DateSpan {" + a.toString() + " : " + b.toString() + "} = " + Math.abs(a.getTime() - b.getTime()));
		return Math.abs(a.getTime() - b.getTime());
	}

	public static final long timeSpan(long a, long b) {
		logger.info("longSpan: {" + a + " : " + b + "}=" + Math.abs(a - b));
		return Math.abs(a - b);
	}

	public static Integer toInteger(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Integer) {
			return (Integer) o;
		} else {
			return Integer.parseInt(o.toString());
		}
	}

	public static String toText(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof String) {
			return (String) o;
		} else {
			return o.toString();
		}
	}
}
