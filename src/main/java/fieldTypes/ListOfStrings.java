package fieldTypes;


public class ListOfStrings extends AbstractListOfData<String> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ListOfStrings(String input) {
		super();
		for (String token : super.tokenizeFlashlistInput(input)) {
			this.add(token);
		}
	}
}
