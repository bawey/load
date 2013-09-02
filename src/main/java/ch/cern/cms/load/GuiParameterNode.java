package ch.cern.cms.load;

import java.util.HashMap;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;

public class GuiParameterNode extends DefaultMutableTreeNode {
	private static final long serialVersionUID = 1L;

	public final Map<String, Object> stuff = new HashMap<String, Object>();

	private Object modelKey;


	public GuiParameterNode(Object modelKey) {
		super(modelKey);
		this.modelKey = modelKey;
	}

	public Object getModelKey() {
		return this.modelKey;
	}

}
