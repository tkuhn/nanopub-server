package ch.tkuhn.nanopub.server;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import net.trustyuri.TrustyUriUtils;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.nanopub.MultiNanopubRdfHandler;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.MultiNanopubRdfHandler.NanopubHandler;
import org.nanopub.extra.server.NanopubServerUtils;
import org.openrdf.rio.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CollectNanopubs implements Runnable {

	private static final int processPagesPerRun = 1;

	private static NanopubDb db = NanopubDb.get();
	private static final boolean logNanopubLoading = ServerConf.get().isLogNanopubLoadingEnabled();

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	private ServerInfo peerInfo;
	private int peerPageSize;
	private boolean isFinished = false;

	public CollectNanopubs(ServerInfo peerInfo) {
		this.peerInfo = peerInfo;
	}

	@Override
	public void run() {
		try {
			logger.info("Checking if there are new nanopubs at " + peerInfo.getPublicUrl());
			int startFromPage = 1;
			long startFromNp = 0;
			long newNanopubsCount;
			peerPageSize = peerInfo.getPageSize();
			long peerNanopubNo = peerInfo.getNextNanopubNo();
			long peerJid = peerInfo.getJournalId();
			Pair<Long,Long> lastSeenPeerState = db.getLastSeenPeerState(peerInfo.getPublicUrl());
			if (lastSeenPeerState != null) {
				startFromNp = lastSeenPeerState.getRight();
				newNanopubsCount = peerNanopubNo - startFromNp;
				if (lastSeenPeerState.getLeft() == peerJid) {
					if (startFromNp == peerNanopubNo) {
						logger.info("Already up-to-date");
						isFinished = true;
						return;
					}
					startFromPage = (int) (startFromNp/peerPageSize + 1);
					logger.info(newNanopubsCount + " new nanopubs");
				} else {
					logger.info(newNanopubsCount + " nanopubs in total (unknown journal)");
				}
			} else {
				newNanopubsCount = peerNanopubNo;
				logger.info(newNanopubsCount + " nanopubs in total (unknown peer state)");
			}
			int lastPage = (int) (peerNanopubNo/peerPageSize + 1);
			long ignoreBeforePos = startFromNp;
			logger.info("Starting from page " + startFromPage + " of " + lastPage);
			int pageCountThisRun = 0;
			boolean interrupted = false;
			for (int p = startFromPage ; p <= lastPage ; p++) {
				pageCountThisRun++;
				if (pageCountThisRun > processPagesPerRun) {
					interrupted = true;
					break;
				}
				processPage(p, p == lastPage, ignoreBeforePos);
				ignoreBeforePos = 0;
			}
			if (interrupted) {
				logger.info("To be continued (see if other peers have new nanopubs)");
			} else {
				logger.info("Done");
				isFinished = true;
			}
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
	}

	public boolean isFinished() {
		return isFinished;
	}

	private void processPage(int page, boolean isLastPage, long ignoreBeforePos) throws Exception {
		logger.info("Process page " + page + " from " + peerInfo.getPublicUrl());
		long processNp = (page-1) * peerPageSize;
		List<String> toLoad = new ArrayList<>();
		boolean downloadAsPackage = false;
		for (String nanopubUri : NanopubServerUtils.loadNanopubUriList(peerInfo, page)) {
			if (processNp >= ignoreBeforePos) {
				String ac = TrustyUriUtils.getArtifactCode(nanopubUri);
				if (ac != null && !db.hasNanopub(ac)) {
					toLoad.add(ac);
					if (!isLastPage && toLoad.size() > 0.1 * db.getPageSize()) {
						// Download entire package if at least 10% of nanopubs are new
						downloadAsPackage = true;
						break;
					}
				}
			}
			processNp++;
		}
		if (downloadAsPackage) {
			logger.info("Download page " + page + " as package...");
			HttpGet get = new HttpGet(peerInfo.getPublicUrl() + "package?page=" + page);
			get.setHeader("Accept", "application/trig");
			HttpResponse resp = HttpClientBuilder.create().build().execute(get);
			if (!wasSuccessful(resp)) {
				throw new RuntimeException(resp.getStatusLine().getReasonPhrase());
			}
			InputStream in = resp.getEntity().getContent();
			MultiNanopubRdfHandler.process(RDFFormat.TRIG, in, new NanopubHandler() {
				@Override
				public void handleNanopub(Nanopub np) {
					try {
						loadNanopub(np);
					} catch (Exception ex) {
						throw new RuntimeException(ex);
					}
				}
			});
		} else {
			logger.info("Download " + toLoad.size() + " nanopubs individually...");
			for (String ac : toLoad) {
				HttpGet get = new HttpGet(peerInfo.getPublicUrl() + ac);
				get.setHeader("Accept", "application/trig");
				HttpResponse resp = HttpClientBuilder.create().build().execute(get);
				if (!wasSuccessful(resp)) {
					throw new RuntimeException(resp.getStatusLine().getReasonPhrase());
				}
				InputStream in = resp.getEntity().getContent();
				loadNanopub(new NanopubImpl(in, RDFFormat.TRIG));
			}
		}
		db.updatePeerState(peerInfo, processNp);
	}

	private boolean wasSuccessful(HttpResponse resp) {
		int c = resp.getStatusLine().getStatusCode();
		return c >= 200 && c < 300;
	}

	private void loadNanopub(Nanopub np) throws Exception {
		db.loadNanopub(np);
		if (logNanopubLoading) {
			logger.info("Nanopub loaded: " + np.getUri());
		}
	}

}
