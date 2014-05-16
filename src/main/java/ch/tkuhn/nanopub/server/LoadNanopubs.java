package ch.tkuhn.nanopub.server;

import java.io.File;

import org.nanopub.MultiNanopubRdfHandler;
import org.nanopub.MultiNanopubRdfHandler.NanopubHandler;
import org.nanopub.Nanopub;
import org.openrdf.rio.RDFFormat;

public class LoadNanopubs {

	private LoadNanopubs() {}  // no instances allowed

	public static void main(String[] args) throws Exception {
		for (String filename : args) {
			RDFFormat format = RDFFormat.forFileName(filename);
			MultiNanopubRdfHandler.process(format, new File(filename), new NanopubHandler() {
				@Override
				public void handleNanopub(Nanopub np) {
					try {
						NanopubDb.get().loadNanopub(np);
					} catch (Exception ex) {
						throw new RuntimeException(ex);
					}
				}
			});
		}
	}

}
