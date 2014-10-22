package ch.tkuhn.nanopub.server;

import javax.servlet.http.HttpServletRequest;

public class ServerRequest {

	private HttpServletRequest httpRequest;
	private String presentationFormat;
	private String extension;
	private String requestString;

	public ServerRequest(HttpServletRequest httpRequest) {
		this.httpRequest = httpRequest;
		init();
	}

	private void init() {
		String r = httpRequest.getServletPath().substring(1);
		if (r.endsWith(".txt")) {
			presentationFormat = "text/plain";
			r = r.replaceFirst("\\.txt$", "");
		} else if (r.endsWith(".html")) {
			presentationFormat = "text/html";
			r = r.replaceFirst("\\.html$", "");
		} else if (r.endsWith(".gz")) {
			presentationFormat = "application/x-gzip";
			r = r.replaceFirst("\\.gz$", "");
		}
		if (r.matches(".*\\.[a-z]{1,10}")) {
			extension = r.replaceFirst("^.*\\.([a-z]{1,10})$", "$1");
			requestString = r.replaceFirst("^(.*)\\.[a-z]{1,10}$", "$1");
		} else {
			requestString = r;
		}
	}

	public HttpServletRequest getHttpRequest() {
		return httpRequest;
	}

	public String getPresentationFormat() {
		return presentationFormat;
	}

	public String getExtension() {
		return extension;
	}

	public String getRequestString() {
		return requestString;
	}

	public String getFullRequest() {
		return httpRequest.getServletPath();
	}

	public boolean isEmpty() {
		return requestString.isEmpty() && presentationFormat == null;
	}

	public boolean hasArtifactCode() {
		return requestString.matches("RA[A-Za-z0-9\\-_]{43}");
	}

	public String getArtifactCode() {
		if (hasArtifactCode()) return requestString;
		return null;
	}

}
