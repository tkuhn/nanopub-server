package ch.tkuhn.nanopub.server;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;

public abstract class Page {

	private ServerRequest req;
	private HttpServletResponse httpResp;

	public Page(ServerRequest req, HttpServletResponse httpResp) {
		this.req = req;
		this.httpResp = httpResp;
		httpResp.setCharacterEncoding("UTF-8");
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

	public void printHtmlHeader(String title) throws IOException {
		println("<!DOCTYPE HTML>");
		println("<html><head>");
		println("<title>" + title + "</title>");
		println("<meta charset=\"utf-8\"/>");
		println("<script type=\"text/javascript\" src=\"scripts/nanopub.js\"></script>");
		println("<link rel=\"stylesheet\" href=\"style/plain.css\" type=\"text/css\" media=\"screen\" title=\"Stylesheet\" />");
		// TODO favicon.ico is currently broken:
		//println("<link rel=\"icon\" href=\"style/favicon.ico\" type=\"image/x-icon\" />");
		println("</head><body>");
	}

	public void printHtmlFooter() throws IOException {
		println("</body></html>");
	}

	public void printAltLinks(String artifactCode) throws IOException {
		print("<a href=\"" + artifactCode + "\">get</a> <span class=\"small\">(");
		print("<a href=\"" + artifactCode + ".trig\" type=\"application/x-trig\">trig</a>, ");
		print("<a href=\"" + artifactCode + ".nq\" type=\"text/x-nquads\">nq</a>, ");
		print("<a href=\"" + artifactCode + ".xml\" type=\"application/trix\">xml</a>, ");
		print("<a href=\"" + artifactCode + ".jsonld\" type=\"application/ld+json\">jsonld</a>, ");
		print("<a href=\"" + artifactCode + ".trig.txt\" type=\"text/plain\">trig.txt</a>, ");
		print("<a href=\"" + artifactCode + ".nq.txt\" type=\"text/plain\">nq.txt</a>, ");
		print("<a href=\"" + artifactCode + ".xml.txt\" type=\"text/plain\">xml.txt</a>, ");
		print("<a href=\"" + artifactCode + ".jsonld.txt\" type=\"text/plain\">jsonld.txt</a>");
		print(")</span>");
	}

	public String escapeHtml(String text) {
		return StringEscapeUtils.escapeHtml(text);
	}

	public void setCanonicalLink(String url) {
		httpResp.addHeader("Link", "<" + url + ">; rel=\"canonical\"");
	}

}
