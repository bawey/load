package ch.cern.cms.esper;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	public static final String formatMs(long ms) {
		StringBuilder sb = new StringBuilder();

		Integer msec = (int) (ms % 1000);
		Integer sec = (int) ((ms / 1000) % 60);
		Integer mins = (int) (ms / 1000 / 60) % 60;
		Integer hours = (int) (ms / 1000 / 60 / 60);

		return sb.append(String.format("%02d", hours)).append("h:").append(String.format("%02d", mins)).append("m:").append(String.format("%02d", sec))
				.append("s.").append(String.format("%03d", msec)).toString();
	}

	public static final String format(double d) {
		return String.format("%.3f", d);
	}

	public static final String format(String fmt, Number n) {
		return String.format(fmt, n);
	}

	public static final String regExtract(String src, String regexp, int groupNo) {
		Pattern p = Pattern.compile(regexp);
		Matcher matcher = p.matcher(src);
		matcher.find();
		return matcher.group(groupNo);
	}

	public static final Collection<Integer> indexesOf(Object needle, List<?> list) {
		Collection<Integer> rslt = new TreeSet<Integer>();
		for (int i = 0; i < list.size(); ++i) {
			if (needle != null) {
				if (needle.equals(list.get(i))) {
					rslt.add(i);
				}
			} else {
				if (list.get(i) == null) {
					rslt.add(i);
				}
			}
		}
		return rslt;
	}

	public static final List<Object> subList(List<?> list, Collection<Integer> indexes) {
		List<Object> rslt = new ArrayList<Object>(indexes.size());
		for (Integer index : indexes) {
			rslt.add(list.get(index));
		}
		return rslt;
	}

	public static final double getBusyProcessorsRatio(List<String> macro, List<String> micro) {
		// get the indexes corresponding to running ones
		// System.out.println("macro: " + macro);
		// System.err.println("micro: " + micro);
		Collection<Integer> indexes = indexesOf("3", macro);
		if (indexes.size() == 0) {
			return 0;
		}
		List<Object> runningStates = subList(micro, indexes);
		runningStates.removeAll(new LinkedList<String>() {
			{
				add("2");
			}
		});
		// System.out.println(runningStates);
		return runningStates.size() / (double) indexes.size();
	}

	public static void main(String[] args) {
		System.out.println(format(3.1415) + " " + format(.56789));
		System.out.println(format(3));
	}

	public static final String reformat(String input, String separator, String newSeparator) {
		String[] tokens = input.split(separator);
		Arrays.sort(tokens);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < tokens.length - 1; ++i) {
			sb.append(tokens[i]).append(newSeparator);
		}
		return sb.append(tokens[tokens.length - 1]).toString();
	}

	public static final boolean compareHostnames(String a, String b) {
		int indexA = a.length() - 1;
		while (Character.isDigit(a.charAt(indexA))) {
			--indexA;
		}
		if (a.charAt(indexA) != ':') {
			indexA = a.length() - 1;
		}

		int indexB = b.length() - 1;
		while (Character.isDigit(b.charAt(indexB))) {
			--indexB;
		}
		if (b.charAt(indexB) != ':') {
			indexB = b.length() - 1;
		}

		if (indexA == indexB) {
			while (indexA >= 0) {
				if (a.charAt(indexA) != b.charAt(indexA)) {
					return false;
				}
				--indexA;
			}
		}
		return true;
	}

		// public static final String getHost(String http) {
	// int splitPoint = http.length();
	// char c = http.charAt(splitPoint-1);
	// while (Character.isDigit(c) || c == ':') {
	// --splitPoint;
	// }
	//
	// }
	//
	// public static final String areSameHosts(String http1, String http1) {
	//
	// }
}