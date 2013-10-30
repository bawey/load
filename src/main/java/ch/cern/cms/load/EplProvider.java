package ch.cern.cms.load;

/**
 * Wraps up the storage of rules between launches and exposes some related functions
 * 
 */
public interface EplProvider {

	public void registerStatements(EventProcessor ep);

}
