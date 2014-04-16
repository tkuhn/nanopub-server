package ch.tkuhn.nanopub.server;

import java.io.File;

import org.nanopub.NanopubImpl;

public class LoadNanopubs {

	private LoadNanopubs() {}  // no instances allowed

	public static void main(String[] args) {
		try {
			for (String s : args) {
				NanopubDb.get().loadNanopub(new NanopubImpl(new File(s)));
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

}
