package ch.cern.cms.load;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DbPumper {

	static Load load = Load.getInstance();
	static Settings settings = load.getSettings();
	List<File> rootDirs = new LinkedList<File>();
	Map<String, String[]> columns = new HashMap<String, String[]>();

	public static void main(String[] args) {
		try {
			DbPumper dbp = new DbPumper();
			dbp.earlySetup(load);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void earlySetup(Load app) throws IOException {
		rootDirs = new LinkedList<File>();
		for (String s : app.getSettings().getMany("flashlistForDbDir")) {
			rootDirs.add(new File(s));
		}

		for (File dir : rootDirs) {
			for (File subdir : dir.listFiles(FF_DIRECTORY)) {
				String eventName = dirToEventName(subdir.getName());
				System.out.println(eventName);
				readStructure(subdir);
				createDbIfNeeded();
			}
		}

	}

	private void createDbIfNeeded() {

	}

	private String dirToEventName(String name) {
		if (name.contains(":")) {
			return name.substring(name.lastIndexOf(":") + 1);
		}
		return name;
	}

	private void readStructure(File dir) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(dir.listFiles(FF_0)[0]));
		columns.put(dir.getName(), getRow(br.readLine()).split(","));
		br.close();
	}

	private long getTime(String line) {
		return Long.parseLong(line.substring(0, line.indexOf(':')));
	}

	private String getRow(String line) {
		return line.substring(line.indexOf(':') + 1).trim();
	}

	private static final FileFilter FF_0 = new FileFilter() {
		@Override
		public boolean accept(File pathname) {
			return pathname.getName().equals("0");
		}
	};

	public static final FileFilter FF_DIRECTORY = new FileFilter() {
		@Override
		public boolean accept(File pathname) {
			return pathname.isDirectory();
		}
	};

}
