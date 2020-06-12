package ch.tkuhn.nanopub.server;

import java.io.File;

import org.nanopub.MultiNanopubRdfHandler;
import org.nanopub.MultiNanopubRdfHandler.NanopubHandler;
import org.nanopub.extra.server.NanopubSurfacePattern;
import org.nanopub.Nanopub;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoadFiles implements Runnable {

	private static LoadFiles running;
	private static Thread thread;

	private static NanopubDb db = NanopubDb.get();
	private static NanopubSurfacePattern ourPattern = ServerConf.getInfo().getNanopubSurfacePattern();

	public static synchronized void check() {
		if (running != null) {
//			if (running.aliveAtTime + 60 * 60 * 1000 < System.currentTimeMillis()) {
//				running.logger.info("No sign of life of the daemon for 60 minutes... (the starting of a new one is disabled)");
//				running.aliveAtTime = System.currentTimeMillis();
//				return;
			if (running.aliveAtTime + 7 * 24 * 60 * 60 * 1000 < System.currentTimeMillis()) {
				running.logger.info("No sign of life of the daemon for 7 days. Starting a new one...");
				running.aliveAtTime = System.currentTimeMillis();
				running = null;
				thread.interrupt();
			} else {
				return;
			}
		}
		if (ServerConf.get().getLoadDir() == null) return;
		try {
			running = new LoadFiles();
			thread = new Thread(running);
			thread.setDaemon(true);
			thread.start();
		} catch (Exception ex) {
			LoggerFactory.getLogger(LoadFiles.class).error(ex.getMessage(), ex);
		}
	}

	private Logger logger = LoggerFactory.getLogger(this.getClass());
	private long aliveAtTime;

	private final File loadDir, processingDir, doneDir;

	private LoadFiles() {
		stillAlive();
		loadDir = new File(ServerConf.get().getLoadDir());
		processingDir = new File(loadDir, "processing");
		if (!processingDir.exists()) processingDir.mkdir();
		doneDir = new File(loadDir, "done");
		if (!doneDir.exists()) doneDir.mkdir();
	}

	@Override
	public void run() {
		stillAlive();
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
			stillAlive();
			if (f.isDirectory()) continue;
			logger.info("Try to load file: " + f);
			try {
				final File processingFile = new File(processingDir, f.getName());
				f.renameTo(processingFile);
				RDFFormat format = Rio.getParserFormatForFileName(processingFile.getName()).orElse(null);
				MultiNanopubRdfHandler.process(format, processingFile, new NanopubHandler() {
					@Override
					public void handleNanopub(Nanopub np) {
						if (!ourPattern.matchesUri(np.getUri().toString())) return;
						try {
							db.loadNanopub(np);
						} catch (Exception ex) {
							logger.error("Failed to load nanopublication", ex);
						}
						stillAlive();
					}
				});
				processingFile.renameTo(new File(doneDir, f.getName()));
				logger.info("File loaded: " + processingFile);
			} catch (Exception ex) {
				logger.error("Failed to load file: " + f, ex);
			}
		}
	}

	private void stillAlive() {
		aliveAtTime = System.currentTimeMillis();
	}

}
