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

import com.espertech.esper.client.time.CurrentTimeSpanEvent;

import ch.cern.cms.load.Load;
import ch.cern.cms.load.Settings;
import ch.cern.cms.load.annotations.TimeSource;

@TimeSource
public class DataBaseFlashlistEventsTap extends AbstractFlashlistEventsTap {

	private class Columns extends HashMap<String, String[]> {
		private static final long serialVersionUID = 1L;
	}

	private class Selects extends HashMap<String, PreparedStatement> {
		private static final long serialVersionUID = 1L;
	}

	public static final String KEY_DB_MODE = "flashlistDbMode";
	public static final String KEY_DB_TYPE = "flashlistDbType";
	public static final String KEY_DB_HOST = "flashlistDbHost";
	public static final String KEY_DB_NAME = "flashlistDbName";
	public static final String KEY_DB_USER = "flashlistDbUser";
	public static final String KEY_DB_PASS = "flashlistDbPass";
	public static final String KEY_RETRIEVAL_TIMESTAMP_NAME = "retrievalTimestampName";

	private Connection conn;
	private Statement statement;
	private PreparedStatement showTables;
	private PreparedStatement descTable;
	private Selects selects;
	private String fetchstamp;

	private Columns columns;

	private String fetchstampName;
	private Long timespanStart;
	private Long timespanEnd;

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
	}

	public static final String getDbPath(Settings settings) {
		StringBuilder dbUrl = new StringBuilder("jdbc:");
		dbUrl.append(settings.getProperty(KEY_DB_TYPE)).append("://").append(settings.getProperty(KEY_DB_HOST)).append("/");
		dbUrl.append(settings.getProperty(KEY_DB_NAME)).append("?user=").append(settings.getProperty(KEY_DB_USER));
		dbUrl.append("&password=").append(settings.getProperty(KEY_DB_PASS));
		return dbUrl.toString();
	}

	private final Runnable dbjob = new Runnable() {

		@Override
		public void run() {
			try {
				System.out.println("Running the thread");
				selects = new Selects();
				for (String table : columns.keySet()) {
					selects.put(
							table,
							conn.prepareStatement(new StringBuilder("select * from ").append(table).append(" where fetchstamp=?")
									.toString()));
				}

				ResultSet times = conn.createStatement().executeQuery(
						"select fetchstamp from fetchstamps where fetchstamp between " + timespanStart + " and " + timespanEnd);

				CurrentTimeSpanEvent timeEvent = new CurrentTimeSpanEvent(0);
				while (times.next()) {
					long time = times.getLong(1);
					timeEvent.setTargetTimeInMillis(time);
					controller.getEventProcessor().getRuntime().sendEvent(timeEvent);
					for (String table : columns.keySet()) {
						selects.get(table).setLong(1, time);
						ResultSet events = selects.get(table).executeQuery();
						while (events.next()) {
							Map<String, Object> event = new HashMap<String, Object>();
							for (String column : columns.get(table)) {
								if (column.equals(fetchstampName)) {
									event.put(column, events.getLong(column));
								}
								event.put(column, controller.getResolver().convert(events.getString(column), column, table));
							}
							controller.getEventProcessor().getRuntime().sendEvent(event, table);
						}
					}
				}
			} catch (Exception e1) {
				throw new RuntimeException(e1);
			}
		}
	};
}
