package ch.tkuhn.nanopub.server;

import java.net.UnknownHostException;

import net.trustyuri.TrustyUriUtils;
import net.trustyuri.rdf.CheckNanopub;

import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.NanopubUtils;
import org.openrdf.rio.RDFFormat;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;

public class NanopubDb {

	// Use trig internally to keep namespaces:
	private static RDFFormat internalFormat = RDFFormat.TRIG;

	private static NanopubDb obj;

	public static NanopubDb get() {
		if (obj == null) {
			try {
				obj= new NanopubDb();
			} catch (UnknownHostException ex) {
				ex.printStackTrace();
				System.exit(1);
			}
		}
		return obj;
	}

	private ServerConf conf;
	private MongoClient mongo;
	private DB db;

	private NanopubDb() throws UnknownHostException {
		conf = ServerConf.get();
		mongo = new MongoClient(conf.getMongoDbHost(), conf.getMongoDbPort());
		db = mongo.getDB(conf.getMongoDbName());
	}

	public MongoClient getMongoClient() {
		return mongo;
	}

	public DB getDb() {
		return db;
	}

	public DBCollection getNanopubCollection() {
		return db.getCollection("nanopubs");
	}

	public Nanopub getNanopub(String artifactCode) throws Exception {
		BasicDBObject query = new BasicDBObject("_id", artifactCode);
		DBCursor cursor = NanopubDb.get().getNanopubCollection().find(query);
		if (!cursor.hasNext()) {
			return null;
		}
		String nanopubString = cursor.next().get("nanopub").toString();
		Nanopub np = new NanopubImpl(nanopubString, internalFormat);
		if (!CheckNanopub.isValid(np)) {
			throw new Exception("Nanopub verification failed");
		}
		return np;
	}

	public void loadNanopub(Nanopub np) throws Exception {
		if (!CheckNanopub.isValid(np)) {
			throw new Exception("Nanopub doesn't have a valid trusty URI");
		}
		String artifactCode = TrustyUriUtils.getArtifactCode(np.getUri().toString());
		String npString = NanopubUtils.writeToString(np, internalFormat);
		BasicDBObject id = new BasicDBObject("_id", artifactCode);
		BasicDBObject dbObj = id.append("nanopub", npString).append("uri", np.getUri().toString());
		getNanopubCollection().insert(dbObj);
	}

}
