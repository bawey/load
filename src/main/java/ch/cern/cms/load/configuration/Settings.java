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
	private String endpoint = "http://pcbawejdesktop.cern.ch:10000/rcms/services/ParameterController";

	/**
	 * stuff for notification collection
	 * http://pcbawejdesktop.cern.ch:10000/urn:
	 * rcms-fm:fullpath=/bawey/test/testLEVELZERO,group=levelZeroFM,owner=bawey
	 */

	public String[] getIDS() {
		return IDS;
	}

	public void setIDS(String[] iDS) {
		IDS = iDS;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public String getIdForNotificationsRetrieval() {
		return IDS[0];
	}

}
