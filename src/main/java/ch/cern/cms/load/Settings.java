package ch.cern.cms.load;

import java.io.File;
import java.util.Properties;

public class Settings extends Properties {

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

	protected Settings() {
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

}