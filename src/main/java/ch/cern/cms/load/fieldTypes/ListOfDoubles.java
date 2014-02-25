package ch.cern.cms.load.fieldTypes;


public class ListOfDoubles extends AbstractListOfData<Double> {
	private static final long serialVersionUID = 644447255108729026L;

	public ListOfDoubles(String dump) {
		super();
		for (String token : super.tokenizeFlashlistInput(dump)) {
			this.add(Double.parseDouble(token));
		}
	}
}
