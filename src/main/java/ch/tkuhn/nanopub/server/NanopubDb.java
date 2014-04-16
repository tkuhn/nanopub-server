package ch.tkuhn.nanopub.server;

import java.net.UnknownHostException;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;

public class NanopubDb {

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

}
