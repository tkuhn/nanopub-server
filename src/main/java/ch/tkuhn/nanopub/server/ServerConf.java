package ch.tkuhn.nanopub.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ServerConf {

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	private static ServerConf obj = new ServerConf();

	public static ServerConf get() {
		return obj;
	}

	public static ServerInfo getInfo() {
		return obj.info;
	}

	private Properties conf;
	private String uriPattern;
	private String hashPattern;
	private ServerInfo info;
	private String[] postUrls;

	private ServerConf() {
		conf = new Properties();
		loadProperties("conf.properties");
		loadProperties("local.conf.properties");
		info = new ServerInfo(conf);
	}

	private void loadProperties(String fileName) {
		InputStream in = null;
		try {
			in = ServerConf.class.getResourceAsStream(fileName);
			Properties tempProp = new Properties();
			try {
				tempProp.load(in);
			} catch (IOException ex) {
				LoggerFactory.getLogger(NanopubDb.class).error(ex.getMessage(), ex);
				System.exit(1);
			}
			for (Object k : tempProp.keySet()) {
				// This is for backward compatibility, as we used to use hyphens instead of
				// periods in property keys:
				conf.setProperty(k.toString().replace('-', '.'), tempProp.getProperty(k.toString()));
			}
		} finally {
			close(in);
		}
	}

	private void close(InputStream st) {
		if (st == null) return;
		try {
			st.close();
		} catch (IOException ex) {
			logger.error(ex.getMessage(), ex);
		}
	}

	public boolean isRunAsLocalServerEnabled() {
		return Boolean.parseBoolean(conf.getProperty("run.as.local.server"));
	}

	public boolean isPeerScanEnabled() {
		return Boolean.parseBoolean(conf.getProperty("peer.scan.enabled"));
	}

	public boolean isCollectNanopubsEnabled() {
		return Boolean.parseBoolean(conf.getProperty("collect.nanopubs.enabled"));
	}

	public boolean isCheckNanopubsOnGetEnabled() {
		return Boolean.parseBoolean(conf.getProperty("check.nanopubs.on.get"));
	}

	public String getMongoDbHost() {
		return conf.getProperty("mongodb.host");
	}

	public int getMongoDbPort() {
		return Integer.parseInt(conf.getProperty("mongodb.port"));
	}

	public String getMongoDbName() {
		return conf.getProperty("mongodb.dbname");
	}

	public String getMongoDbUsername() {
		String username = conf.getProperty("mongodb.username");
		if (username == null || username.isEmpty()) return null;
		return username;
	}

	public String getMongoDbPassword() {
		return conf.getProperty("mongodb.password");
	}

	public String[] getInitialPeers() {
		String s = conf.getProperty("initial.peers");
		if (s == null) return new String[] {};
		return s.split(" ");
	}

	public int getInitPageSize() {
		return Integer.parseInt(conf.getProperty("init.page.size"));
	}

	public int getWaitMsBeforePeerScan() {
		return Integer.parseInt(conf.getProperty("wait.ms.before.peer.scan"));
	}

	public int getWaitMsBeforeFileLoad() {
		return Integer.parseInt(conf.getProperty("wait.ms.before.file.load"));
	}

	public boolean isLogNanopubLoadingEnabled() {
		return Boolean.parseBoolean(conf.getProperty("log.nanopub.loading"));
	}

	public String getLoadDir() {
		String loadDir = conf.getProperty("load.dir");
		if (loadDir != null && loadDir.isEmpty()) loadDir = null;
		return loadDir;
	}

	public String getUriPattern() {
		if (uriPattern == null) {
			uriPattern = conf.getProperty("uri.pattern").replaceAll("\\s+", " ").trim();
		}
		return uriPattern;
	}

	public String getHashPattern() {
		if (hashPattern == null) {
			hashPattern = conf.getProperty("hash.pattern").replaceAll("\\s+", " ").trim();
		}
		return hashPattern;
	}

	public String[] getPostUrls() {
		if (postUrls == null) {
			postUrls = conf.getProperty("post.new.nanopubs.to").trim().split(" ");
		}
		return postUrls;
	}
}
