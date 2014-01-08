package ch.cern.cms.load.eplProviders;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import ch.cern.cms.load.EplProvider;
import ch.cern.cms.load.EventProcessor;
import ch.cern.cms.load.Load;

public class FileBasedEplProvider implements EplProvider {

	private static final Logger logger = Logger.getLogger(FileBasedEplProvider.class);
	private static Set<String> imported = new HashSet<String>();

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
		imported.clear();
		for (File dir : eplRoots) {
			importEplDir(dir);
		}
	}

	private void importEplDir(File dir) {
		logger.info("enetering " + dir.getAbsolutePath() + " EPL directory");
		for (File f : dir.listFiles(eplFileFilter)) {
			logger.info("found " + f.getName() + " EPL file");

			/** detecting a dependency subdirectory (same name as EPL file, yet no trailing '.epl' extension). Abandoned and discouraged **/
			File dpnDir = new File(dir.getAbsolutePath() + "/" + f.getName().substring(0, f.getName().lastIndexOf('.')));
			if (dpnDir.exists() && dpnDir.isDirectory()) {
				importEplDir(dpnDir);
			}
			importEplFile(f);
		}
	}

	private void importEplFile(File f) {
		logger.debug("imported count: " + imported.size());
		if (imported.contains(f.getAbsolutePath())) {
			logger.debug("File " + f.getAbsolutePath() + " already imported.");
			return;
		} else {
			logger.debug("Marking file " + f.getAbsolutePath() + " as imported and importing EPL.");
			imported.add(f.getAbsolutePath());
		}
		try {
			BufferedReader br = new BufferedReader(new FileReader(f));
			StringBuffer content = new StringBuffer();
			List<String> includes = new LinkedList<String>();
			String line = null;
			for (String ln = br.readLine(); ln != null; ln = br.readLine()) {
				if (ln.trim().startsWith("#include")) {
					includes.add(ln);
				} else {
					content.append(ln).append("\n");
				}
			}
			for (String include : includes) {
				String filePath = include.substring(include.indexOf('"') + 1, include.lastIndexOf('"'));
				File dependency = new File(f.getParentFile().getAbsolutePath() + "/" + filePath);
				if (dependency.exists()) {
					importEplFile(dependency);
				} else {
					logger.warn("EPL file " + dependency.getAbsolutePath() + " marked for import does not exist");
				}
			}
			InputStream inputStream = new ByteArrayInputStream(content.toString().getBytes("UTF-8"));
			Load.getInstance().getEventProcessor().getAdministrator().getDeploymentAdmin().readDeploy(inputStream, null, null, null);
			logger.info("successfully deployed a module from  " + f.getPath());
		} catch (Exception e) {
			logger.error("Failed to read EPL file: " + f.getAbsolutePath(), e);
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
