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
	private ServerInfo info;

	private ServerConf() {
		conf = new Properties();
		InputStream in1 = null;
		InputStream in2 = null;
		try {
			in1 = ServerConf.class.getResourceAsStream("conf.properties");
			try {
				conf.load(in1);
			} catch (IOException ex) {
				LoggerFactory.getLogger(NanopubDb.class).error(ex.getMessage(), ex);
				System.exit(1);
			}
			in2 = ServerConf.class.getResourceAsStream("local.conf.properties");
			if (in2 != null) {
				try {
					conf.load(in2);
				} catch (IOException ex) {
					LoggerFactory.getLogger(NanopubDb.class).error(ex.getMessage(), ex);
					System.exit(1);
				}
			}
			info = new ServerInfo(conf);
		} finally {
			close(in1);
			close(in2);
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

	public boolean isPeerScanEnabled() {
		return Boolean.parseBoolean(conf.getProperty("peer-scan-enabled"));
	}

	public boolean isCollectNanopubsEnabled() {
		return Boolean.parseBoolean(conf.getProperty("collect-nanopubs-enabled"));
	}

	public boolean isCheckNanopubsOnGetEnabled() {
		return Boolean.parseBoolean(conf.getProperty("check-nanopubs-on-get"));
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

	public String[] getInitialPeers() {
		String s = conf.getProperty("initial-peers");
		if (s == null) return new String[] {};
		return s.split(" ");
	}

	public int getInitPageSize() {
		return Integer.parseInt(conf.getProperty("init-page-size"));
	}

	public int getWaitMsBeforePeerScan() {
		return Integer.parseInt(conf.getProperty("wait-ms-before-peer-scan"));
	}

	public int getWaitMsBeforeFileLoad() {
		return Integer.parseInt(conf.getProperty("wait-ms-before-file-load"));
	}

	public boolean isLogNanopubLoadingEnabled() {
		return Boolean.parseBoolean(conf.getProperty("log-nanopub-loading"));
	}

	public String getLoadDir() {
		String loadDir = conf.getProperty("load-dir");
		if (loadDir != null && loadDir.isEmpty()) loadDir = null;
		return loadDir;
	}

}
