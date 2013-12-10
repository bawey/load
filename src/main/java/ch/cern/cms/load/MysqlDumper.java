package ch.cern.cms.load;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import ch.cern.cms.load.taps.flashlist.DataBaseFlashlistEventsTap;
import ch.cern.cms.load.taps.flashlist.NovemberFlashlistToolkit;

public class MysqlDumper {

	private static final Logger logger = Logger.getLogger(MysqlDumper.class);

	private java.sql.Connection connect = null;
	private java.sql.Statement statement = null;
	private List<String> flashBlackList = Arrays.asList(new String[] {});// "l1ts_dbjobs", "l1ts_cell" });

	static Load load = Load.getInstance();
	static Settings settings = load.getSettings();
	List<File> rootDirs = new LinkedList<File>();
	Map<String, String[]> columns = new HashMap<String, String[]>();
	PreparedStatement addTime = null;
	Map<String, PreparedStatement> inserts = new HashMap<String, PreparedStatement>();

	public static void main(String[] args) {
		try {
			String dbType = load.getSettings().getProperty(DataBaseFlashlistEventsTap.KEY_DB_TYPE);
			if ("mysql".equalsIgnoreCase(dbType)) {
				Class.forName("com.mysql.jdbc.Driver");
				MysqlDumper dbp = new MysqlDumper();
				dbp.earlySetup(load);
			} else if ("mongo".equalsIgnoreCase(dbType) || "mongodb".equalsIgnoreCase(dbType)) {
				MongoDumper.main(args);
			}
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
				System.out.println("DIR: " + eventName);
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
		PreparedStatement insert = inserts.get(eventName);

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

				addTime.addBatch();
				insert.addBatch();

				if (Math.random() < 0.01) {
					executeBathches();
				}

			} catch (SQLException e) {
				logger.error("SQL exception for event: " + eventName);
				logger.error(e);
			} catch (Throwable th) {
				logger.warn("Something went wrong when parsing " + eventName, th);
			}

		}
		br.close();
		executeBathches();
	}

	private void prepareStatements() throws SQLException {
		System.out.println("preparing insert statements");
		for (String eventName : columns.keySet()) {
			System.out.println(eventName);
			StringBuilder sql = new StringBuilder("insert into ").append(eventName).append("(fetchstamp");
			for (String field : columns.get(eventName)) {
				sql.append(", `").append(field).append("`");
			}
			sql.append(") values (").append("?");
			for (int i = 0; i < columns.get(eventName).length; ++i) {
				sql.append(", ").append("?").append("");
			}
			sql.append(")");

			inserts.put(eventName, connect.prepareStatement(sql.toString()));
		}
	}

	private void executeBathches() throws SQLException {
		addTime.executeBatch();
		for (PreparedStatement ps : inserts.values()) {
			ps.executeBatch();
		}
	}

	private void initDb() throws Exception {

		String dbPath = DataBaseFlashlistEventsTap.getDbPath(Load.getInstance().getSettings());

		String engine = Load.getInstance().getSettings().getProperty("flashlistDbEngine", "myIsam");
		connect = DriverManager.getConnection(dbPath);

		addTime = connect.prepareStatement("insert ignore into fetchstamps(`fetchstamp`) values(?)");
		connect.createStatement().execute(
		// "create table if not exists fetchstamps (fetchstamp BIGINT primary key, index `time_index` (`fetchstamp` ASC)) ENGINE = " +
		// engine);
				"create table if not exists fetchstamps (fetchstamp BIGINT) ENGINE = " + engine);

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
		prepareStatements();
	}

	public static final String dirToEventName(String name) {
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

	public static final FileFilter FF_0 = new FileFilter() {
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
