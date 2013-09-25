package ch.cern.cms.load.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ch.cern.cms.load.eventProcessing.EventProcessor;

public class FleischList extends LinkedList<Map<String, String>> {

	private String listName;
	private static final long serialVersionUID = 1L;

	public FleischList(File file) {
		super();
		listName = file.getParentFile().getName();
		String[] keys = null;
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line = null;
			while ((line = br.readLine()) != null) {
				if (keys == null) {
					keys = line.split(",");
				} else {
					Map<String, String> map = new HashMap<String, String>();
					String[] tokens = smartSplit(line);
					if (tokens.length != keys.length) {
						throw new RuntimeException("tokens and keys lengths differ for file: " + file.getAbsolutePath());
					}
					for (int i = 0; i < tokens.length; ++i) {
						map.put(keys[i], tokens[i]);
					}
					this.add(map);
				}
			}
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
		for (Map<String, String> event : this) {
			//System.out.println("sending " + listName + ": " + event);
			ep.getRuntime().sendEvent(event, listName);
		}
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
