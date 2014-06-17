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
			println("<p>[ ");
			println("<a href=\"" + getReq().getRequestString() + ".txt?page=" + pageNo + "\" rel=\"alternate\" type=\"text/plain\">as plain text</a> | ");
			println("<a href=\".\" rel=\"home\">home</a> |");
			println("<a href=\"journal.html?page=1\" rel=\"start\">&lt;&lt; first page</a> | ");
			if (pageNo == 1) {
				println("&lt; previous page | ");
			} else {
				println("<a href=\"journal.html?page=" + (pageNo-1) + "\" rel=\"prev\">&lt; previous page</a> |");
			}
			if (pageNo == lastPage) {
				println("next page &gt; | ");
			} else {
				println("<a href=\"journal.html?page=" + (pageNo+1) + "\" rel=\"next\">next page &gt;</a> | ");
			}
			println("<a href=\"journal.html?page=" + lastPage + "\">last page &gt;&gt;</a> ");
			println("]</p>");
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
			print("<a href=\"" + artifactCode + ".trig\" type=\"application/x-trig\">trig</a>,");
			print("<a href=\"" + artifactCode + ".nq\" type=\"text/x-nquads\">nq</a>,");
			print("<a href=\"" + artifactCode + ".xml\" type=\"application/trix\">xml</a>)");
			print("</td>");
			print("<td>");
			print("<a href=\"" + artifactCode + ".txt\" type=\"text/plain\">show</a> (");
			print("<a href=\"" + artifactCode + ".trig.txt\" type=\"text/plain\">trig</a>,");
			print("<a href=\"" + artifactCode + ".nq.txt\" type=\"text/plain\">nq</a>,");
			print("<a href=\"" + artifactCode + ".xml.txt\" type=\"text/plain\">xml</a>)");
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
