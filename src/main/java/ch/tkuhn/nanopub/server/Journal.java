package ch.tkuhn.nanopub.server;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;

public class Journal {

	private final long journalId;
	private final int pageSize;
	private final String uriPattern;
	private final String hashPattern;
	private long nextNanopubNo = -1;

	private DB db;

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	public Journal(DB db) {
		this.db = db;
		init();
		long j = Long.parseLong(getField("journal-id"));
		if (j == 0) {
			// Prebuilt DB from a downloaded package that doesn't have a journal ID yet
			j = Math.abs(new Random().nextLong());
			setField("journal-id", j + "");
		}
		journalId = j;
		pageSize = Integer.parseInt(getField("page-size"));
		uriPattern = getField("uri-pattern");
		hashPattern = getField("hash-pattern");
		nextNanopubNo = Long.parseLong(getField("next-nanopub-no"));
	}

	private void init() {
		if (!db.getCollectionNames().contains("journal")) {
			logger.info("No journal found: Create new one");
			setField("journal-version", NanopubServerUtils.journalVersion);
			setField("journal-id", Math.abs(new Random().nextLong()) + "");
			setField("next-nanopub-no", "0");
			setField("page-size", ServerConf.get().getInitPageSize() + "");
			setField("uri-pattern", ServerConf.get().getUriPattern());
			setField("hash-pattern", ServerConf.get().getHashPattern());
		}
		int v = getVersionValue();
		if (v == NanopubServerUtils.journalVersionValue) {
			logger.info("Journal version is up-to-date: " + getField("journal-version"));
			return;
		}
		if (v > NanopubServerUtils.journalVersionValue) {
			logger.error("Unknown (too new) journal version found");
			throw new RuntimeException("Unknown (too new) journal version found");
		}
		logger.info("Journal version is not up-to-date: " + getField("journal-version"));
		logger.info("Journal version is not up-to-date: Try to upgrade...");
		if (v < 2) {
			// Found journal version is too old: Abort
			logger.error("Old database found in MongoDB: " + ServerConf.get().getMongoDbName() +
				". Erase or rename this DB and restart the nanopub server.");
			throw new RuntimeException("Old database found in MongoDB");
		}
		if (v == 2) {
			setField("uri-pattern", "");
			setField("hash-pattern", "");
		}
		setField("journal-version", NanopubServerUtils.journalVersion);
		logger.info("Journal upgraded to version " + NanopubServerUtils.journalVersion);
	}

	public long getId() {
		return journalId;
	}

	public int getPageSize() {
		return pageSize;
	}

	public synchronized long getNextNanopubNo() {
		return nextNanopubNo;
	}

	public synchronized long getCurrentPageNo() {
		return getNextNanopubNo()/getPageSize() + 1;
	}

	public String getUriPattern() {
		return uriPattern;
	}

	public String getHashPattern() {
		return hashPattern;
	}

	private String getField(String field) {
		BasicDBObject query = new BasicDBObject("_id", field);
		DBCursor cursor = getJournalCollection().find(query);
		if (cursor.hasNext()) {
			return cursor.next().get("value").toString();
		} else {
			return null;
		}
	}

	private void setField(String field, String value) {
		BasicDBObject dbObj = new BasicDBObject("_id", field).append("value", value);
		getJournalCollection().save(dbObj);
	}

	private DBCollection getJournalCollection() {
		return db.getCollection("journal");
	}

	public synchronized String getStateId() {
		return getId() + "/" + getNextNanopubNo();
	}

	public synchronized int getVersionValue() {
		try {
			return NanopubServerUtils.getVersionValue(getField("journal-version"));
		} catch (Exception ex) {
			return 0;
		}
	}

	// Raise error if there is evidence of two parallel processes accessing the database:
	public synchronized void checkNextNanopubNo() {
		long loadedNextNanopubNo = Long.parseLong(getField("next-nanopub-no"));
		if (loadedNextNanopubNo != nextNanopubNo) {
			if (loadedNextNanopubNo > nextNanopubNo) nextNanopubNo = loadedNextNanopubNo;
			throw new RuntimeException("ERROR. Mismatch of nanopub count from MongoDB: several parallel processes?");
		}
	}

	synchronized void increaseNextNanopubNo() {
		nextNanopubNo++;
		setField("next-nanopub-no", "" + nextNanopubNo);
	}

	synchronized void setPageContent(long pageNo, String pageContent) {
		setField("page" + pageNo, pageContent);
	}

	public synchronized String getPageContent(long pageNo) {
		String pageName = "page" + pageNo;
		String pageContent = getField(pageName);
		if (pageContent == null) {
			if (getNextNanopubNo() % getPageSize() > 0) {
				throw new RuntimeException("Cannot find journal page: " + pageName);
			}
			// Make new page
			pageContent = "";
		}
		return pageContent;
	}

}
