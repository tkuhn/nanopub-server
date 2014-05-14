package ch.tkuhn.nanopub.server;


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
		collectPeers();
		collectNanopubs();
		running = null;
	}

	private void collectPeers() {
		for (String peerUri : db.getPeerUris()) {
			try {
				ServerInfo si = ServerInfo.load(peerUri);
				for (String peerFromPeer : Utils.loadPeerList(si)) {
					db.addPeer(peerFromPeer);
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
