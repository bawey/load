package ch.cern.cms.load.taps.flashlist;

import ch.cern.cms.load.EventsTap;
import ch.cern.cms.load.Load;

/**
 * Nobody knows if anybody really needs it
 */

public abstract class AbstractFlashlistEventsTap extends EventsTap {
	public static final String PKEY_FIELD_TYPE = "PROPERTY_KEY_FIELD_NAME_TO_TYPE_MAP";
	@Deprecated
	public static final String KEY_FLASHLISTS_BLACKLIST = "flashBlackList";
	public static final String KEY_FLASHLISTS = "flashlists";

	protected AbstractFlashlistEventsTap(Load expert, String path) {
		super(expert, path);
	}

	/**
	 * Discards the prefixes used when listing the flashlists online
	 */
	protected String extractFlashlistEventName(String str) {
		return str.substring(str.lastIndexOf(':') + 1);
	}

}
