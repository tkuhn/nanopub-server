package ch.tkuhn.nanopub.server;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;


public class ScanPeers implements Runnable {

	private static ScanPeers running;

	public static void check() {
		if (running != null) return;
		if (!ServerConf.get().isPeerScanEnabled()) return;
		running = new ScanPeers();
		new Thread(running).start();
	}

	private static NanopubDb db = NanopubDb.get();

	private ScanPeers() {
	}

	@Override
	public void run() {
		try {
			Thread.sleep(10000);
		} catch(InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		collectAndContactPeers();
		collectNanopubs();
		running = null;
	}

	private void collectAndContactPeers() {
		for (String peerUri : db.getPeerUris()) {
			try {
				ServerInfo si = ServerInfo.load(peerUri);
				String myUrl = ServerConf.getInfo().getPublicUrl();
				boolean knowsMe = false;
				for (String peerFromPeer : Utils.loadPeerList(si)) {
					if (myUrl.equals(peerFromPeer)) {
						knowsMe = true;
					} else {
						db.addPeer(peerFromPeer);
					}
				}
				if (!myUrl.isEmpty() && !knowsMe && si.isPostPeersEnabled()) {
					HttpPost post = new HttpPost(peerUri + PeerListPage.PAGE_NAME);
					post.setEntity(new StringEntity(myUrl));
					HttpResponse response = HttpClientBuilder.create().build().execute(post);
					System.err.println("Introduced myself to " + peerUri + ": " + response.getStatusLine().getReasonPhrase());
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	private void collectNanopubs() {
		if (!ServerConf.get().isCollectNanopubsEnabled()) return;
		for (String peerUri : db.getPeerUris()) {
			CollectNanopubs r = new CollectNanopubs(peerUri, "RA");
			r.run();
		}
	}

}
