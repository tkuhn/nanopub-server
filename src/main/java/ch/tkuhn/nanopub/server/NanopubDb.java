package ch.tkuhn.nanopub.server;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
	private long journalId;
	private long nextNanopubNo;
	private int pageSize;

	private NanopubDb() throws UnknownHostException {
		conf = ServerConf.get();
		mongo = new MongoClient(conf.getMongoDbHost(), conf.getMongoDbPort());
		db = mongo.getDB(conf.getMongoDbName());
		init();
	}

	private void init() {
		for (String s : conf.getInitialPeers()) {
			try {
				addPeer(s, false);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		if (!db.getCollectionNames().contains("journal")) {
			setJournalField("journal-id", new Random().nextLong() + "");
			setJournalField("next-nanopub-no", "0");
			setJournalField("page-size", ServerConf.getInfo().getInitPageSize() + "");
		}
		journalId = Long.parseLong(getJournalField("journal-id"));
		nextNanopubNo = Long.parseLong(getJournalField("next-nanopub-no"));
		pageSize = Integer.parseInt(getJournalField("page-size"));
	}

	private String getJournalField(String field) {
		BasicDBObject query = new BasicDBObject("_id", field);
		return getJournalCollection().find(query).next().get("value").toString();
	}

	private void setJournalField(String field, String value) {
		BasicDBObject dbObj = new BasicDBObject("_id", field).append("value", value);
		getJournalCollection().save(dbObj);
	}

	public MongoClient getMongoClient() {
		return mongo;
	}

	private DBCollection getNanopubCollection() {
		return db.getCollection("nanopubs");
	}

	public long getNanopubCount() {
		return getNanopubCollection().count();
	}

	private DBCollection getJournalCollection() {
		return db.getCollection("journal");
	}

	public Nanopub getNanopub(String artifactCode) throws Exception {
		BasicDBObject query = new BasicDBObject("_id", artifactCode);
		DBCursor cursor = getNanopubCollection().find(query);
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

	public boolean hasNanopub(String artifactCode) throws Exception {
		BasicDBObject query = new BasicDBObject("_id", artifactCode);
		return getNanopubCollection().find(query).hasNext();
	}

	public synchronized void loadNanopub(Nanopub np) throws Exception {
		if (!CheckNanopub.isValid(np)) {
			throw new Exception("Nanopub doesn't have a valid trusty URI");
		}
		String artifactCode = TrustyUriUtils.getArtifactCode(np.getUri().toString());
		String npString = NanopubUtils.writeToString(np, internalFormat);
		BasicDBObject id = new BasicDBObject("_id", artifactCode);
		BasicDBObject dbObj = new BasicDBObject("_id", artifactCode).append("nanopub", npString).append("uri", np.getUri().toString());
		DBCollection coll = getNanopubCollection();
		if (!coll.find(id).hasNext()) {
			coll.insert(dbObj);
			addToJournal(np);
		}
	}

	private void addToJournal(Nanopub np) {
		String pageContent = getCurrentPageContent();
		pageContent += np.getUri() + "\n";
		setJournalField(getCurrentPageName(), pageContent);
		nextNanopubNo++;
		setJournalField("next-nanopub-no", "" + nextNanopubNo);
	}

	public long getCurrentPageNo() {
		return nextNanopubNo / pageSize;
	}

	public String getCurrentPageName() {
		return "page" + getCurrentPageNo();
	}

	public String getPageContent(long pageNo) {
		String pageContent = "";
		if (nextNanopubNo % pageSize > 0) {
			pageContent = getJournalField(getCurrentPageName());
		}
		return pageContent;
	}

	public String getCurrentPageContent() {
		return getPageContent(getCurrentPageNo());
	}

	public DBCollection getPeerCollection() {
		return db.getCollection("peers");
	}

	public List<String> getPeerUris() {
		List<String> peers = new ArrayList<String>();
		DBCursor cursor = getPeerCollection().find();
		while (cursor.hasNext()) {
			peers.add(cursor.next().get("_id").toString());
		}
		return peers;
	}

	public void addPeer(String peerUrl) throws Exception {
		addPeer(peerUrl, true);
	}

	private void addPeer(String peerUrl, boolean check) throws Exception {
		if (peerUrl.equals(ServerConf.getInfo().getPublicUrl())) {
			return;
		}
		if (check) {
			ServerInfo info = ServerInfo.load(peerUrl);
			if (!info.getPublicUrl().equals(peerUrl)) {
				throw new Exception("Peer URL does not match its declared public URL");
			}
		}
		DBCollection coll = getPeerCollection();
		BasicDBObject dbObj = new BasicDBObject("_id", peerUrl);
		if (!coll.find(dbObj).hasNext()) {
			coll.insert(dbObj);
		}
	}

	public long getJournalId() {
		return journalId;
	}

	public synchronized long getNextNanopubNo() {
		return nextNanopubNo;
	}

	public int getPageSize() {
		return pageSize;
	}

}
