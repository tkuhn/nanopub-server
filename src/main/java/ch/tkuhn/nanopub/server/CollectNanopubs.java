package ch.tkuhn.nanopub.server;

import java.io.InputStream;

import net.trustyuri.TrustyUriUtils;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.nanopub.NanopubImpl;
import org.openrdf.rio.RDFFormat;

public class CollectNanopubs implements Runnable {

	private static NanopubDb db = NanopubDb.get();

	private ServerInfo peerInfo;

	public CollectNanopubs(ServerInfo peerInfo) {
		this.peerInfo = peerInfo;
	}

	@Override
	public void run() {
		try {
			System.err.println("Checking if there are new nanopubs at " + peerInfo.getPublicUrl());
			int startFromPage = 0;
			long startFromNp = 0;
			long newNanopubsCount;
			Pair<Long,Long> lastSeenPeerState = db.getLastSeenPeerState(peerInfo.getPublicUrl());
			if (lastSeenPeerState != null) {
				startFromNp = lastSeenPeerState.getRight();
				newNanopubsCount = peerInfo.getNextNanopubNo() - startFromNp;
				if (lastSeenPeerState.getLeft() == peerInfo.getJournalId()) {
					if (startFromNp == peerInfo.getNextNanopubNo()) {
						System.err.println("Already up-to-date");
						return;
					}
					startFromPage = (int) (startFromNp/peerInfo.getPageSize() + 1);
					System.err.println("Fetching " + newNanopubsCount + " new nanopubs");
				} else {
					System.err.println("Fetching all " + newNanopubsCount + " nanopubs (unknown journal)");
				}
			} else {
				newNanopubsCount = peerInfo.getNextNanopubNo();
				System.err.println("Fetching all " + newNanopubsCount + " nanopubs (unknown peer state)");
			}
			int lastPage = (int) (peerInfo.getNextNanopubNo()/peerInfo.getPageSize() + 1);
			long ignoreBeforePos = startFromNp;
			for (int p = startFromPage ; p < lastPage ; p++) {
				processPage(p, ignoreBeforePos);
				ignoreBeforePos = 0;
			}
			db.updatePeerState(peerInfo);
			System.err.println("Done");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void processPage(int startFromPage, long ignoreBeforePos) throws Exception {
		long processNp = (startFromPage-1) * peerInfo.getPageSize();
		for (String nanopubUri : Utils.loadNanopubUriList(peerInfo, startFromPage)) {
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
	}

}
