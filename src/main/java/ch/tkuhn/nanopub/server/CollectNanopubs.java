package ch.tkuhn.nanopub.server;

import java.io.InputStream;

import net.trustyuri.TrustyUriUtils;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.nanopub.NanopubImpl;
import org.openrdf.rio.RDFFormat;

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
				if (ac != null && !db.hasNanopub(ac)) {
					HttpGet get = new HttpGet(serverUri + ac);
					get.setHeader("Content-Type", "application/trig");
					InputStream in = HttpClientBuilder.create().build().execute(get).getEntity().getContent();
					db.loadNanopub(new NanopubImpl(in, RDFFormat.TRIG));
				}
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
