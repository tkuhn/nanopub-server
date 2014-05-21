package ch.tkuhn.nanopub.server;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import net.trustyuri.TrustyUriUtils;

public class NanopubListPage extends Page {

	private boolean asHtml;

	public static void show(ServerRequest req, HttpServletResponse httpResp) throws IOException {
		NanopubListPage obj = new NanopubListPage(req, httpResp);
		obj.show();
	}

	public NanopubListPage(ServerRequest req, HttpServletResponse httpResp) {
		super(req, httpResp);
		String rf = getReq().getPresentationFormat();
		if (rf == null) {
			String suppFormats = "text/plain,text/html";
			asHtml = "text/html".equals(Utils.getMimeType(getHttpReq(), suppFormats));
		} else {
			asHtml = "text/html".equals(getReq().getPresentationFormat());
		}
	}

	public void show() throws IOException {
		NanopubDb db = NanopubDb.get();
		String pageContent = db.getCurrentPageContent();
		printStart();
		long n = db.getCurrentPageNo() * db.getPageSize();
		for (String uri : pageContent.split("\\n")) {
			printElement(n, uri);
			n++;
		}
		printEnd();
		if (asHtml) {
			getResp().setContentType("text/html");
		} else {
			getResp().setContentType("text/plain");
		}
	}

	private void printStart() throws IOException {
		if (asHtml) {
			long pageNo = NanopubDb.get().getCurrentPageNo();
			printHtmlHeader("Nanopub Server: Nanopub Journal Page " + pageNo);
			print("<h3>Nanopub Journal Page " + pageNo + "</h3>");
			println("<p>[ <a href=\"" + getReq().getRequestString() + ".txt\">as plain text</a> | <a href=\".\">home</a> ]</p>");
			println("<table><tbody>");
		}
	}

	private void printElement(long n, String npUri) throws IOException {
		String artifactCode = TrustyUriUtils.getArtifactCode(npUri);
		if (asHtml) {
			print("<tr>");
			print("<td>" + n + "</td>");
			print("<td>");
			print("<a href=\"" + artifactCode + "\">get</a> (");
			print("<a href=\"" + artifactCode + ".trig\">trig</a>,");
			print("<a href=\"" + artifactCode + ".nq\">nq</a>,");
			print("<a href=\"" + artifactCode + ".xml\">xml</a>)");
			print("</td>");
			print("<td>");
			print("<a href=\"" + artifactCode + ".txt\">show</a> (");
			print("<a href=\"" + artifactCode + ".trig.txt\">trig</a>,");
			print("<a href=\"" + artifactCode + ".nq.txt\">nq</a>,");
			print("<a href=\"" + artifactCode + ".xml.txt\">xml</a>)");
			print("</td>");
			print("<td><span class=\"code\">" + npUri + "</span></td>");
			println("</tr>");
		} else {
			println(npUri);
		}
	}

	private void printEnd() throws IOException {
		if (asHtml) {
			println("</tbody></table>");
			printHtmlFooter();
		}
	}

}
