package ch.tkuhn.nanopub.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import net.trustyuri.TrustyUriUtils;

import org.apache.commons.lang3.tuple.Pair;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.NanopubUtils;
import org.nanopub.NanopubWithNs;
import org.nanopub.extra.server.NanopubServerUtils;
import org.nanopub.extra.server.ServerInfo.ServerInfoException;
import org.nanopub.trusty.TrustyNanopubUtils;
import org.openrdf.OpenRDFException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;

/**
 * Class that connects to MongoDB. Each nanopub server instance needs its own DB (but this is not
 * checked).
 *
 * @author Tobias Kuhn
 */
public class NanopubDb {

	public static class NotTrustyNanopubException extends Exception {

		private static final long serialVersionUID = -3782872539656552144L;

		public NotTrustyNanopubException(Nanopub np) {
			super(np.getUri().toString());
		}

	}

	public static class OversizedNanopubException extends Exception {

		private static final long serialVersionUID = -8828914376012234462L;

		public OversizedNanopubException(Nanopub np) {
			super(np.getUri().toString());
		}

	}

	public static class NanopubDbException extends Exception {

		private static final long serialVersionUID = 162796031985052353L;

		public NanopubDbException(String message) {
			super(message);
		}

	}

	private static final boolean logNanopubLoading = ServerConf.get().isLogNanopubLoadingEnabled();

	// Use trig internally to keep namespaces:
	private static RDFFormat internalFormat = RDFFormat.TRIG;

	private static NanopubDb obj;

	public static NanopubDb get() {
		if (obj == null) {
			try {
				obj = new NanopubDb();
			} catch (UnknownHostException ex) {
				LoggerFactory.getLogger(NanopubDb.class).error(ex.getMessage(), ex);
				System.exit(1);
			}
		}
		return obj;
	}

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	private ServerConf conf;
	private MongoClient mongo;
	private DB db;
	private GridFS packageGridFs;
	private long journalId;
	private int pageSize;
	private long nextNanopubNo;

	private NanopubDb() throws UnknownHostException {
		conf = ServerConf.get();
		mongo = new MongoClient(conf.getMongoDbHost(), conf.getMongoDbPort());
		db = mongo.getDB(conf.getMongoDbName());
		packageGridFs = new GridFS(db, "packages_gzipped");
		init();
	}

	private void init() {
		for (String s : conf.getInitialPeers()) {
			addPeerToCollection(s);
		}
		if (!db.getCollectionNames().contains("journal")) {
			setJournalField("journal-version", "0.2");
			setJournalField("journal-id", Math.abs(new Random().nextLong()) + "");
			setJournalField("next-nanopub-no", "0");
			setJournalField("page-size", ServerConf.get().getInitPageSize() + "");
		} else if (getJournalVersionValue() < 0.002) {
			logger.error("Old database found in MongoDB: " + conf.getMongoDbName() +
				". Erase or rename this DB and restart the nanopub server.");
			throw new RuntimeException("Old database found in MongoDB");
		}
		journalId = Long.parseLong(getJournalField("journal-id"));
		pageSize = Integer.parseInt(getJournalField("page-size"));
		nextNanopubNo = Long.parseLong(getJournalField("next-nanopub-no"));
	}

	private String getJournalField(String field) {
		BasicDBObject query = new BasicDBObject("_id", field);
		DBCursor cursor = getJournalCollection().find(query);
		if (cursor.hasNext()) {
			return cursor.next().get("value").toString();
		} else {
			return null;
		}
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

	private DBCollection getJournalCollection() {
		return db.getCollection("journal");
	}

	public Nanopub getNanopub(String artifactCode) {
		BasicDBObject query = new BasicDBObject("_id", artifactCode);
		DBCursor cursor = getNanopubCollection().find(query);
		if (!cursor.hasNext()) {
			return null;
		}
		String nanopubString = cursor.next().get("nanopub").toString();
		Nanopub np = null;
		try {
			np = new NanopubImpl(nanopubString, internalFormat);
		} catch (MalformedNanopubException ex) {
			throw new RuntimeException("Stored nanopub is not wellformed (this shouldn't happen)", ex);
		} catch (OpenRDFException ex) {
			throw new RuntimeException("Stored nanopub is corrupted (this shouldn't happen)", ex);
		}
		if (ServerConf.get().isCheckNanopubsOnGetEnabled() && !TrustyNanopubUtils.isValidTrustyNanopub(np)) {
			throw new RuntimeException("Stored nanopub is not trusty (this shouldn't happen)");
		}
		return np;
	}

	public boolean hasNanopub(String artifactCode) {
		BasicDBObject query = new BasicDBObject("_id", artifactCode);
		return getNanopubCollection().find(query).hasNext();
	}

	public synchronized void loadNanopub(Nanopub np) throws NotTrustyNanopubException,
			OversizedNanopubException, NanopubDbException {
		if (np instanceof NanopubWithNs) {
			((NanopubWithNs) np).removeUnusedPrefixes();
		}
		if (!TrustyNanopubUtils.isValidTrustyNanopub(np)) {
			throw new NotTrustyNanopubException(np);
		}
		ServerInfo info = ServerConf.getInfo();
		if (info.getMaxNanopubTriples() != null && np.getTripleCount() > info.getMaxNanopubTriples()) {
			throw new OversizedNanopubException(np);
		}
		if (info.getMaxNanopubBytes() != null && np.getByteCount() > info.getMaxNanopubBytes()) {
			throw new OversizedNanopubException(np);
		}
		if (info.getMaxNanopubs() != null && nextNanopubNo > info.getMaxNanopubs()) {
			throw new NanopubDbException("Server is full (maximum number of nanopubs reached)");
		}
		String artifactCode = TrustyUriUtils.getArtifactCode(np.getUri().toString());
		String npString = null;
		try {
			npString = NanopubUtils.writeToString(np, internalFormat);
		} catch (RDFHandlerException ex) {
			throw new RuntimeException("Unexpected exception when processing nanopub", ex);
		}
		db.requestStart();
		try {
			db.requestEnsureConnection();
			BasicDBObject id = new BasicDBObject("_id", artifactCode);
			BasicDBObject dbObj = new BasicDBObject("_id", artifactCode).append("nanopub", npString).append("uri", np.getUri().toString());
			DBCollection coll = getNanopubCollection();
			if (!coll.find(id).hasNext()) {
				readNextNanopubNo();
				long currentPageNo = getCurrentPageNo();
				String pageContent = getPageContent(currentPageNo);
				pageContent += np.getUri() + "\n";
				nextNanopubNo++;
				// TODO Implement proper transactions, rollback, etc.
				// The following three lines of code are critical. If Java gets interrupted
				// in between, the data will remain in a slightly inconsistent state (but, I
				// think, without serious consequences).
				setJournalField("next-nanopub-no", "" + nextNanopubNo);
				// If interrupted here, the current page of the journal will miss one entry
				// (e.g. contain only 999 instead of 1000 entries).
				setPageContent(currentPageNo, pageContent);
				// If interrupted here, journal will contain an entry that cannot be found in
				// the database. This entry might be loaded later and then appear twice in the
				// journal.
				coll.insert(dbObj);
			}
		} finally {
			db.requestDone();
		}
		if (logNanopubLoading) {
			logger.info("Nanopub loaded: " + np.getUri());
		}
	}

	public long getCurrentPageNo() {
		return getNextNanopubNo()/getPageSize() + 1;
	}

	private void setPageContent(long pageNo, String pageContent) {
		setJournalField("page" + pageNo, pageContent);
	}

	public String getPageContent(long pageNo) {
		String pageName = "page" + pageNo;
		String pageContent = getJournalField(pageName);
		if (pageContent == null) {
			if (getNextNanopubNo() % getPageSize() > 0) {
				throw new RuntimeException("Cannot find journal page: " + pageName);
			}
			// Make new page
			pageContent = "";
		}
		return pageContent;
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

	public void addPeer(String peerUrl) throws ServerInfoException {
		ServerInfo.load(peerUrl);  // throw exception if something is wrong
		addPeerToCollection(peerUrl);
	}

	private void addPeerToCollection(String peerUrl) {
		if (peerUrl.equals(ServerConf.getInfo().getPublicUrl())) {
			return;
		}
		DBCollection coll = getPeerCollection();
		BasicDBObject dbObj = new BasicDBObject("_id", peerUrl);
		if (!coll.find(dbObj).hasNext()) {
			coll.insert(dbObj);
		}
	}

	public void updatePeerState(ServerInfo peerInfo, long npno) {
		String url = peerInfo.getPublicUrl();
		BasicDBObject q = new BasicDBObject("_id", url);
		long jid = peerInfo.getJournalId();
		BasicDBObject update = new BasicDBObject("_id", url).append("journalId", jid).append("nextNanopubNo", npno);
		getPeerCollection().update(q, update);
	}

	public Pair<Long,Long> getLastSeenPeerState(String peerUrl) {
		BasicDBObject q = new BasicDBObject("_id", peerUrl);
		Long journalId = null;
		Long nextNanopubNo = null;
		DBObject r = getPeerCollection().find(q).next();
		if (r.containsField("journalId")) journalId = Long.parseLong(r.get("journalId").toString());
		if (r.containsField("nextNanopubNo")) nextNanopubNo = Long.parseLong(r.get("nextNanopubNo").toString());
		if (journalId == null || nextNanopubNo == null) return null;
		return Pair.of(journalId, nextNanopubNo);
	}

	public void writePackageToStream(long pageNo, boolean gzipped, OutputStream out) throws IOException {
		if (pageNo < 1 || pageNo >= getCurrentPageNo()) {
			throw new IllegalArgumentException("Not a complete page: " + pageNo);
		}
		GridFSDBFile f = packageGridFs.findOne(pageNo + "");
		OutputStream packageOut = null;
		InputStream packageAsStream = null;
		try {
			if (f == null) {
				if (gzipped) {
					out = new GZIPOutputStream(out);
				}
				ByteArrayOutputStream bOut = new ByteArrayOutputStream();
				packageOut = new GZIPOutputStream(bOut);
				String pageContent = getPageContent(pageNo);
				for (String uri : pageContent.split("\\n")) {
					Nanopub np = getNanopub(TrustyUriUtils.getArtifactCode(uri));
					String s;
					try {
						s = NanopubUtils.writeToString(np, RDFFormat.TRIG);
					} catch (RDFHandlerException ex) {
						throw new RuntimeException("Unexpected RDF handler exception", ex);
					}
					byte[] bytes = (s + "\n").getBytes();
					out.write(bytes);
					packageOut.write(bytes);
				}
				packageAsStream = new ByteArrayInputStream(bOut.toByteArray());
				GridFSInputFile i = packageGridFs.createFile(packageAsStream);
				i.setFilename(pageNo + "");
				i.save();
			} else {
				if (gzipped) {
					f.writeTo(out);
				} else {
					GZIPInputStream in = new GZIPInputStream(f.getInputStream());
					byte[] buffer = new byte[1024];
					int len;
					while ((len = in.read(buffer)) > 0) {
						out.write(buffer, 0, len);
					}
					in.close();
				}
			}
		} finally {
			if (out != null) out.close();
			if (packageOut != null) packageOut.close();
			if (packageAsStream != null) packageAsStream.close();
		}
	}

	public long getJournalId() {
		return journalId;
	}

	public synchronized long getNextNanopubNo() {
		return nextNanopubNo;
	}

	public synchronized void readNextNanopubNo() {
		long loadedNextNanopubNo = Long.parseLong(getJournalField("next-nanopub-no"));
		if (loadedNextNanopubNo != nextNanopubNo) {
			nextNanopubNo = loadedNextNanopubNo;
			throw new RuntimeException("ERROR. Mismatch of nanopub count from MongoDB: several parallel processes?");
		}
	}

	public String getJournalStateId() {
		return getJournalId() + "/" + getNextNanopubNo();
	}

	public int getPageSize() {
		return pageSize;
	}

	public float getJournalVersionValue() {
		try {
			return NanopubServerUtils.getVersionValue(getJournalField("journal-version"));
		} catch (Exception ex) {
			return 0.0f;
		}
	}

}
