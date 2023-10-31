package ch.tkuhn.nanopub.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.nanopub.extra.server.NanopubServerUtils;
import org.nanopub.extra.server.NanopubSurfacePattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScanPeers implements Runnable {

	private static ScanPeers running;
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
		if (!ServerConf.get().isPeerScanEnabled()) return;
		running = new ScanPeers();
		thread = new Thread(running);
		thread.setDaemon(true);
		thread.start();
	}

	private Logger logger = LoggerFactory.getLogger(this.getClass());
	private Random random = new Random();

	protected static Map<String,Float> lastTimeMeasureMap = new HashMap<String,Float>();

	private boolean peerListsChecked = false;
	private boolean isFinished = false;
	private long aliveAtTime;

	private ScanPeers() {
		stillAlive();
	}

	@Override
	public void run() {
		stillAlive();
		logger.info("Start peer scanning thread");
		try {
			int ms = ServerConf.get().getWaitMsBeforePeerScan();
			logger.info("Wait " + ms + "ms...");
			Thread.sleep(ms);
			while (!isFinished) {
				collectAndContactPeers();
			}
		} catch (InterruptedException ex) {
			throw new RuntimeException(ex);
		} finally {
			running = null;
		}
	}

	private void collectAndContactPeers() {
		stillAlive();
		isFinished = true;
		List<String> peerUris = new ArrayList<>(db.getPeerUris());
		// Ignore own URL if present in peer list:
		peerUris.remove(ServerConf.getInfo().getPublicUrl());
		if (random.nextFloat() < 0.1) {
			// Use random ordering with 10% chance
			logger.info("Collect and contact peers (random ordering)...");
			Collections.shuffle(peerUris);
		} else {
			// Use fast-first ordering with 90% chance
			logger.info("Collect and contact peers (fast-first ordering)...");
			Collections.sort(peerUris, fastFirstSorter);
		}
		for (String peerUri : peerUris) {
			ServerInfo si = null;
			try {
				logger.info("Trying " + peerUri);
				si = ServerInfo.load(peerUri);
				checkPeerLists(si);
				collectNanopubs(si);
			} catch (Exception ex) {
				logger.error(ex.getMessage(), ex);
				if (si != null) {
					lastTimeMeasureMap.put(si.getPublicUrl(), Float.MAX_VALUE);
				}
				isFinished = true;
			}
			if (!isFinished) break; // start over again
		}
	}

	private void checkPeerLists(ServerInfo si) throws Exception {
		stillAlive();
		if (peerListsChecked) return;
		logger.info("Check peer lists...");

		String myUrl = ServerConf.getInfo().getPublicUrl();
		boolean knowsMe = false;
		for (String peerFromPeer : NanopubServerUtils.loadPeerList(si)) {
			stillAlive();
			if (myUrl.equals(peerFromPeer)) {
				knowsMe = true;
			} else {
				try {
					db.addPeer(peerFromPeer);
				} catch (Exception ex) {
					logger.error("Failed adding peer: " + peerFromPeer, ex);
				}
			}
		}
		if (!ServerConf.get().isRunAsLocalServerEnabled() && !myUrl.isEmpty() && !knowsMe && si.isPostPeersEnabled()) {
			HttpPost post = new HttpPost(si.getPublicUrl() + PeerListPage.PAGE_NAME);
			post.setEntity(new StringEntity(myUrl));
			HttpResponse response = HttpClientBuilder.create().build().execute(post);
			logger.info("Introduced myself to " + si.getPublicUrl() + ": " + response.getStatusLine().getReasonPhrase());
		}

		peerListsChecked = true;
	}

	private void collectNanopubs(ServerInfo si) {
		stillAlive();
		logger.info("Check if we are interested in more nanopubs...");
		if (!ServerConf.get().isCollectNanopubsEnabled()) {
			logger.info("This server doesn't collect nanopubs. Aborting nanopub collection.");
			isFinished = true;
			return;
		}
		if (NanopubDb.get().isFull()) {
			logger.info("This server is full. Aborting nanopub collection.");
			isFinished = true;
			return;
		}
		logger.info("Check if other server's nanopub subset overlaps with ours...");
		if (!ourPattern.overlapsWith(si.getNanopubSurfacePattern())) {
			logger.info("Patters of this and other server don't overlap. Aborting nanopub collection.");
			isFinished = true;
			return;
		}
		logger.info("Collecting nanopubs...");
		CollectNanopubs r = new CollectNanopubs(si, this);
		r.run();
		if (!r.isFinished()) {
			isFinished = false;
		}
	}

	protected void stillAlive() {
		aliveAtTime = System.currentTimeMillis();
	}


	private static final FastFirstSorter fastFirstSorter = new FastFirstSorter();

	private static class FastFirstSorter implements Comparator<String> {

		@Override
		public int compare(String s1, String s2) {
			Float l1 = lastTimeMeasureMap.get(s1);
			if (l1 == null) return -1;
			Float l2 = lastTimeMeasureMap.get(s2);
			if (l2 == null) return 1;
			return (int) ((l1*1000) - (l2*1000));
		}

	}

}
