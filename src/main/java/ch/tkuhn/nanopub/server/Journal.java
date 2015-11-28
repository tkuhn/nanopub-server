package ch.tkuhn.nanopub.server;

import java.util.Random;

import org.nanopub.extra.server.NanopubServerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;

public class Journal {

	private final long journalId;
	private int pageSize;
	private long nextNanopubNo;

	private DB db;

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	public Journal(DB db) {
		this.db = db;
		if (!db.getCollectionNames().contains("journal")) {
			setField("journal-version", "0.2");
			setField("journal-id", Math.abs(new Random().nextLong()) + "");
			setField("next-nanopub-no", "0");
			setField("page-size", ServerConf.get().getInitPageSize() + "");
		} else if (getVersionValue() < 0.002) {
			logger.error("Old database found in MongoDB: " + ServerConf.get().getMongoDbName() +
				". Erase or rename this DB and restart the nanopub server.");
			throw new RuntimeException("Old database found in MongoDB");
		}
		journalId = Long.parseLong(getField("journal-id"));
		pageSize = Integer.parseInt(getField("page-size"));
		nextNanopubNo = Long.parseLong(getField("next-nanopub-no"));
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

	public synchronized float getVersionValue() {
		try {
			return NanopubServerUtils.getVersionValue(getField("journal-version"));
		} catch (Exception ex) {
			return 0.0f;
		}
	}

	// Raise error if there is evidence of two parallel processes accessing the database:
	public synchronized void checkNextNanopubNo() {
		long loadedNextNanopubNo = Long.parseLong(getField("next-nanopub-no"));
		if (loadedNextNanopubNo != nextNanopubNo) {
			nextNanopubNo = loadedNextNanopubNo;
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
