package ch.cern.cms.load.eplProviders;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import ch.cern.cms.load.EplProvider;
import ch.cern.cms.load.EventProcessor;
import ch.cern.cms.load.Load;

public class FileBasedEplProvider implements EplProvider {

	private static final Logger logger = Logger.getLogger(FileBasedEplProvider.class);

	List<File> eplRoots = new LinkedList<File>();

	public FileBasedEplProvider() {
		for (String dir : Load.getInstance().getSettings().getMany("eplDir")) {
			File eplDir = new File(dir);
			if (eplDir.exists() && eplDir.isDirectory()) {
				eplRoots.add(eplDir);
			} else {
				logger.error("Directory " + dir + " specified in configuration file doesn't exist.");
			}
		}
	}

	@Override
	public void registerStatements(EventProcessor ep) {
		for (File dir : eplRoots) {
			importEplDir(dir);
		}
	}

	private void importEplDir(File dir) {
		logger.info("enetering " + dir.getAbsolutePath() + " EPL directory");
		for (File f : dir.listFiles(eplFileFilter)) {
			logger.info("found " + f.getName() + " EPL file");
			File dpnDir = new File(dir.getAbsolutePath() + "/" + f.getName().substring(0, f.getName().lastIndexOf('.')));
			if (dpnDir.exists() && dpnDir.isDirectory()) {
				importEplDir(dpnDir);
			}
			try {
				InputStream inputFile = new FileInputStream(f);
				Load.getInstance().getEventProcessor().getAdministrator().getDeploymentAdmin().readDeploy(inputFile, null, null, null);
			} catch (Exception e) {
				logger.error("Failed to read EPL file: " + f.getAbsolutePath(), e);
			}
		}
	}

	public final FileFilter eplFileFilter = new FileFilter() {
		@Override
		public boolean accept(File f) {
			int index = -1;
			if (!f.isDirectory() && (index = f.getName().lastIndexOf('.')) > 0) {
				if (f.getName().substring(index + 1).equalsIgnoreCase("epl")) {
					return true;
				}
			}
			return false;
		}
	};
}
