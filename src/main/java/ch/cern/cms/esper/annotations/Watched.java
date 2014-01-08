package ch.cern.cms.esper.annotations;

/**
 * 
 * @author Tomasz Bawej
 * 
 *         Annotation to be used with EPL statements returning a value to be monitored within views offering such possibility.
 */
public @interface Watched {
	/**
	 * Optional label to print the value with. Overrides the label from statement result.
	 */
	String label() default "";
	
	/**
	 * Indicates the name of a column containing label value 
	 */
	String labelName() default "";
	
	
}
