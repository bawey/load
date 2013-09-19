package ch.cern.cms.load.mocks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import ch.cern.cms.load.eventData.EventProcessorStatus;

public class MockEPSEventParser {
	public List<EventProcessorStatus> bruteParse(File input) throws IOException {

		List<EventProcessorStatus> list = new LinkedList<EventProcessorStatus>();

		BufferedReader br = new BufferedReader(new FileReader(input));
		String line = null;

		long nbAccepted = 0;
		List<Integer> epMacroStateInt = null;
		int age = 0;
		int lid = 0;
		;
		int instance = 0;
		int runNumber = 0;
		String stateName = null;
		Date timestamp = null;
		long updateTime = 0;
		String context = null;
		List<String> epMacroStateStr = null;
		long nbProcessed = 0;
		List<Integer> epMicroStateInt = null;
		int sessionId = 0;

		while ((line = br.readLine()) != null) {
			if (line.contains("nbAccepted")) {
				nbAccepted = getLong(line);
			} else if (line.contains("epMacroStateInt")) {

			} else if (line.contains("_age")) {

			} else if (line.contains("lid")) {

			} else if (line.contains("instance")) {

			} else if (line.contains("runNumber")) {

			} else if (line.contains("stateName")) {

			} else if (line.contains("timestamp")) {

			} else if (line.contains("_updatetime")) {

			} else if (line.contains("context")) {
				context = getString(line);
			} else if (line.contains("epMacroStateStr")) {

			} else if (line.contains("nbProcessed")) {
				nbProcessed = getLong(line);
			} else if (line.contains("epMicroStateInt")) {
				epMicroStateInt = getIntList(line);
			} else if (line.contains("sessionid") && epMicroStateInt != null) {

				list.add(new EventProcessorStatus(nbAccepted, epMacroStateInt, age, lid, instance, runNumber, stateName, timestamp,
						updateTime, context, epMacroStateStr, nbProcessed, epMicroStateInt, sessionId));

				epMicroStateInt = null;

			}
		}
		return list;
	}

	private String getString(String line) {
		String[] tokens = line.split("=>");
		return tokens.length > 1 ? tokens[1].substring(tokens[1].indexOf('\'') + 1, tokens[1].lastIndexOf('\'')) : null;
	}

	private int getInt(String line) {
		return Integer.parseInt(getString(line));
	}

	private long getLong(String line) {
		return Long.parseLong(getString(line));
	}

	private String[] getTokens(String line) {
		String str = getString(line);
		str = str.substring(1, str.length() - 1);
		return str.split(",");
	}

	private List<Integer> getIntList(String line) {
		List<Integer> ints = new LinkedList<Integer>();
		for (String token : getTokens(line)) {
			ints.add(Integer.parseInt(token));
		}
		return ints;
	}
}
