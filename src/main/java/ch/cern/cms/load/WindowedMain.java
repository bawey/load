package ch.cern.cms.load;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.io.File;
import java.util.Map;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;

import ch.cern.cms.load.configuration.Settings;
import ch.cern.cms.load.configuration.Settings.Runmode;
import ch.cern.cms.load.model.Model;
import ch.cern.cms.load.model.ModelListener;

public class WindowedMain implements ModelListener, TreeSelectionListener {

	private static WindowedMain instance = null;
	private JFrame frame = null;
	private Model model;
	private JScrollPane leftScrollPane = null;
	private Settings settings = Settings.getInstance();

	private JPanel rightPanel = new JPanel();
	private JScrollPane rightScrollPane = new JScrollPane(rightPanel);

	private JPanel rightTopPanel = new JPanel();
	private JScrollPane rightTopScrollPane = new JScrollPane(rightTopPanel);
	private JPanel rightBottomPanel = new JPanel();
	private JScrollPane rightBottomScrollPane = new JScrollPane(rightBottomPanel);

	private WindowedMain(JFrame frame, Model model) {
		super();
		this.frame = frame;
		this.model = model;
	}

	private static void switchToOfflineMode() {
		Settings.getInstance().setRunmode(Runmode.OFFLINE);
		Settings.getInstance().setPlaybackRate(10d);
		Settings.getInstance().setDataSource(new File("dmp/data.bt"));
	}

	public static final void main(String[] args) {
		switchToOfflineMode();
		Model model = Model.getInstance();
		JFrame frame = new JFrame("Parameters overview");
		instance = new WindowedMain(frame, model);
		model.registerListener(instance);
		instance.setRightPane();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(1024, 768);
		frame.setVisible(true);
	}

	@Override
	public void react(EventType changeType) {
		if (changeType.equals(EventType.DATA_SET_CHANGED)) {
			System.out.println("planting a tree");
			setTree();
		}

	}

	private void setRightPane() {
		JPanel column = new JPanel();
		column.setLayout(new BoxLayout(column, BoxLayout.PAGE_AXIS));
		rightTopPanel.setBackground(Color.BLACK);
		rightTopPanel.add(new JLabel("Abrakadabra"));
		rightPanel.setBackground(Color.GREEN);
		rightBottomPanel.setBackground(Color.WHITE);
		column.add(rightTopScrollPane);
		column.add(rightScrollPane);
		column.add(rightBottomScrollPane);
		frame.getContentPane().add(column, BorderLayout.LINE_END);
	}

	private void setTree() {
		boolean listNotYetThere = leftScrollPane == null;
		Container cnt = frame.getContentPane();

		DefaultMutableTreeNode top = new DefaultMutableTreeNode("Lv0 FM Parameters");
		createNodes(top, model.getData());
		JTree tree = new JTree(top);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.addTreeSelectionListener(this);

		Dimension size = new Dimension(frame.getWidth() / 3, frame.getHeight());

		tree.setMaximumSize(size);
		tree.setPreferredSize(size);
		tree.setSize(size);

		leftScrollPane = new JScrollPane(tree);
		leftScrollPane.setMaximumSize(size);
		leftScrollPane.setSize(size);
		cnt.add(leftScrollPane, BorderLayout.LINE_START);
		if (listNotYetThere) {
			cnt.validate();
		} else {

		}
	}

	private void expandSubtree(GuiParameterNode node, Object value) {
		node.stuff.put("START_VAL", value);

		if (value instanceof Map<?, ?>) {
			createNodes(node, (Map<?, ?>) value);
		} else if (value instanceof Vector<?>) {
			createNodes(node, (Vector<?>) value);
		}
		StringBuilder sb = new StringBuilder(node.getUserObject().toString());
		if (value == null) {
			sb.append(" [NULL]");
		} else {
			sb.append(" [").append(value.getClass().getSimpleName()).append("] ");
			String string = value.toString();
			if (string.length() > settings.getMaxValueLengthInTree()) {
				sb.append(string.substring(0, settings.getMaxValueLengthInTree())).append("...");
			} else {
				sb.append(string);
			}
		}
		node.setUserObject(sb.toString());
	}

	private void createNodes(DefaultMutableTreeNode node, Map<?, ?> map) {
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			GuiParameterNode child = new GuiParameterNode(entry.getKey().toString());
			node.add(child);
			expandSubtree(child, entry.getValue());
		}
	}

	private void createNodes(DefaultMutableTreeNode node, Vector<?> vector) {
		for (int i = 0; i < vector.size(); ++i) {
			GuiParameterNode child = new GuiParameterNode(i);
			node.add(child);
			expandSubtree(child, vector.get(i));
		}
	}

	/**
	 * Simple version: highlight in red if the parameter has just changed,
	 * revert to black otherwise
	 */
	private void highlightUpdated() {
		if (leftScrollPane == null) {
			return;
		}
	}

	@Override
	public void valueChanged(TreeSelectionEvent e) {
		Object[] path = e.getNewLeadSelectionPath().getPath();
		Object getFrom = model.getData();
		Object key = null;

		for (int i = 1; i < path.length && (key = ((GuiParameterNode) path[i++]).getModelKey()) != null;) {
			if (getFrom instanceof Map<?, ?>) {
				getFrom = ((Map<?, ?>) getFrom).get(key);
			} else if (getFrom instanceof Vector<?>) {
				getFrom = ((Vector<?>) getFrom).get((Integer) key);
			}
		}

		if (e.getNewLeadSelectionPath().getPath().length > 1) {
			GuiParameterNode node = (GuiParameterNode) e.getNewLeadSelectionPath().getLastPathComponent();
			String nodeString = (getFrom != null ? getFrom.toString() : "null");
			this.rightBottomPanel.removeAll();
			JTextArea txt = new JTextArea(nodeString);
			txt.setWrapStyleWord(true);
			txt.setLineWrap(true);
			this.rightBottomPanel.add(txt);
			System.out.println(nodeString);
			this.rightBottomPanel.repaint();
			this.rightBottomPanel.validate();
		}
	}
}
