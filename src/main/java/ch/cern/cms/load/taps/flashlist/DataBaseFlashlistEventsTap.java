package ch.cern.cms.load.taps.flashlist;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import oracle.xml.transx.loader;

import ch.cern.cms.load.Load;
import ch.cern.cms.load.Settings;

public class DataBaseFlashlistEventsTap extends AbstractFlashlistEventsTap {

	private class Columns extends HashMap<String, String[]> {
		private static final long serialVersionUID = 1L;
	}

	public static final String KEY_DB_MODE = "flashlistDbMode";
	public static final String KEY_DB_TYPE = "flashlistDbType";
	public static final String KEY_DB_HOST = "flashlistDbHost";
	public static final String KEY_DB_NAME = "flashlistDbName";
	public static final String KEY_DB_USER = "flashlistDbUser";
	public static final String KEY_DB_PASS = "flashlistDbPass";
	public static final String KEY_RETRIEVAL_TIMESTAMP_NAME = "retrievalTimestampName";

	private Connection conn = null;
	private Statement statement = null;
	private PreparedStatement showTables = null;
	private PreparedStatement descTable = null;
	private PreparedStatement select = null;
	private String fetchstamp = null;

	private Columns columns = null;

	private String fetchstampName = null;
	private Long timespanStart = null;
	private Long timespanEnd = null;

	/**
	 * 
	 * @param load
	 * @param path
	 *            database adress
	 */
	public DataBaseFlashlistEventsTap(Load load) {
		super(load, getDbPath(load.getSettings()));
		this.job = this.dbjob;
	}

	@Override
	public void preRegistrationSetup(Load expert) {
		columns = new Columns();
		fetchstampName = this.controller.getSettings().getProperty(KEY_RETRIEVAL_TIMESTAMP_NAME, "fetchstamp");
		timespanStart = this.controller.getSettings().getLong(Settings.KEY_TIMER_START, 0l);
		timespanEnd = this.controller.getSettings().getLong(Settings.KEY_TIMER_END, System.currentTimeMillis());

		try {
			String type = path.split(":")[1];
			Class.forName("com." + type + ".jdbc.Driver");
			conn = DriverManager.getConnection(path);
		} catch (Exception e) {
			throw new RuntimeException("Failed to establish connection to flashlists DB", e);
		}
		try {
			prepareStatements();
			ResultSet rs = showTables.executeQuery();
			ResultSetMetaData meta = rs.getMetaData();

			while (rs.next()) {
				String tableName = rs.getString(1);
				ResultSet details = conn.createStatement().executeQuery("describe " + tableName);

				List<String> headers = new LinkedList<String>();
				while (details.next()) {
					headers.add(details.getString(1));
				}
				columns.put(tableName, headers.toArray(new String[headers.size()]));
			}

		} catch (Exception e) {
			throw new RuntimeException("Initial setup failed", e);
		}
	}

	@Override
	public void registerEventTypes(Load expert) {
		for (String eventName : columns.keySet()) {
			Map<String, Object> types = new HashMap<String, Object>();
			for (String fieldName : columns.get(eventName)) {
				Class<?> type = String.class;
				if (fieldName.equals(fetchstamp)) {
					type = Long.class;
				} else {
					type = controller.getResolver().getFieldType(fieldName, eventName);
				}
			}
			System.out.println("Registering event: " + eventName);
			controller.getEventProcessor().getAdministrator().getConfiguration().addEventType(eventName, types);
		}
	}

	private void prepareStatements() throws SQLException {
		showTables = conn.prepareStatement("show tables");
		descTable = conn.prepareStatement("describe ?");
		select = conn.prepareStatement("select * from `?`");
	}

	public static final String getDbPath(Settings settings) {
		StringBuilder dbUrl = new StringBuilder("jdbc:");
		dbUrl.append(settings.getProperty(KEY_DB_TYPE)).append("://").append(settings.getProperty(KEY_DB_HOST)).append("/");
		dbUrl.append(settings.getProperty(KEY_DB_NAME)).append("?user=").append(settings.getProperty(KEY_DB_USER));
		dbUrl.append("&password=").append(settings.getProperty(KEY_DB_PASS));
		return dbUrl.toString();
	}

	private static final Runnable dbjob = new Runnable() {

		@Override
		public void run() {
			while (true) {
				System.out.println("yupa!");
				try {
					Thread.sleep(4000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	};
}
