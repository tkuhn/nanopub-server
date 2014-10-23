package ch.tkuhn.nanopub.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

	public static void check() {
		if (running != null) return;
		if (!ServerConf.get().isPeerScanEnabled()) return;
		running = new ScanPeers();
		new Thread(running).start();
	}

	private Logger logger = LoggerFactory.getLogger(this.getClass());

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
		logger.info("Collect and contact peers...");
		isFinished = true;
		List<String> peerUris = new ArrayList<>(db.getPeerUris());
		Collections.shuffle(peerUris);
		for (String peerUri : peerUris) {
			try {
				ServerInfo si = ServerInfo.load(peerUri);
				checkPeerLists(si);
				collectNanopubs(si);
			} catch (Exception ex) {
				logger.error(ex.getMessage(), ex);
			}
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

}
