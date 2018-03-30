package ch.tkuhn.nanopub.server;

import java.io.IOException;

public class PopulatePackageCache {

	public static void main(String[] args) throws IOException {
		NanopubDb.get().populatePackageCache();
	}

}
