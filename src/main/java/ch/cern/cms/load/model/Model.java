package ch.cern.cms.load.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author Tomasz Bawej let's put it all here and later see how to improve on
 *         that
 * 
 *         initially subscribe with LZNR to pick all the updates and put them to
 *         the map right-away also try to obtain the full parameterSet via
 *         PartameterService. Obtained data should be merged with what could
 *         have been sent in the meantime by LZNR, but not overwrite it (it
 *         takes usually quite long to pull the whole parameter set and god
 *         knows what can be dropped in the meantime
 * 
 */
public class Model {
	private Map<String, String> data = new HashMap<String, String>();

}
