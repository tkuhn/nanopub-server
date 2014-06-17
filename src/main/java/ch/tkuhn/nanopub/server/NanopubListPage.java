package ch.tkuhn.nanopub.server;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import net.trustyuri.TrustyUriUtils;

public class NanopubListPage extends Page {

	public static final String PAGE_NAME = "journal";

	private boolean asHtml;

	public static void show(ServerRequest req, HttpServletResponse httpResp) throws IOException {
		NanopubListPage obj = new NanopubListPage(req, httpResp);
		obj.show();
	}

	private NanopubDb db = NanopubDb.get();
	private int pageSize = db.getPageSize();
	private long lastPage = db.getCurrentPageNo();
	private long pageNo;

	public NanopubListPage(ServerRequest req, HttpServletResponse httpResp) {
		super(req, httpResp);
		String[] paramValues = req.getHttpRequest().getParameterValues("page");
		if (paramValues != null && paramValues.length > 0) {
			pageNo = Integer.parseInt(paramValues[0]);
		} else {
			pageNo = lastPage;
		}
		setCanonicalLink("/" + PAGE_NAME + "?page=" + pageNo);
		String rf = getReq().getPresentationFormat();
		if (rf == null) {
			String suppFormats = "text/plain,text/html";
			asHtml = "text/html".equals(Utils.getMimeType(getHttpReq(), suppFormats));
		} else {
			asHtml = "text/html".equals(getReq().getPresentationFormat());
		}
	}

	public void show() throws IOException {
		String pageContent = db.getPageContent(pageNo);
		printStart();
		long n = (pageNo-1) * pageSize;
		for (String uri : pageContent.split("\\n")) {
			if (uri.isEmpty()) continue;
			printElement(n, uri);
			n++;
		}
		if (asHtml && n == 0) {
			println("<tr><td>*EMPTY*</td></tr>");
		}
		if (asHtml && n % pageSize > 0 && db.getNextNanopubNo() == n) {
			println("<tr><td>*END*</td></tr>");
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
			String title = "Nanopub Server Journal Page " + pageNo + " of " + lastPage;
			printHtmlHeader(title);
			print("<h3>" + title + "</h3>");
			println("<p>[ <a href=\"" + getReq().getRequestString() + ".txt?page=" + pageNo + "\">as plain text</a> | <a href=\".\">home</a> |");
			long pr = Math.max(1, pageNo-1);
			println("<a href=\"journal.html?page=1\">&lt;&lt; first page</a> | <a href=\"journal.html?page=" + pr + "\">&lt; previous page</a> |");
			long nx = Math.min(lastPage, pageNo+1);
			println("<a href=\"journal.html?page=" + nx + "\">next page &gt;</a> | <a href=\"journal.html?page=" + lastPage + "\">last page &gt;&gt;</a> ]</p>");
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
