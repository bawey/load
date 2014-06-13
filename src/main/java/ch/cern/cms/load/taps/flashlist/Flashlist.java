package ch.cern.cms.load.taps.flashlist;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import ch.cern.cms.load.EventProcessor;
import ch.cern.cms.load.Load;

public class Flashlist extends LinkedList<Map<String, Object>> {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(Flashlist.class);
	private String streamName;
	private Long fetchstamp;

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
				if(fetchstamp==null){
					fetchstamp = System.currentTimeMillis();
				}
				if (keys == null) {
					keys = line.split(",");
				} else {
					String[] tokens = smartSplit(line);
					if (tokens.length != keys.length) {
						throw new RuntimeException("tokens and keys lengths differ for: " + url);
					}
					/**
					 * creating actual events. should already know which rows should be unrolled
					 **/
					Map<String, List<?>> unrolledValues = new HashMap<String, List<?>>();
					int unrolledSize = -1;
					for (int i = 0; i < tokens.length; ++i) {
						if (Load.getInstance().getResolver().isRolled(keys[i], listName)) {
							unrolledValues.put(keys[i], unrollValues(tokens[i], keys[i], listName));
							if (unrolledSize == -1) {
								unrolledSize = unrolledValues.get(keys[i]).size();
							} else {
								assert (unrolledSize == unrolledValues.get(keys[i]).size());
							}
						}
					}
					for (int a = 0; a < Math.max(1, unrolledSize); ++a) {
						Map<String, Object> map = new HashMap<String, Object>();
						for (int i = 0; i < tokens.length; ++i) {
							if (unrolledValues.containsKey(keys[i])) {
								map.put(keys[i], unrolledValues.get(keys[i]).get(a));
							} else {
								map.put(keys[i], Load.getInstance().getResolver().convert(tokens[i], keys[i], listName));
							}
						}
						map.put("catchstamp", Long.parseLong(url.getFile().substring(url.getFile().lastIndexOf('/') + 1)));
						this.add(map);
					}
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
			event.put("fetchstamp", fetchstamp);
			ep.getRuntime().sendEvent(event, streamName);
		}
	}

	private List<?> unrollValues(String rolledString, String fieldName, String listName) {
		String[] tokens = rolledString.trim()
				.substring((rolledString.startsWith("[") ? 1 : 0), (rolledString.endsWith("]") ? rolledString.length() - 1 : rolledString.length())).split(",");
		List<Object> list = new ArrayList<Object>(tokens.length);
		for (String token : tokens) {
			list.add(Load.getInstance().getResolver().convert(token, fieldName, listName));
		}
		return list;
	}

	public static String[] smartSplit(String str) {
		List<String> lst = new LinkedList<String>();
		try {
			int a = 0, b = 0;

			while (a < str.length() && b < str.length()) {
				// a has to stop on opening quote, b on closing
				while (str.charAt(a) != '"') {
					++a;
				}
				while (!(str.charAt(b) == '"' && (str.length() - b == 1 || str.charAt(b + 1) == ',')) || b == a) {
					if (isCloseable(str.charAt(b)) && !isFakeClosable(b, str)) {
						b = findClosing(str, b);
					} else {
						++b;
					}
				}
				if (a < 0 || b < 0) {
					logger.error("a:" + a + ", b:" + b);
				}
				lst.add(str.substring(a + 1, b));
				a = ++b;
			}
			return lst.toArray(new String[lst.size()]);
		} catch (StringIndexOutOfBoundsException sioobe) {
			throw new RuntimeException("Failed to smart-split this: " + str, sioobe);
		}
	}

	private static boolean isCloseable(char c) {
		return (c == '[' || c == '{');
	}

	private static List<Character> fakeClosableNeighbours = Arrays.asList(new Character[] { '<', '>' });

	// an awkward fix for rare cases of stray '[' characters being embedded in a regular text
	private static boolean isFakeClosable(int startPosition, String str) {
		char chr = ' ';
		int position = startPosition;
		while (Character.isWhitespace((chr = str.charAt(++position)))) {

		}
		if (Character.isLetter(chr) || fakeClosableNeighbours.contains(chr)) {
			return true;
		}
		// ahhh, need to check before as well..
		position = startPosition;
		while (Character.isWhitespace((chr = str.charAt(--position)))) {

		}
		if (Character.isLetter(chr) || fakeClosableNeighbours.contains(chr)) {
			return true;
		}
		return false;
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
				if (++pos == s.length()) {
					System.err.println(s);
					throw new RuntimeException("Ran out of string trying to close: " + c + ": ");
				}
			}
		}
		return pos;
	}
}
