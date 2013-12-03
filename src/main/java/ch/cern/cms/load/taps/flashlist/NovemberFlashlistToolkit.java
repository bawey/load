package ch.cern.cms.load.taps.flashlist;

import java.io.BufferedReader;
import java.io.IOException;

import org.apache.log4j.Logger;

/**
 * 
 * @author Tomasz Bawej Contains utility methods for flashlists dumped in the same format as in November 2013, where each row is prepended
 *         by timestamp and colon.
 */

public class NovemberFlashlistToolkit {

	private final static Logger logger = Logger.getLogger(NovemberFlashlistToolkit.class);

	public static String getNonLabelLine(BufferedReader br) throws IOException {
		String line = "";
		while ((line = br.readLine()) != null && !line.contains("\"")) {

		}
		return line;
	}

	public static String[] timeAndLine(String lineFromFile) {
		int x = lineFromFile.indexOf(':');
		return new String[] { lineFromFile.substring(0, x).trim(), lineFromFile.substring(x + 1).trim() };
	}

	public static String[] getNextValidRow(BufferedReader br, String[] input) throws IOException {
		if (input == null) {
			String readline = getNonLabelLine(br);
			if (readline == null) {
				return null;
			}
			input = NovemberFlashlistToolkit.timeAndLine(readline);
		}
		String[] result = null;
		if (!input[1].endsWith("\"")) {
			StringBuilder sb = new StringBuilder(input[1]);
			while (sb.charAt(sb.length() - 1) != '"') {
				String nextLine = getNonLabelLine(br);
				if (nextLine == null) {
					logger.warn("Found an incomplete row at the end of the file, abandoning the incomplete.");
					logger.warn("row: " + sb.toString());
					logger.warn("next: " + nextLine);
					return null;
				} else {
					String[] nextInput = NovemberFlashlistToolkit.timeAndLine(nextLine);
					if (nextInput[1].charAt(0) != '"') {
						sb.append("\n").append(nextInput[1]);
					} else {
						logger.warn("Found a new row while looking for the missing part of previous one. Dropping the incomplete");
						logger.warn("row: " + sb.toString());
						logger.warn("next: " + nextLine);
						return getNextValidRow(br, nextInput);
					}
				}
			}
			input[1] = sb.toString();
		}
		String[] tokens = Flashlist.smartSplit(input[1]);
		result = new String[tokens.length + 1];
		result[0] = input[0];
		for (int i = 1; i < result.length; ++i) {
			result[i] = tokens[i - 1];
		}
		return result;
	}

}
