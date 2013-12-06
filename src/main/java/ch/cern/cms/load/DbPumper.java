package ch.cern.cms.load;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import ch.cern.cms.load.taps.flashlist.DataBaseFlashlistEventsTap;
import ch.cern.cms.load.taps.flashlist.NovemberFlashlistToolkit;

public class DbPumper {

	private static final Logger logger = Logger.getLogger(DbPumper.class);

	private java.sql.Connection connect = null;
	private java.sql.Statement statement = null;
	private List<String> flashBlackList = Arrays.asList(new String[] {});// "l1ts_dbjobs", "l1ts_cell" });

	static Load load = Load.getInstance();
	static Settings settings = load.getSettings();
	List<File> rootDirs = new LinkedList<File>();
	Map<String, String[]> columns = new HashMap<String, String[]>();
	java.sql.PreparedStatement addTime = null;

	public static void main(String[] args) {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			DbPumper dbp = new DbPumper();
			dbp.earlySetup(load);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void earlySetup(Load app) throws Exception {
		rootDirs = new LinkedList<File>();
		for (String s : app.getSettings().getMany("flashlistForDbDir")) {
			rootDirs.add(new File(s));
		}

		for (File dir : rootDirs) {
			for (File subdir : dir.listFiles(FF_DIRECTORY)) {
				String eventName = dirToEventName(subdir.getName());
				System.out.println(eventName);
				readStructure(subdir);
			}
		}
		initDb();
		for (File dir : rootDirs) {
			for (File subdir : dir.listFiles(FF_DIRECTORY)) {
				for (File dump : subdir.listFiles()) {
					pushIntoDb(dump, dirToEventName(subdir.getName()));
				}
			}
		}

	}

	private void pushIntoDb(File f, String eventName) throws Exception {
		if (flashBlackList.contains(eventName)) {
			return;
		}
		BufferedReader br = new BufferedReader(new FileReader(f));

		StringBuilder sql = new StringBuilder("insert into ").append(eventName).append("(fetchstamp");
		for (String field : columns.get(eventName)) {
			sql.append(", `").append(field).append("`");
		}
		sql.append(") values (").append("?");
		for (int i = 0; i < columns.get(eventName).length; ++i) {
			sql.append(", ").append("?").append("");
		}
		sql.append(")");

		java.sql.PreparedStatement insert = connect.prepareStatement(sql.toString());

		String[] tokens = null;
		while ((tokens = NovemberFlashlistToolkit.getNextValidRow(br, null)) != null) {
			Long time = Long.parseLong(tokens[0]);
			addTime.setLong(1, time);

			try {
				if (tokens.length - columns.get(eventName).length != 1) {
					for (int i = 1; i < tokens.length; ++i) {
						logger.error(columns.get(eventName)[Math.min(i, columns.get(eventName).length - 1)] + ": "
								+ tokens[i].substring(0, Math.min(100, tokens[i].length())));
					}
					throw new RuntimeException("Got more fields than columns " + eventName + " than expected");
				}
				insert.setLong(1, time);
				for (int i = 1; i < tokens.length; ++i) {
					insert.setString(i + 1, tokens[i]);
				}
				addTime.execute();
				insert.execute();
			} catch (SQLException e) {
				logger.error("SQL exception for event: " + eventName + " and query: " + sql);
				logger.error(e);
			} catch (Throwable th) {
				logger.warn("Something went wrong when parsing " + eventName, th);
			}

		}
		br.close();
	}

	private void initDb() throws Exception {

		String dbPath = DataBaseFlashlistEventsTap.getDbPath(Load.getInstance().getSettings());

		connect = DriverManager.getConnection(dbPath);

		addTime = connect.prepareStatement("insert ignore into fetchstamps(`fetchstamp`) values(?)");
		connect.createStatement().execute(
				"create table if not exists fetchstamps (fetchstamp BIGINT primary key, index `time_index` (`fetchstamp` ASC)) ENGINE = InnoDb");

		String engine = Load.getInstance().getSettings().getProperty("flashlistDbEngine", "myIsam");

		for (String table : columns.keySet()) {
			StringBuilder sb = new StringBuilder("create table if not exists ").append(table).append(" ( `fetchstamp` BIGINT");
			for (int i = 0; i < columns.get(table).length; ++i) {
				sb.append(", `").append(columns.get(table)[i]).append("` ").append(getDbType(table, columns.get(table)[i]));
			}
			if ("true".equalsIgnoreCase(Load.getInstance().getSettings().getProperty("flashlistDbIndexTimestamps"))) {
				sb.append(",INDEX `fetchtime_index` (`fetchstamp` ASC)");
			}
			sb.append(") ENGINE = ").append(engine);
			try {
				statement = connect.createStatement();
				statement.execute(sb.toString());
			} catch (Exception e) {
				System.err.println(sb.toString());
				throw e;
			}
		}
	}

	private String dirToEventName(String name) {
		if (name.contains(":")) {
			return name.substring(name.lastIndexOf(":") + 1);
		}
		return name;
	}

	private void readStructure(File dir) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(dir.listFiles(FF_0)[0]));
		String eventName = dirToEventName(dir.getName());
		if (!flashBlackList.contains(eventName)) {
			columns.put(eventName, NovemberFlashlistToolkit.timeAndLine(br.readLine())[1].split(","));
		}
		br.close();
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

	private String getDbType(String table, String column) {
		if (column.equals("FED_INFO_MAP")) {
			return "LONGTEXT";
		}
		return "TEXT";
	}

}
