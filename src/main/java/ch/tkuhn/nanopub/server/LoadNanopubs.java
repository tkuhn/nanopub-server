package ch.tkuhn.nanopub.server;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.nanopub.MultiNanopubRdfHandler;
import org.nanopub.MultiNanopubRdfHandler.NanopubHandler;
import org.nanopub.Nanopub;
import org.openrdf.rio.RDFFormat;

import ch.tkuhn.nanopub.index.NanopubIndex;
import ch.tkuhn.nanopub.index.SimpleIndexCreator;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class LoadNanopubs {

	@com.beust.jcommander.Parameter(description = "input-files", required = true)
	private List<File> inputFiles = new ArrayList<File>();

	@com.beust.jcommander.Parameter(names = "-i", description = "Make index; load index and nanopubs")
	private boolean makeIndex;

	@com.beust.jcommander.Parameter(names = "-it", description = "Title of index (only with -i)")
	private String iTitle;

	@com.beust.jcommander.Parameter(names = "-id", description = "Description of index (only with -i)")
	private String iDesc;

	@com.beust.jcommander.Parameter(names = "-ic", description = "Creator of index (only with -i)")
	private List<String> iCreators = new ArrayList<>();

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

	private SimpleIndexCreator indexCreator = null;

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
		indexCreator = new SimpleIndexCreator() {

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
				System.out.println("Index URI: " + npc.getUri());
				try {
					NanopubDb.get().loadNanopub(npc);
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}

		};
		String url = ServerConf.getInfo().getPublicUrl();
		if (url != null && !url.isEmpty()) {
			indexCreator.setBaseUri(url);
		}
		if (iTitle != null) {
			indexCreator.setTitle(iTitle);
		}
		if (iDesc != null) {
			indexCreator.setDescription(iDesc);
		}
		for (String creator : iCreators) {
			indexCreator.addCreator(creator);
		}
	}

}
