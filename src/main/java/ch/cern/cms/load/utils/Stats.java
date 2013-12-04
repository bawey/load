package ch.cern.cms.load.utils;

import java.util.Collection;
import java.util.Iterator;

public class Stats {

	public static double sum(Iterable<? extends Number> s) {
		double sum = 0;
		Iterator<? extends Number> i = s.iterator();
		while (i.hasNext()) {
			sum += i.next().doubleValue();
		}
		return sum;
	}

	private static int length(Iterable<?> s) {
		if (s instanceof Collection<?>) {
			return ((Collection<?>) s).size();
		}
		int length = 0;
		Iterator<?> i = s.iterator();
		while (i.hasNext()) {
			i.next();
			++length;
		}
		return length;
	}

	public static double mean(Iterable<? extends Number> s) {
		return sum(s) / length(s);
	}

	public static double min(Iterable<? extends Number> s) {
		Iterator<? extends Number> i = s.iterator();
		double min = Double.MAX_VALUE;
		while (i.hasNext()) {
			min = Math.min(min, i.next().doubleValue());
		}
		return min;
	}

	public static double max(Iterable<? extends Number> s) {
		Iterator<? extends Number> i = s.iterator();
		double max = Double.MIN_VALUE;
		while (i.hasNext()) {
			max = Math.max(max, i.next().doubleValue());
		}
		return max;
	}

	public static String summarize(String label, Iterable<? extends Number> s) {
		StringBuilder sb = new StringBuilder(label);
		sb.append("length: ").append(length(s)).append(", max: ").append(max(s)).append(", min: ").append(min(s)).append(", mean: ").append(mean(s))
				.append(", total: ").append(sum(s));
		return sb.toString();
	}
}
