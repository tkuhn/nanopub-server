package ch.tkuhn.nanopub.server;

import java.net.URL;

import net.trustyuri.TrustyUriUtils;

import org.nanopub.NanopubImpl;

public class CollectNanopubs implements Runnable {

	private static  NanopubDb db = NanopubDb.get();

	private String serverUri;
	private String artifactCodeStart;

	public CollectNanopubs(String serverUri, String artifactCodeStart) {
		this.serverUri = serverUri;
		this.artifactCodeStart = artifactCodeStart;
	}

	@Override
	public void run() {
		try {
			ServerInfo si = ServerInfo.load(serverUri);
			for (String nanopubUri : Utils.loadNanopubUriList(si, artifactCodeStart)) {
				if ("...".equals(nanopubUri)) {
					processDeeperLevel();
					break;
				}
				String ac = TrustyUriUtils.getArtifactCode(nanopubUri);
				db.loadNanopub(new NanopubImpl(new URL(serverUri + ac)));
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void processDeeperLevel() {
		if (artifactCodeStart.length() > 44) {
			throw new RuntimeException("Unexcepted search depth: something went wrong");
		}
		for (char ch : Utils.base64Alphabet.toCharArray()) {
			CollectNanopubs obj = new CollectNanopubs(serverUri, artifactCodeStart + ch);
			obj.run();
		}
	}

}
