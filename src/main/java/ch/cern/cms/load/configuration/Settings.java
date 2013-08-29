package ch.cern.cms.load.configuration;

public class Settings {
	private static Settings instance = null;

	public static Settings getInstance() {
		if (instance == null) {
			synchronized (Settings.class) {
				if (instance == null) {
					instance = new Settings();
				}
			}
		}
		return instance;
	}

	private Settings() {
	}

	/**
	 * STATIC CODE DONE, INSTANCE CODE BELOW
	 */

	private String[] IDS = { "http://pcbawejdesktop.cern.ch:10000/urn:rcms-fm:fullpath=/bawey/test/testLEVELZERO,group=levelZeroFM,owner=bawey" };
	private String parameterEndpoint = "http://pcbawejdesktop.cern.ch:10000/rcms/services/ParameterController";
	private String notificationEndpoint = "http://pcbawejdesktop.cern.ch:10000/rcms/services/NotificationService?wsdl";

	private String idForNotification = "http://pcbawejdesktop.cern.ch:10000/urn:rcms-fm:fullpath=/bawey/test/testLEVELZERO,group=levelZeroFM,owner=bawey";

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
}
