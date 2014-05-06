package ch.tkuhn.nanopub.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


public class ServerConf {

	private static ServerConf obj = new ServerConf();

	public static ServerConf get() {
		return obj;
	}

	private Properties conf;

	private ServerConf() {
		conf = new Properties();
		InputStream in = ServerConf.class.getResourceAsStream("conf.properties");
		try {
			conf.load(in);
		} catch (IOException ex) {
			ex.printStackTrace();
			System.exit(1);
		}
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

	public int getMaxListSize() {
		return Integer.parseInt(conf.getProperty("maxlistsize"));
	}

	public boolean isPushEnabled() {
		return Boolean.parseBoolean(conf.getProperty("push-enabled"));
	}

}
