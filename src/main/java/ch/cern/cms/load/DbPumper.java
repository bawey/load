package ch.cern.cms.load;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ch.cern.cms.load.taps.flashlist.Flashlist;

import com.mysql.jdbc.PreparedStatement;
import com.mysql.jdbc.ResultSet;

public class DbPumper {

	private java.sql.Connection connect = null;
	private java.sql.Statement statement = null;
	private PreparedStatement preparedStatement = null;
	private ResultSet resultSet = null;

	static Load load = Load.getInstance();
	static Settings settings = load.getSettings();
	List<File> rootDirs = new LinkedList<File>();
	Map<String, String[]> columns = new HashMap<String, String[]>();

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
		BufferedReader br = new BufferedReader(new FileReader(f));
		int lineNo = 0;
		String line = null;
		while ((line = br.readLine()) != null) {
			if (++lineNo == 1 && f.getName().equals("0")) {
				continue;
			}
			Long time = getTime(line);
			String[] fields = Flashlist.smartSplit(getRow(line));

			statement = connect.createStatement();

			StringBuilder sql = new StringBuilder("insert into ").append(eventName).append("(fetchstamp");
			for (String field : columns.get(eventName)) {
				sql.append(", `").append(field).append("`");
			}
			sql.append(") values (").append(time);
			for (String field : fields) {
				//field.replace("\"", "\\\"")
				sql.append(", '").append("XYZ").append("'");
			}
			sql.append(")");
			try {
				statement.execute(sql.toString());
			} catch (Exception e) {
				System.err.println(sql.toString());
				throw e;
			}

		}
		br.close();
	}

	private void initDb() throws Exception {
		connect = DriverManager.getConnection("jdbc:mysql://localhost/flashlists?" + "user=root&password=sql48ppf");
		for (String table : columns.keySet()) {
			StringBuilder sb = new StringBuilder("create table if not exists ").append(table).append(" ( `fetchstamp` BIGINT");
			for (int i = 0; i < columns.get(table).length; ++i) {
				sb.append(", `").append(columns.get(table)[i]).append("` TEXT");
			}
			sb.append(") ENGINE = InnoDb");
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
		columns.put(dirToEventName(dir.getName()), getRow(br.readLine()).split(","));
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
