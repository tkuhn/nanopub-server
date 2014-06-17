package ch.tkuhn.nanopub.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.commonjava.mimeparse.MIMEParse;

public class Utils {

	private Utils() {}  // no instances allowed

	public static List<String> loadPeerList(ServerInfo si) throws IOException {
		return loadList(si.getPublicUrl() + PeerListPage.PAGE_NAME);
	}

	public static List<String> loadNanopubUriList(ServerInfo si, String artifactCodeStart) throws IOException {
		return loadList(si.getPublicUrl() + artifactCodeStart + "+");
	}

	public static List<String> loadList(String url) throws IOException {
		List<String> list = new ArrayList<String>();
		HttpGet get = new HttpGet(url);
		get.setHeader("Content-Type", "text/plain");
		InputStream in = HttpClientBuilder.create().build().execute(get).getEntity().getContent();
	    BufferedReader r = new BufferedReader(new InputStreamReader(in));
	    String line = null;
	    while ((line = r.readLine()) != null) {
	    	list.add(line.trim());
	    }
	    r.close();
		return list;
	}

	public static String getMimeType(HttpServletRequest req, String supported) {
		List<String> supportedList = Arrays.asList(StringUtils.split(supported, ','));
		String mimeType = supportedList.get(0);
		try {
			mimeType = MIMEParse.bestMatch(supportedList, req.getHeader("Accept"));
		} catch (Exception ex) {}
		return mimeType;
	}

	public static final String base64Alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";

}
