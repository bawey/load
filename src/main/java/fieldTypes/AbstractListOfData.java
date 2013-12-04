package fieldTypes;

import java.util.LinkedList;

public abstract class AbstractListOfData<T> extends LinkedList<T> {

	private static final long serialVersionUID = 1L;

	public static final String[] tokenizeFlashlistInput(String input) {
		input = input.trim();
		input = input.substring(input.startsWith("[") ? 1 : 0, input.endsWith("]") ? input.length() - 1 : input.length());
		if (input.length() == 0) {
			return new String[] {};
		} else {
			return input.split(",");
		}
	}
}
