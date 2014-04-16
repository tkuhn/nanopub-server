package ch.tkuhn.nanopub.server;

import java.io.File;

import net.trustyuri.TrustyUriUtils;
import net.trustyuri.rdf.CheckNanopub;

import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.NanopubUtils;
import org.openrdf.rio.RDFFormat;

import com.mongodb.BasicDBObject;

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
		Nanopub np = new NanopubImpl(file);
		if (!CheckNanopub.isValid(np)) {
			throw new Exception("Nanopub doesn't have a valid trusty URI");
		}
		String artifactCode = TrustyUriUtils.getArtifactCode(np.getUri().toString());
		String npString = NanopubUtils.writeToString(np, RDFFormat.TRIG);
		BasicDBObject id = new BasicDBObject("id", artifactCode);
		BasicDBObject dbObj = id.append("nanopub", npString);
		NanopubDb.get().getNanopubCollection().update(id, dbObj, true, false);
	}

}
