package ch.tkuhn.nanopub.server;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class Page {

	private ServerRequest req;
	private HttpServletResponse httpResp;

	public Page(ServerRequest req, HttpServletResponse httpResp) {
		this.req = req;
		this.httpResp = httpResp;
	}

	public ServerRequest getReq() {
		return req;
	}

	public HttpServletRequest getHttpReq() {
		return req.getHttpRequest();
	}

	public HttpServletResponse getResp() {
		return httpResp;
	}

	public void println(String s) throws IOException {
		httpResp.getOutputStream().println(s);
	}

	public void print(String s) throws IOException {
		httpResp.getOutputStream().print(s);
	}

	public abstract void show() throws IOException;

}
