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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScanPeers implements Runnable {

	private static ScanPeers running;

	private static NanopubDb db = NanopubDb.get();

	public static synchronized void check() {
		if (running != null) return;
		if (!ServerConf.get().isPeerScanEnabled()) return;
		running = new ScanPeers();
		new Thread(running).start();
	}

	private Logger logger = LoggerFactory.getLogger(this.getClass());
	private Random random = new Random();

	protected static Map<String,Long> lastTimeMeasureMap = new HashMap<String,Long>();

	private boolean peerListsChecked = false;
	private boolean isFinished = false;

	private ScanPeers() {
	}

	@Override
	public void run() {
		logger.info("Start peer scanning thread");
		try {
			try {
				int ms = ServerConf.get().getWaitMsBeforePeerScan();
				logger.info("Wait " + ms + "ms...");
				Thread.sleep(ms);
			} catch(InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			while (!isFinished) {
				collectAndContactPeers();
			}
		} finally {
			running = null;
		}
	}

	private void collectAndContactPeers() {
		isFinished = true;
		List<String> peerUris = new ArrayList<>(db.getPeerUris());
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
				si = ServerInfo.load(peerUri);
				checkPeerLists(si);
				collectNanopubs(si);
			} catch (Exception ex) {
				logger.error(ex.getMessage(), ex);
				if (si != null) {
					lastTimeMeasureMap.put(si.getPublicUrl(), Long.MAX_VALUE);
				}
				isFinished = true;
			}
			if (!isFinished) break; // start over again
		}
	}

	private void checkPeerLists(ServerInfo si) throws Exception {
		if (peerListsChecked) return;
		logger.info("Check peer lists...");

		String myUrl = ServerConf.getInfo().getPublicUrl();
		boolean knowsMe = false;
		for (String peerFromPeer : NanopubServerUtils.loadPeerList(si)) {
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
		if (!myUrl.isEmpty() && !knowsMe && si.isPostPeersEnabled()) {
			HttpPost post = new HttpPost(si.getPublicUrl() + PeerListPage.PAGE_NAME);
			post.setEntity(new StringEntity(myUrl));
			HttpResponse response = HttpClientBuilder.create().build().execute(post);
			logger.info("Introduced myself to " + si.getPublicUrl() + ": " + response.getStatusLine().getReasonPhrase());
		}

		peerListsChecked = true;
	}

	private void collectNanopubs(ServerInfo si) {
		if (!ServerConf.get().isCollectNanopubsEnabled()) {
			isFinished = true;
			return;
		}
		logger.info("Collecting nanopubs...");
		CollectNanopubs r = new CollectNanopubs(si);
		r.run();
		if (!r.isFinished()) {
			isFinished = false;
		}
	}


	private static final FastFirstSorter fastFirstSorter = new FastFirstSorter();

	private static class FastFirstSorter implements Comparator<String> {

		@Override
		public int compare(String s1, String s2) {
			Long l1 = lastTimeMeasureMap.get(s1);
			if (l1 == null) return -1;
			Long l2 = lastTimeMeasureMap.get(s2);
			if (l2 == null) return 1;
			return (int) (l1 - l2);
		}

	}

}
