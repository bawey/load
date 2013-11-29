package ch.cern.cms.load.taps.flashlist;

import java.io.File;
import java.io.FileFilter;
import java.util.LinkedList;
import java.util.List;

import ch.cern.cms.load.Load;

public class OfflineFlashlistEventsTap2 extends AbstractFlashlistEventsTap {

	private Load app = null;
	private List<File> rootDirs = null;

	public OfflineFlashlistEventsTap2(Load application) {
		super(application, null);
		app = application;
	}

	@Override
	public void preRegistrationSetup(Load app) {
		assignRunnable();
		rootDirs = new LinkedList<File>();
		for (String s : app.getSettings().getMany(OfflineFlashlistEventsTap.SETTINGS_KEY_FLASHLIST_DIR)) {
			rootDirs.add(new File(s));
		}

		for (File dir : rootDirs) {
			for (File subdir : dir.listFiles(FF_DIRECTORY)) {
				String eventName = dirToEventName(subdir.getName());
				System.out.println(eventName);
			}
		}

	}

	@Override
	public void registerEventTypes(Load app) {
		// TODO Auto-generated method stub

	}

	private void assignRunnable() {
		this.job = new Runnable() {
			@Override
			public void run() {

			}
		};
	}

	private String dirToEventName(String name) {
		if (name.contains(":")) {
			return name.substring(name.lastIndexOf(":") + 1);
		}
		return name;
	}

	public static final FileFilter FF_DIRECTORY = new FileFilter() {
		@Override
		public boolean accept(File pathname) {
			return pathname.isDirectory();
		}
	};
}
