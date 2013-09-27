package ch.cern.cms.load.guis;

import java.awt.Dimension;

import javax.swing.JFrame;

import ch.cern.cms.load.ExpertController;

public class DefaultGui extends JFrame implements ExpertGui {
	private static final long serialVersionUID = 1L;
	private ExpertController expert;

	public DefaultGui() {
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setTitle("load");
		this.setSize(new Dimension(800, 600));
	}

	public ExpertGui attach(ExpertController expert) {
		this.expert = expert;
		this.setVisible(true);
		return this;
	}
}
