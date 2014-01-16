package ch.cern.cms.load;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

public class Settings extends Properties {

	public static final String PROPERTIES_FILENAME = "load.properties";

	public static final String KEY_SOCKS_PROXY_HOST = "socksProxyHost";
	public static final String KEY_SOCKS_PROXY_PORT = "socksProxyPort";
	public static final String KEY_SOCKS_PROXY_SET = "proxySet";

	public static final String KEY_TIMER = "timer";
	public static final String KEY_TIMER_START = "timerStart";
	public static final String KEY_TIMER_PACE = "timerPace";
	public static final String KEY_TIMER_STEP = "timerStep";
	public static final String KEY_TIMER_END = "timerEnd";
	public static final String KEY_TIMER_OFFSET = "timerOffset";

	public static final String PREFIX_BLACKLIST = "blacklist_";

	private static final Logger logger = Logger.getLogger(Settings.class);

	/**
	 * 
	 */
	private static final long serialVersionUID = 2461597629371497239L;

	public enum Runmode {
		ONLINE, // usual run with on-the-fly data processing
		OFFLINE, // processing previously recorded data
		ONLINE_RECORD; // system receives data but only saves it
	}

	public final static String KEY_RESOLVER = "SETTINGS_KEY_RESOLVER";

	/** reads the .properties file if found, sets defaults and creates the file otherwise **/
	protected Settings() {
		File props = new File(PROPERTIES_FILENAME);
		try {
			if (!props.exists()) {
				throw new FileNotFoundException(PROPERTIES_FILENAME);
			} else {
				FileReader fr = new FileReader(props);
				load(fr);
				fr.close();
			}
		} catch (Exception e1) {
			try {
				setDefaults();
				props.createNewFile();
				autosave();
				logger.warn("Could not open properties file. Setting the defaults and saving the file to: " + props.getAbsolutePath() + ".\nProblem details: ",
						e1);
			} catch (Exception e2) {
				logger.error("Could neither read nor write the properties! Using the defauls.");
			}
		}
	}

	public void setMany(String propertyName, Collection<String> values) {
		int counter = 0;
		for (String value : values) {
			super.setProperty(propertyName + "[" + (counter++) + "]", value);
		}
		safeAutosave();
	}

	public Collection<String> getSemicolonSeparatedValues(String propertyName) {
		String raw = getProperty(propertyName);
		if (raw != null) {
			return Arrays.asList(raw.split(";"));
		}
		return new LinkedList<String>();
	}

	public Collection<String> getMany(String propertyName) {
		Collection<String> values = new LinkedList<String>();
		int counter = 0;
		String value = getProperty(propertyName + "[0]");
		while (value != null) {
			values.add(value);
			value = getProperty(propertyName + "[" + (++counter) + "]");
		}
		if (getProperty(propertyName) != null) {
			values.add(getProperty(propertyName));
		}
		return values;
	}

	@Override
	public synchronized Object setProperty(String key, String value) {
		Object result = super.setProperty(key, value);
		safeAutosave();
		return result;
	}

	protected void autosave() throws IOException {
		FileWriter fw = new FileWriter(new File(PROPERTIES_FILENAME));
		store(fw, "autosaved properties");
		fw.close();
	}

	private void safeAutosave() {
		try {
			autosave();
		} catch (IOException e) {
			logger.warn("Could not auto save the properties file.", e);
		}
	}

	private void setDefaults() {
		setProperty(KEY_SOCKS_PROXY_HOST, "127.0.0.1");
		setProperty(KEY_SOCKS_PROXY_PORT, "1080");
		setProperty(KEY_SOCKS_PROXY_SET, "true");
	}

	/**
	 * STATIC CODE DONE, INSTANCE CODE BELOW
	 */

	private Runmode runmode = Runmode.ONLINE;
	private String[] IDS = { "http://pcbawejdesktop.cern.ch:10000/urn:rcms-fm:fullpath=/bawey/test/testLEVELZERO,group=levelZeroFM,owner=bawey" };
	private String parameterEndpoint = "http://pcbawejdesktop.cern.ch:10000/rcms/services/ParameterController";
	private String notificationEndpoint = "http://pcbawejdesktop.cern.ch:10000/rcms/services/NotificationService?wsdl";
	private String idForNotification = "http://pcbawejdesktop.cern.ch:10000/urn:rcms-fm:fullpath=/bawey/test/testLEVELZERO,group=levelZeroFM,owner=bawey";
	private int maxValueLengthInTree = 20;
	public final String flashlistDumpName = "flashlistDump_2013-09-04-11-50-01.ascii";

	// negative value indicates no playback. positive one speeds up (>1) or
	// slows down the rate of data delivery
	private double playbackRate = -1d;
	private File dataSource = null;

	public String[] getIDS() {
		return IDS;
	}

	public void setIDS(String[] iDS) {
		IDS = iDS;
	}

	public String getParametersEndpoint() {
		return parameterEndpoint;
	}

	public void setParametersEndpoint(String endpoint) {
		this.parameterEndpoint = endpoint;
	}

	public String getIdForNotificationsRetrieval() {
		return idForNotification;
	}

	public String getNotificationEndpoint() {
		return notificationEndpoint;
	}

	public void setNotificationEndpoint(String notificationEndpoint) {
		this.notificationEndpoint = notificationEndpoint;
	}

	public Runmode getRunmode() {
		return runmode;
	}

	public void setRunmode(Runmode runmode) {
		this.runmode = runmode;
	}

	public double getPlaybackRate() {
		return playbackRate;
	}

	public void setPlaybackRate(double playbackRate) {
		this.playbackRate = playbackRate;
	}

	public File getDataSource() {
		return dataSource;
	}

	public void setDataSource(File dataSource) {
		this.dataSource = dataSource;
	}

	public int getMaxValueLengthInTree() {
		return maxValueLengthInTree;
	}

	public void setMaxValueLengthInTree(int maxValueLengthInTree) {
		this.maxValueLengthInTree = maxValueLengthInTree;
	}

	public long getLong(String key, long defaultValue) {
		if (containsKey(key)) {
			return Long.parseLong(this.getProperty(key));
		} else {
			return defaultValue;
		}
	}

	public Set<String> getBlacklistedFields(String flashlistName) {
		if (this.containsKey(PREFIX_BLACKLIST + flashlistName)) {
			String[] tokens = this.getProperty(PREFIX_BLACKLIST + flashlistName).split(";");
			Set<String> set = new HashSet<String>();
			for (String s : tokens) {
				set.add(s);
			}
			return set;
		}
		return null;
	}
}
