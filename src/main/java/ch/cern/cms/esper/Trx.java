package ch.cern.cms.esper;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
		return Math.abs(a.getTime() - b.getTime());
	}

	public static final long timeSpan(long a, long b) {
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

	public static <N extends Object, H extends Object> boolean inArray(N needle, H[] haystack) {
		for (H cand : haystack) {
			if (cand.equals(needle)) {
				return true;
			}
		}
		return false;
	}

	public static boolean inArray(Integer needle, Integer[] haystack) {
		return inArray((Object) needle, haystack);
	}

	public static final Map<?, ? extends Object> tuple(Object a, Object b) {
		Map<Object, Object> map = null;

		if (a instanceof Collection<?> && b instanceof Collection<?>) {
			Collection<?> as = (Collection<?>) a;
			Collection<?> bs = (Collection<?>) b;
			assert (as.size() == bs.size());
			map = new HashMap<Object, Object>(bs.size());
			Iterator<?> ita = as.iterator();
			Iterator<?> itb = bs.iterator();
			while (ita.hasNext() && itb.hasNext()) {
				map.put(ita.next(), itb.next());
			}
		} else {
			map = new HashMap<Object, Object>(1);
			map.put(a, b);
		}
		return map;
	}
}
