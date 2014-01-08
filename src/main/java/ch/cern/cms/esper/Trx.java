package ch.cern.cms.esper;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
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

	public static final Date toDate(long time) {
		return new Date(time);
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

	public static final FakeMap tuple(Object a, Object b) {
		FakeMap map = null;

		Collection<?> as = null;
		Collection<?> bs = null;

		if (a.getClass().isArray()) {
			ArrayList<Object> list = new ArrayList<Object>(Array.getLength(a));
			for (int i = 0; i < Array.getLength(a); ++i) {
				list.add(Array.get(a, i));
			}
			as = list;
		} else if (a instanceof Collection<?>) {
			as = (Collection<?>) a;
		}
		if (b.getClass().isArray()) {
			ArrayList<Object> list = new ArrayList<Object>(Array.getLength(b));
			for (int i = 0; i < Array.getLength(b); ++i) {
				list.add(Array.get(b, i));
			}
			bs = list;
		} else if (b instanceof Collection<?>) {
			bs = (Collection<?>) b;
		}

		if (as != null && bs != null) {

			assert (as.size() == bs.size());
			map = new FakeMap(bs.size());
			Iterator<?> ita = as.iterator();
			Iterator<?> itb = bs.iterator();
			while (ita.hasNext() && itb.hasNext()) {
				map.put(ita.next(), itb.next());
			}
		} else {
			map = new FakeMap(1);
			map.put(a, b);
		}
		return map;
	}
	
	
	public static final boolean isNonvariant(Object o) {
		System.out.println(o.getClass());
		System.out.println(o.toString());
		return false;
	}

	
	public static final class FakeMap extends HashMap<Object, Object> {
		private static final long serialVersionUID = 1L;

		public FakeMap(int capacity) {
			super(capacity);
		}

		public Object take(Object key) {
			return super.get(key);
		}
	}

	
	
}
