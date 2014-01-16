package ch.cern.cms.load.taps.flashlist;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;

import ch.cern.cms.esper.Trx;
import ch.cern.cms.load.Load;
import ch.cern.cms.load.Settings;
import ch.cern.cms.load.annotations.TimeSource;

import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.time.CurrentTimeSpanEvent;

@TimeSource
public class DataBaseFlashlistEventsTap extends AbstractFlashlistEventsTap {

	private class Columns extends HashMap<String, String[]> {
		private static final long serialVersionUID = 1L;
	}

	private class Selects extends HashMap<String, PreparedStatement> {
		private static final long serialVersionUID = 1L;
	}

	private final static Logger logger = Logger.getLogger(DataBaseFlashlistEventsTap.class);

	public static final String KEY_DB_MODE = "flashlistDbMode";
	public static final String KEY_DB_TYPE = "flashlistDbType";
	public static final String KEY_DB_HOST = "flashlistDbHost";
	public static final String KEY_DB_NAME = "flashlistDbName";
	public static final String KEY_DB_USER = "flashlistDbUser";
	public static final String KEY_DB_PASS = "flashlistDbPass";
	public static final String KEY_RETRIEVAL_TIMESTAMP_NAME = "retrievalTimestampName";
	public static final boolean USE_MAPS = false;

	private Connection conn;
	private Statement statement;
	private PreparedStatement showTables;
	private PreparedStatement descTable;
	private Selects selects;

	private Columns definitions;

	private String fetchstampName;
	private Long timespanStart;
	private Long timespanEnd;

	private Collection<String> flashlistNames;
	private Map<String, Set<String>> blacklists;

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
		definitions = new Columns();
		fetchstampName = this.controller.getSettings().getProperty(KEY_RETRIEVAL_TIMESTAMP_NAME, "fetchstamp");
		timespanStart = this.controller.getSettings().getLong(Settings.KEY_TIMER_START, 0l);
		timespanEnd = this.controller.getSettings().getLong(Settings.KEY_TIMER_END, System.currentTimeMillis());
		flashlistNames = this.controller.getSettings().getSemicolonSeparatedValues(AbstractFlashlistEventsTap.KEY_FLASHLISTS);
		blacklists = new HashMap<String, Set<String>>();

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

			while (rs.next()) {
				String tableName = rs.getString(1);

				blacklists.put(tableName, expert.getSettings().getBlacklistedFields(tableName));

				if (flashlistNames.contains(tableName)) {
					ResultSet tableDescription = conn.createStatement().executeQuery("describe " + tableName);

					List<String> columnNamesList = new LinkedList<String>();
					while (tableDescription.next()) {
						String fieldName = tableDescription.getString(1);
						if (blacklists.get(tableName) == null || !blacklists.get(tableName).contains(fieldName)) {
							columnNamesList.add(fieldName);
						}
					}
					definitions.put(tableName, columnNamesList.toArray(new String[columnNamesList.size()]));
				}
			}
			System.out.println("Gathered definitions: " + definitions.keySet());

		} catch (Exception e) {
			throw new RuntimeException("Initial setup failed", e);
		}
	}

	@Override
	public void registerEventTypes(Load expert) {
		for (String eventName : definitions.keySet()) {
			if (USE_MAPS) {
				Map<String, Object> types = new HashMap<String, Object>();
				for (String fieldName : definitions.get(eventName)) {
					Class<?> type = String.class;
					if (fieldName.equals(fetchstampName)) {
						type = Long.class;
					} else {
						type = controller.getResolver().getFieldType(fieldName, eventName);
					}
					types.put(fieldName, type);
					logger.debug("event: " + eventName + ", field: " + fieldName);
				}
				controller.getEventProcessor().getAdministrator().getConfiguration().addEventType(eventName, types);
			} else {
				// as Object[]
				Object[] fieldTypes = new Object[definitions.get(eventName).length];
				fieldTypes[0] = Long.class;
				for (int i = 1; i < fieldTypes.length; ++i) {
					fieldTypes[i] = controller.getResolver().getFieldType(definitions.get(eventName)[i], eventName);
				}
				controller.getEventProcessor().getAdministrator().getConfiguration().addEventType(eventName, definitions.get(eventName), fieldTypes);
			}
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
		if (settings.containsKey(KEY_DB_PASS)) {
			dbUrl.append("&password=").append(settings.getProperty(KEY_DB_PASS));
		}
		return dbUrl.toString();
	}

	private final Runnable dbjob = new Runnable() {

		final class EventEnvelope {
			public final Object event;
			public final String name;

			public EventEnvelope(String name, Object event) {
				this.event = event;
				this.name = name;
			}

			public Map<?, ?> map() {
				return (Map<?, ?>) event;
			}

			public Object[] array() {
				return (Object[]) event;
			}
		}

		private BlockingQueue<EventEnvelope> queue = new ArrayBlockingQueue<EventEnvelope>(1000);

		private Runnable heraldRunnable = new Runnable() {
			@Override
			public void run() {
				EPRuntime rt = controller.getEventProcessor().getRuntime();
				// ArrayList<Long> deliveryTimes = new ArrayList<Long>(1000);
				while (true) {
					try {
						EventEnvelope ee = queue.take();
						long start = System.currentTimeMillis();
						if (ee.name != null) {
							if (USE_MAPS) {
								rt.sendEvent(ee.map(), ee.name);
							} else {
								rt.sendEvent(ee.array(), ee.name);
							}
						} else {
							rt.sendEvent(ee.event);
						}
						// deliveryTimes.add(System.currentTimeMillis() - start);
					} catch (InterruptedException e) {
						logger.warn("Event herald interrupted ", e);
					}
					// if (deliveryTimes.size() == 1000) {
					// logger.info(Stats.summarize("delivery times", deliveryTimes));
					// deliveryTimes.clear();
					// }
				}
			}
		};

		@Override
		public void run() {
			try {
				new Thread(heraldRunnable).start();

				System.out.println("Running the thread");
				selects = new Selects();
				for (String table : definitions.keySet()) {
					selects.put(table, conn.prepareStatement(new StringBuilder("select * from ").append(table).append(" where fetchstamp=?").toString()));
				}

				ResultSet times = conn.createStatement().executeQuery(
						"select fetchstamp from fetchstamps where fetchstamp between " + timespanStart + " and " + timespanEnd + " order by fetchstamp asc");

				// ArrayList<Long> dataPushTimes = new ArrayList<Long>(100);
				// ArrayList<Long> timePushTimes = new ArrayList<Long>(100);
				// ArrayList<Long> dataPullTimes = new ArrayList<Long>(1000);

				int timestampsCounter = 0;

				while (times.next()) {
					long start = System.currentTimeMillis();
					long time = times.getLong(1);
					queue.put(new EventEnvelope(null, new CurrentTimeSpanEvent(time)));
					// timePushTimes.add(System.currentTimeMillis() - start);
					for (String eventName : definitions.keySet()) {
						selects.get(eventName).setLong(1, time);
						start = System.currentTimeMillis();
						ResultSet fetched = selects.get(eventName).executeQuery();
						// dataPullTimes.add(System.currentTimeMillis() - start);
						start = System.currentTimeMillis();
						String[] columnsArray = definitions.get(eventName);
						Set<String> blacklist = blacklists.get(eventName);
						while (fetched.next()) {
							if (USE_MAPS) {
								Map<String, Object> event = new HashMap<String, Object>();
								event.put(columnsArray[0], fetched.getLong(1));
								for (int i = 1; i < fetched.getMetaData().getColumnCount(); ++i) {
									String columnName = fetched.getMetaData().getColumnName(i+1);
									if (blacklist == null || !blacklist.contains(columnName)) {
										event.put(columnName, controller.getResolver().convert(fetched.getString(i + 1), columnName, eventName));
									}
								}
								queue.put(new EventEnvelope(eventName, event));
							} else {
								// as Object[]?
								Object[] event = new Object[columnsArray.length];
								event[0] = fetched.getLong(1);
								int valids = 1;
								for (int i = 1; i < fetched.getMetaData().getColumnCount(); ++i) {
									String columnName = fetched.getMetaData().getColumnName(i+1);
									if (blacklist == null || !blacklist.contains(columnName)) {
										event[valids++] = controller.getResolver().convert(fetched.getString(i + 1), columnName, eventName);
									}
								}
								queue.put(new EventEnvelope(eventName, event));
							}
						}
						// dataPushTimes.add(System.currentTimeMillis() - start);
					}
					// if (timePushTimes.size() == 10) {
					// logger.info(Stats.summarize("Data pushing: ", dataPushTimes));
					// logger.info(Stats.summarize("Data pulling: ", dataPullTimes));
					// logger.info(Stats.summarize("Time pushing: ", timePushTimes));
					// dataPullTimes.clear();
					// dataPushTimes.clear();
					// timePushTimes.clear();
					// }
					if ((++timestampsCounter) % 10000 == 0) {
						System.out.println("Processed " + timestampsCounter + " timestamps. Last fetchstamp: " + Trx.toDate(time) + ", current local time: "
								+ new Date().toString());
					}
				}
			} catch (Exception e1) {
				throw new RuntimeException(e1);
			}
		}
	};
}
