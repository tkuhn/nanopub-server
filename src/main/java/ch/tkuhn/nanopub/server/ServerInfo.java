package ch.tkuhn.nanopub.server;

import java.util.Properties;

public class ServerInfo extends org.nanopub.extra.server.ServerInfo {

	private static final long serialVersionUID = 3460590224836603269L;

	public static ServerInfo load(String serverUrl) throws ServerInfoException {
		return (ServerInfo) load(serverUrl, ServerInfo.class);
	}

	private transient boolean loadFromDb = false;

	public ServerInfo() {
	}

	public ServerInfo(Properties prop) {
		protocolVersion = NanopubServerUtils.protocolVersion;
		publicUrl = prop.getProperty("public.url");
		admin = prop.getProperty("admin");
		postNanopubsEnabled = Boolean.parseBoolean(prop.getProperty("post.nanopubs.enabled"));
		postPeersEnabled = Boolean.parseBoolean(prop.getProperty("post.peers.enabled"));
		description = "nanopub-server " + prop.getProperty("version") + ", " + prop.getProperty("build.date");
		try {
			maxNanopubTriples = Integer.parseInt(prop.getProperty("max.nanopub.triples"));
		} catch (Exception ex) {}
		try {
			maxNanopubBytes = Long.parseLong(prop.getProperty("max.nanopub.bytes"));
		} catch (Exception ex) {}
		try {
			maxNanopubs = Long.parseLong(prop.getProperty("max.nanopubs"));
		} catch (Exception ex) {}
		loadFromDb = true;
	}

	@Override
	public int getPageSize() {
		if (loadFromDb) {
			pageSize = NanopubDb.get().getJournal().getPageSize();
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
			journalId = NanopubDb.get().getJournal().getId();
		}
		return super.getJournalId();
	}

	@Override
	public String getUriPattern() {
		if (loadFromDb) {
			uriPattern = NanopubDb.get().getJournal().getUriPattern();
		}
		return super.getUriPattern();
	}

	@Override
	public String getHashPattern() {
		if (loadFromDb) {
			hashPattern = NanopubDb.get().getJournal().getHashPattern();
		}
		return super.getHashPattern();
	}

	@Override
	public String asJson() {
		if (loadFromDb) {
			Journal j = NanopubDb.get().getJournal();
			nextNanopubNo = j.getNextNanopubNo();
			pageSize = j.getPageSize();
			journalId = j.getId();
			uriPattern = j.getUriPattern();
			hashPattern = j.getHashPattern();
		}
		return super.asJson();
	}

}
