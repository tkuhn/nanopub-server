package ch.tkuhn.nanopub.server;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.nanopub.MultiNanopubRdfHandler;
import org.nanopub.MultiNanopubRdfHandler.NanopubHandler;
import org.nanopub.Nanopub;
import org.openrdf.rio.RDFFormat;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class LoadNanopubs {

	@com.beust.jcommander.Parameter(description = "input-files", required = true)
	private List<File> inputFiles = new ArrayList<File>();

	public static void main(String[] args) {
		LoadNanopubs obj = new LoadNanopubs();
		JCommander jc = new JCommander(obj);
		try {
			jc.parse(args);
		} catch (ParameterException ex) {
			jc.usage();
			System.exit(1);
		}
		try {
			obj.run();
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	private LoadNanopubs() {
	}

	public void run() throws Exception {
		for (File f : inputFiles) {
			RDFFormat format = RDFFormat.forFileName(f.getName());
			MultiNanopubRdfHandler.process(format, f, new NanopubHandler() {
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
