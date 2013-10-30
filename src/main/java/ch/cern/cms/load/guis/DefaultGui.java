package ch.cern.cms.load.guis;

import java.awt.Dimension;

import javax.swing.JFrame;

import ch.cern.cms.load.Load;

public class DefaultGui extends JFrame implements ExpertGui {
	private static final long serialVersionUID = 1L;
	private Load expert;

	public DefaultGui() {
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setTitle("load");
		this.setSize(new Dimension(800, 600));
	}

	public ExpertGui attach(Load expert) {
		this.expert = expert;
		this.setVisible(true);
		return this;
	}
}
