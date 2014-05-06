package ch.tkuhn.nanopub.server;

import java.util.Properties;

import com.google.gson.Gson;

public class ServerInfo {

	private String publicUrl;
	private String admin;
	private boolean postNanopubsEnabled;
	private int maxListSize = 1000;

	public ServerInfo() {
	}

	public ServerInfo(Properties prop) {
		publicUrl = prop.getProperty("public-url");
		admin = prop.getProperty("admin");
		postNanopubsEnabled = Boolean.parseBoolean(prop.getProperty("post-nanopubs-enabled"));
		maxListSize = Integer.parseInt(prop.getProperty("maxlistsize"));
	}

	public boolean isPostNanopubsEnabled() {
		return postNanopubsEnabled;
	}

	public int getMaxListSize() {
		return maxListSize;
	}

	public String getPublicUrl() {
		return publicUrl;
	}

	public String getAdmin() {
		return admin;
	}

	public String asJson() {
		return new Gson().toJson(this);
	}

}
