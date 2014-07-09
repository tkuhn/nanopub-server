package ch.tkuhn.nanopub.server;

import java.io.InputStream;

import net.trustyuri.TrustyUriUtils;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.nanopub.NanopubImpl;
import org.nanopub.extra.server.NanopubServerUtils;
import org.openrdf.rio.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CollectNanopubs implements Runnable {

	private static final int processPagesPerRun = 1;

	private static NanopubDb db = NanopubDb.get();

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
				processPage(p, ignoreBeforePos);
				ignoreBeforePos = 0;
			}
			if (interrupted) {
				logger.info("To be continued (see if other peers have new nanopubs)");
			} else {
				logger.info("Done");
				isFinished = true;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public boolean isFinished() {
		return isFinished;
	}

	private void processPage(int page, long ignoreBeforePos) throws Exception {
		logger.info("Process page " + page + " from " + peerInfo.getPublicUrl());
		long processNp = (page-1) * peerPageSize;
		for (String nanopubUri : NanopubServerUtils.loadNanopubUriList(peerInfo, page)) {
			if (processNp >= ignoreBeforePos) {
				String ac = TrustyUriUtils.getArtifactCode(nanopubUri);
				if (ac != null && !db.hasNanopub(ac)) {
					HttpGet get = new HttpGet(peerInfo.getPublicUrl() + ac);
					get.setHeader("Content-Type", "application/trig");
					InputStream in = HttpClientBuilder.create().build().execute(get).getEntity().getContent();
					db.loadNanopub(new NanopubImpl(in, RDFFormat.TRIG));
				}
			}
			processNp++;
		}
		db.updatePeerState(peerInfo, processNp);
	}

}
