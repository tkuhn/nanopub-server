package ch.tkuhn.nanopub.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import com.google.gson.Gson;

public class ServerInfo {

	public static ServerInfo load(String serverUrl) throws IOException {
		HttpGet get = new HttpGet(serverUrl);
		get.setHeader("Content-Type", "application/json");
	    InputStream in = HttpClientBuilder.create().build().execute(get).getEntity().getContent();
		return new Gson().fromJson(new InputStreamReader(in), ServerInfo.class);
	}

	private String publicUrl;
	private String admin;
	private boolean postNanopubsEnabled;
	private boolean postPeersEnabled;

	private int pageSize = -1;
	private long nextNanopubNo = -1;
	private long journalId = -1;
	private transient boolean loadFromDb = false;

	public ServerInfo() {
	}

	public ServerInfo(Properties prop) {
		publicUrl = prop.getProperty("public-url");
		admin = prop.getProperty("admin");
		postNanopubsEnabled = Boolean.parseBoolean(prop.getProperty("post-nanopubs-enabled"));
		postPeersEnabled = Boolean.parseBoolean(prop.getProperty("post-peers-enabled"));
		loadFromDb = true;
	}

	public boolean isPostNanopubsEnabled() {
		return postNanopubsEnabled;
	}

	public boolean isPostPeersEnabled() {
		return postPeersEnabled;
	}

	public String getPublicUrl() {
		return publicUrl;
	}

	public String getAdmin() {
		return admin;
	}

	public int getPageSize() {
		ensureLoaded();
		return pageSize;
	}

	public long getNextNanopubNo() {
		ensureLoaded();
		return nextNanopubNo;
	}

	public long getJournalId() {
		ensureLoaded();
		return journalId;
	}

	public String asJson() {
		ensureLoaded();
		return new Gson().toJson(this);
	}

	private void ensureLoaded() {
		if (loadFromDb) {
			NanopubDb db = NanopubDb.get();
			nextNanopubNo = db.getNextNanopubNo();
			pageSize = db.getPageSize();
			journalId = db.getJournalId();
		}
	}

}
