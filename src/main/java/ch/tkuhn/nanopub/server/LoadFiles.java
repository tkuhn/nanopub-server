package ch.tkuhn.nanopub.server;

import java.io.File;

import org.nanopub.MultiNanopubRdfHandler;
import org.nanopub.MultiNanopubRdfHandler.NanopubHandler;
import org.nanopub.Nanopub;
import org.openrdf.rio.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoadFiles implements Runnable {

	private static LoadFiles running;

	private static NanopubDb db = NanopubDb.get();

	public static synchronized void check() {
		if (running != null) return;
		if (ServerConf.get().getLoadDir() == null) return;
		try {
			running = new LoadFiles();
			new Thread(running).start();
		} catch (Exception ex) {
			LoggerFactory.getLogger(LoadFiles.class).error(ex.getMessage(), ex);
		}
	}

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	private final File loadDir, processingDir, doneDir;

	private LoadFiles() {
		loadDir = new File(ServerConf.get().getLoadDir());
		processingDir = new File(loadDir, "processing");
		if (!processingDir.exists()) processingDir.mkdir();
		doneDir = new File(loadDir, "done");
		if (!doneDir.exists()) doneDir.mkdir();
	}

	@Override
	public void run() {
		logger.info("Start file loading thread");
		try {
			try {
				int ms = 30000;
				logger.info("Wait " + ms + "ms...");
				Thread.sleep(ms);
			} catch(InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			checkFilesToLoad();
		} finally {
			running = null;
		}
	}

	private void checkFilesToLoad() {
		logger.info("Check whether there are files to load...");
		for (File f : loadDir.listFiles()) {
			if (f.isDirectory()) continue;
			logger.info("Try to load file: " + f);
			try {
				final File processingFile = new File(processingDir, f.getName());
				f.renameTo(processingFile);
				RDFFormat format = RDFFormat.forFileName(processingFile.getName());
				MultiNanopubRdfHandler.process(format, processingFile, new NanopubHandler() {
					@Override
					public void handleNanopub(Nanopub np) {
						try {
							db.loadNanopub(np);
							logger.info("File loaded: " + processingFile);
						} catch (Exception ex) {
							throw new RuntimeException(ex);
						}
					}
				});
				processingFile.renameTo(new File(doneDir, f.getName()));
			} catch (Exception ex) {
				logger.error("Failed to load file: " + f, ex);
			}
		}
	}

}
