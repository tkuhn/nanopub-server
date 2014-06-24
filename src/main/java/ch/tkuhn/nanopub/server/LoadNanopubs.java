package ch.tkuhn.nanopub.server;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.nanopub.MultiNanopubRdfHandler;
import org.nanopub.NanopubCreator;
import org.nanopub.MultiNanopubRdfHandler.NanopubHandler;
import org.nanopub.Nanopub;
import org.openrdf.rio.RDFFormat;

import ch.tkuhn.nanopub.index.NanopubIndex;
import ch.tkuhn.nanopub.index.NanopubIndexCreator;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class LoadNanopubs {

	@com.beust.jcommander.Parameter(description = "input-files", required = true)
	private List<File> inputFiles = new ArrayList<File>();

	@com.beust.jcommander.Parameter(names = "-i", description = "Make index; load index and nanopubs")
	private boolean makeIndex;

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

	private NanopubIndexCreator indexCreator = null;

	private LoadNanopubs() {
	}

	public void run() throws Exception {
		if (makeIndex) {
			initIndexCreator();
		}
		for (File f : inputFiles) {
			RDFFormat format = RDFFormat.forFileName(f.getName());
			MultiNanopubRdfHandler.process(format, f, new NanopubHandler() {
				@Override
				public void handleNanopub(Nanopub np) {
					if (indexCreator != null) {
						indexCreator.addElement(np);
					}
					try {
						NanopubDb.get().loadNanopub(np);
					} catch (Exception ex) {
						throw new RuntimeException(ex);
					}
				}
			});
		}
		if (indexCreator != null) {
			indexCreator.finalize();
		}
	}

	private void initIndexCreator() {
		indexCreator = new NanopubIndexCreator() {

			@Override
			public String getBaseUri() {
				String s = ServerConf.getInfo().getPublicUrl();
				if (s == null || s.isEmpty()) {
					return "http://tkuhn.ch/nanopub-server/index/";
				} else {
					return s;
				}
			}

			@Override
			public void enrichIncompleteIndex(NanopubCreator npCreator) {}

			@Override
			public void enrichCompleteIndex(NanopubCreator npCreator) {}

			@Override
			public void handleIncompleteIndex(NanopubIndex npc) {
				try {
					NanopubDb.get().loadNanopub(npc);
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}

			@Override
			public void handleCompleteIndex(NanopubIndex npc) {
				try {
					NanopubDb.get().loadNanopub(npc);
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}

		};
	}

}
