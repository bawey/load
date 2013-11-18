package ch.cern.cms.load.taps.flashlist;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.codehaus.plexus.util.CollectionUtils;

import ch.cern.cms.load.EventProcessor;
import ch.cern.cms.load.Load;

public class Flashlist extends LinkedList<Map<String, Object>> {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(Flashlist.class);
	private String streamName;

	public Flashlist(URL url, String listName) {
		super();
		this.streamName = listName;
		String[] keys = null;
		try {
			URLConnection conn = url.openConnection();
			InputStreamReader isr = new InputStreamReader(conn.getInputStream());
			BufferedReader br = new BufferedReader(isr);
			String line = null;
			while ((line = br.readLine()) != null) {
				if (keys == null) {
					keys = line.split(",");
				} else {
					Map<String, Object> map = new HashMap<String, Object>();
					String[] tokens = smartSplit(line);
					if (tokens.length != keys.length) {
						throw new RuntimeException("tokens and keys lengths differ for: " + url);
					}
					for (int i = 0; i < tokens.length; ++i) {
						map.put(keys[i], Load.getInstance().getResolver().convert(tokens[i], keys[i], listName));
					}
					map.put("catchstamp", Long.parseLong(url.getFile().substring(url.getFile().lastIndexOf('/') + 1)));
					this.add(map);
				}
			}
			isr.close();
			br.close();
		} catch (IOException e) {
			throw new RuntimeException("No mistakes allowed!", e);
		}
	}

	public static List<Map<String, String>> parse(File file) throws IOException {
		List<Map<String, String>> results = null;
		return results;
	}

	public void emit(EventProcessor ep) {
		for (Map<String, Object> event : this) {
			for (Map<String, Object> unrolled : unroll(event, streamName)) {
				ep.getRuntime().sendEvent(unrolled, streamName);
			}
		}
	}

	private Collection<Map<String, Object>> unroll(Map<String, Object> event, String name) {
		Set<Map<String, Object>> result = new HashSet<Map<String, Object>>();
		Set<String> unrollable = new HashSet<String>();
		for (String key : event.keySet()) {
			if (Load.getInstance().getResolver().isRolled(key, name)) {
				unrollable.add(key);
			}
		}
		if (unrollable.isEmpty()) {
			result.add(event);
		} else {
			int unrolledLength = -1;
			Map<String, List<?>> unrolled = new HashMap<String, List<?>>();
			for (String key : unrollable) {
				if (event.get(key) instanceof List<?>) {
					unrolled.put(key, (List<?>) event.get(key));
					if (unrolledLength == -1) {
						unrolledLength = unrolled.get(key).size();
					}
					assert (unrolled.get(key).size() == unrolledLength);
				} else {
					throw new RuntimeException("Attempted to unroll a non-list object");
				}
			}
			for (int i = 0; i < unrolledLength; ++i) {
				Map<String, Object> newEvent = new HashMap<String, Object>();
				for (String rootKey : event.keySet()) {
					if (unrolled.keySet().contains(rootKey)) {
						newEvent.put(rootKey, unrolled.get(rootKey).get(i));
					} else {
						newEvent.put(rootKey, event.get(rootKey));
					}
				}
				result.add(newEvent);
			}
		}
		return result;
	}

	public static String[] smartSplit(String str) {
		List<String> lst = new LinkedList<String>();
		int a = 0, b = 0;

		while (a < str.length() && b < str.length()) {
			// a has to stop on opening quote, b on closing
			while (str.charAt(a) != '"') {
				++a;
			}
			while (str.charAt(b) != '"' || b == a) {
				if (isCloseable(str.charAt(b))) {
					b = findClosing(str, b);
				} else {
					++b;
				}
			}
			lst.add(str.substring(a + 1, b));
			a = ++b;
		}
		return lst.toArray(new String[lst.size()]);
	}

	private static boolean isCloseable(char c) {
		return (c == '[' || c == '{');
	}

	private static char closingChar(char c) {
		switch (c) {
		case '[':
			return ']';
		case '{':
			return '}';
		default:
			return 0;
		}
	}

	private static int findClosing(String s, int pos) {
		char c = s.charAt(pos++);
		while (s.charAt(pos) != closingChar(c)) {
			if (isCloseable(s.charAt(pos))) {
				pos = findClosing(s, pos);
			} else {
				++pos;
			}
		}
		return pos;
	}
}
