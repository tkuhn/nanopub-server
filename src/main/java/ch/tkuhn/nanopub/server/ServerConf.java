package ch.tkuhn.nanopub.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


public class ServerConf {

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
		InputStream in = ServerConf.class.getResourceAsStream("conf.properties");
		try {
			conf.load(in);
		} catch (IOException ex) {
			ex.printStackTrace();
			System.exit(1);
		}
		info = new ServerInfo(conf);
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

	public String[] getBootstrapPeers() {
		String s = conf.getProperty("bootstrap-peers");
		if (s == null) return new String[] {};
		return s.split(" ");
	}

}
