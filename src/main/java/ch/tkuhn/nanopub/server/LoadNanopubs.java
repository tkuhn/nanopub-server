package ch.tkuhn.nanopub.server;

import java.io.File;

import org.nanopub.NanopubImpl;

public class LoadNanopubs {

	private LoadNanopubs() {}  // no instances allowed

	public static void main(String[] args) {
		try {
			for (String s : args) {
				loadNanopub(new File(s));
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	public static void loadNanopub(File file) throws Exception {
		try {
			NanopubDb.get().loadNanopub(new NanopubImpl(file));
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

}
