package ch.cern.cms.load.taps.flashlist;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

	private Connection conn;
	private Statement statement;
	private PreparedStatement showTables;
	private PreparedStatement descTable;
	private Selects selects;

	private Columns definitions;

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
		definitions = new Columns();
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

			while (rs.next()) {
				String tableName = rs.getString(1);
				ResultSet details = conn.createStatement().executeQuery("describe " + tableName);

				List<String> headers = new LinkedList<String>();
				while (details.next()) {
					headers.add(details.getString(1));
				}
				definitions.put(tableName, headers.toArray(new String[headers.size()]));
			}

			if (controller.getSettings().containsKey(AbstractFlashlistEventsTap.KEY_FLASHLISTS_BLACKLIST)) {
				for (String blacklisted : this.controller.getSettings().getProperty(AbstractFlashlistEventsTap.KEY_FLASHLISTS_BLACKLIST).split(";")) {
					if (definitions.keySet().contains(blacklisted)) {
						definitions.remove(blacklisted);
					}
				}
			}

		} catch (Exception e) {
			throw new RuntimeException("Initial setup failed", e);
		}
	}

	@Override
	public void registerEventTypes(Load expert) {
		for (String eventName : definitions.keySet()) {
			System.out.println("Registering event: " + eventName);

			// as Object[]
			// Object[] types = new Object[columns.get(eventName).length];
			// types[0] = Long.class;
			// for (int i = 1; i < types.length; ++i) {
			// types[i] = controller.getResolver().getFieldType(eventName, columns.get(eventName)[i]);
			// }
			// controller.getEventProcessor().getAdministrator().getConfiguration().addEventType(eventName, columns.get(eventName), types);

			Map<String, Object> types = new HashMap<String, Object>();
			for (String fieldName : definitions.get(eventName)) {
				Class<?> type = String.class;
				if (fieldName.equals(fetchstampName)) {
					type = Long.class;
				} else {
					type = controller.getResolver().getFieldType(fieldName, eventName);
				}
				types.put(fieldName, type);
			}
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
							rt.sendEvent(ee.map(), ee.name);
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
						"select fetchstamp from fetchstamps where fetchstamp between " + timespanStart + " and " + timespanEnd);

				// ArrayList<Long> dataPushTimes = new ArrayList<Long>(100);
				// ArrayList<Long> timePushTimes = new ArrayList<Long>(100);
				// ArrayList<Long> dataPullTimes = new ArrayList<Long>(1000);

				int timestampsCounter = 0;

				while (times.next()) {
					long start = System.currentTimeMillis();
					long time = times.getLong(1);
					queue.put(new EventEnvelope(null, new CurrentTimeSpanEvent(time)));
					// timePushTimes.add(System.currentTimeMillis() - start);
					for (String table : definitions.keySet()) {
						selects.get(table).setLong(1, time);
						start = System.currentTimeMillis();
						ResultSet fetched = selects.get(table).executeQuery();
						// dataPullTimes.add(System.currentTimeMillis() - start);
						start = System.currentTimeMillis();
						String[] columnsArray = definitions.get(table);
						while (fetched.next()) {

							// as Object[]?
							// Object[] event = new Object[columnsArray.length];
							// event[0] = fetched.getLong(1);
							// for (int i = 1; i < event.length; ++i) {
							// event[i] = controller.getResolver().convert(fetched.getString(i + 1), columnsArray[i], table);
							// }

							Map<String, Object> event = new HashMap<String, Object>();
							event.put(columnsArray[0], fetched.getLong(1));
							for (int i = 1; i < columnsArray.length; ++i) {
								event.put(columnsArray[i], controller.getResolver().convert(fetched.getString(i + 1), columnsArray[i], table));
							}
							queue.put(new EventEnvelope(table, event));
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
						System.out.println("Processed " + timestampsCounter + " timestamps. Last: " + Trx.toDate(time) + ", now: " + new Date().toString());
					}
				}
			} catch (Exception e1) {
				throw new RuntimeException(e1);
			}
		}
	};
}
