package ch.tkuhn.nanopub.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import com.google.gson.Gson;

public class ServerInfo extends org.nanopub.extra.server.ServerInfo {

	public static ServerInfo load(String serverUrl) throws IOException {
		HttpGet get = new HttpGet(serverUrl);
		get.setHeader("Content-Type", "application/json");
	    InputStream in = HttpClientBuilder.create().build().execute(get).getEntity().getContent();
		return new Gson().fromJson(new InputStreamReader(in), ServerInfo.class);
	}

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

	@Override
	public int getPageSize() {
		if (loadFromDb) {
			pageSize = NanopubDb.get().getPageSize();
		}
		return super.getPageSize();
	}

	@Override
	public long getNextNanopubNo() {
		if (loadFromDb) {
			nextNanopubNo = NanopubDb.get().getNextNanopubNo();
		}
		return super.getNextNanopubNo();
	}

	@Override
	public long getJournalId() {
		if (loadFromDb) {
			journalId = NanopubDb.get().getJournalId();
		}
		return super.getJournalId();
	}

	@Override
	public String asJson() {
		if (loadFromDb) {
			NanopubDb db = NanopubDb.get();
			nextNanopubNo = db.getNextNanopubNo();
			pageSize = db.getPageSize();
			journalId = db.getJournalId();
		}
		return super.asJson();
	}

}
