package ch.tkuhn.nanopub.server;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import net.trustyuri.TrustyUriUtils;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.nanopub.MultiNanopubRdfHandler;
import org.nanopub.MultiNanopubRdfHandler.NanopubHandler;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.extra.server.NanopubServerUtils;
import org.nanopub.extra.server.NanopubSurfacePattern;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CollectNanopubs {

	private static final int processPagesPerRun = 10;

	private static NanopubDb db = NanopubDb.get();
	private static NanopubSurfacePattern ourPattern = ServerConf.getInfo().getNanopubSurfacePattern();

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	private ServerInfo peerInfo;
	private ScanPeers parent;
	private int peerPageSize;
	private boolean isFinished = false;
	private int loaded;
	private StopWatch watch;
	private long nextNp;

	public CollectNanopubs(ServerInfo peerInfo, ScanPeers parent) {
		this.peerInfo = peerInfo;
		this.parent = parent;
		parent.stillAlive();
	}

	public void run() {
		parent.stillAlive();
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
					startFromPage = (int) (startFromNp/peerPageSize) + 1;
					logger.info(newNanopubsCount + " new nanopubs");
				} else {
					logger.info(newNanopubsCount + " nanopubs in total (unknown journal)");
				}
			} else {
				newNanopubsCount = peerNanopubNo;
				logger.info(newNanopubsCount + " nanopubs in total (unknown peer state)");
			}
			int lastPage = (int) (peerNanopubNo/peerPageSize) + 1;
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
			isFinished = true;
			ScanPeers.lastTimeMeasureMap.put(peerInfo.getPublicUrl(), Float.MAX_VALUE);
		}
	}

	public boolean isFinished() {
		return isFinished;
	}

	private void processPage(int page, boolean isLastPage, long ignoreBeforePos) throws Exception {
		parent.stillAlive();
		logger.info("Process page " + page + " from " + peerInfo.getPublicUrl());
		loaded = 0;
		nextNp = (page-1) * peerPageSize;
		List<String> toLoad = new ArrayList<>();
		boolean downloadAsPackage = false;
		for (String nanopubUri : NanopubServerUtils.loadNanopubUriList(peerInfo, page)) {
			parent.stillAlive();
			if (nextNp >= ignoreBeforePos) {
				String ac = TrustyUriUtils.getArtifactCode(nanopubUri);
				if (ac != null && ourPattern.matchesUri(nanopubUri) && !db.hasNanopub(ac)) {
					toLoad.add(ac);
					if (!isLastPage && toLoad.size() > 5) {
						// Download entire package if more than 5 nanopubs are new
						downloadAsPackage = true;
						nextNp = (page-1) * peerPageSize;
						break;
					}
				}
			}
			nextNp++;
		}
		RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(5 * 1000).build();
		HttpClient c = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
		watch = new StopWatch();
		watch.start();
		if (downloadAsPackage) {
			logger.info("Download page " + page + " as compressed package...");
			HttpGet get = new HttpGet(peerInfo.getPublicUrl() + "package.gz?page=" + page);
			get.setHeader("Accept", "application/x-gzip");
			HttpResponse resp = c.execute(get);
			InputStream in = null;
			try {
				if (wasSuccessful(resp)) {
					in = new GZIPInputStream(resp.getEntity().getContent());
				} else {
					logger.info("Failed. Trying uncompressed package...");
					// This is for compability with older versions; to be removed at some point...
					get = new HttpGet(peerInfo.getPublicUrl() + "package?page=" + page);
					get.setHeader("Accept", "application/trig");
					resp = c.execute(get);
					if (!wasSuccessful(resp)) {
						logger.error("HTTP request failed: " + resp.getStatusLine().getReasonPhrase());
						recordTime();
						throw new RuntimeException(resp.getStatusLine().getReasonPhrase());
					}
					in = resp.getEntity().getContent();
				}
				MultiNanopubRdfHandler.process(RDFFormat.TRIG, in, new NanopubHandler() {
					@Override
					public void handleNanopub(Nanopub np) {
						nextNp++;
						if (watch.getTime() >  5 * 60 * 1000) {
							// Downloading the whole package should never take more than 5 minutes.
							logger.error("Downloading package took too long; interrupting");
							recordTime();
							throw new RuntimeException("Downloading package took too long; interrupting");
						}
						if (!ourPattern.matchesUri(np.getUri().stringValue())) return;
						try {
							loadNanopub(np);
						} catch (Exception ex) {
							throw new RuntimeException(ex);
						}
					}
				});
			} finally {
				if (in != null) in.close();
			}
		} else {
			logger.info("Download " + toLoad.size() + " nanopubs individually...");
			for (String ac : toLoad) {
				parent.stillAlive();
				HttpGet get = new HttpGet(peerInfo.getPublicUrl() + ac);
				get.setHeader("Accept", "application/trig");
				HttpResponse resp = c.execute(get);
				if (!wasSuccessful(resp)) {
					logger.error("HTTP request failed: " + resp.getStatusLine().getReasonPhrase());
					recordTime();
					throw new RuntimeException(resp.getStatusLine().getReasonPhrase());
				}
				InputStream in = null;
				try {
					in = resp.getEntity().getContent();
					loadNanopub(new NanopubImpl(in, RDFFormat.TRIG));
				} finally {
					if (in != null) in.close();
				}
			}
		}
		recordTime();
		logger.info("Update peer state: " + peerInfo.getPublicUrl() + " at position " + nextNp);
		db.updatePeerState(peerInfo, nextNp);
	}

	private void recordTime() {
		try {
			watch.stop();
			Float avg = null;
			if (loaded > 0) {
				avg = (float) watch.getTime() / loaded;
				ScanPeers.lastTimeMeasureMap.put(peerInfo.getPublicUrl(), avg);
			}
			logger.info("Time measurement: " + watch.getTime() + " for " + loaded + " nanopubs (average: " + avg + ")");
		} catch (Exception ex) {
			// ignore
		}
	}

	private boolean wasSuccessful(HttpResponse resp) {
		int c = resp.getStatusLine().getStatusCode();
		return c >= 200 && c < 300;
	}

	private void loadNanopub(Nanopub np) throws Exception {
		db.loadNanopub(np);
		loaded++;
	}

}
