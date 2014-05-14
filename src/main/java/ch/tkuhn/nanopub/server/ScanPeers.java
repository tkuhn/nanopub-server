package ch.tkuhn.nanopub.server;

public class ScanPeers implements Runnable {

	private static ScanPeers running;

	public static void check() {
		if (running != null) return;
		if (!ServerConf.get().isPeerScanEnabled()) return;
		running = new ScanPeers();
		new Thread(running).start();
	}

	@Override
	public void run() {
		try {
			Thread.sleep(10000);
		} catch(InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		NanopubDb db = NanopubDb.get();
		for (String peerUri : db.getPeerUris()) {
			try {
				ServerInfo si = ServerInfo.load(peerUri);
				for (String peerFromPeer : si.loadPeerList()) {
					db.addPeer(peerFromPeer);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		running = null;
	}

}
