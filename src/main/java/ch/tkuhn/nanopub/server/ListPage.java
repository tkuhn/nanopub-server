package ch.tkuhn.nanopub.server;

import java.io.IOException;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import net.trustyuri.TrustyUriUtils;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;

public class ListPage extends Page {

	private boolean asHtml;

	public static void show(ServerRequest req, HttpServletResponse httpResp) throws IOException {
		ListPage obj = new ListPage(req, httpResp);
		obj.show();
	}

	public ListPage(ServerRequest req, HttpServletResponse httpResp) {
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
		DBCollection coll = NanopubDb.get().getNanopubCollection();
		Pattern p = Pattern.compile(getReq().getListQueryRegex());
		BasicDBObject query = new BasicDBObject("_id", p);
		DBCursor cursor = coll.find(query);
		int c = 0;
		int maxListSize = ServerConf.getInfo().getMaxListSize();
		printStart();
		boolean hasContinuation = false;
		while (cursor.hasNext()) {
			c++;
			if (c > maxListSize) {
				hasContinuation = true;
				break;
			}
			printElement(cursor.next().get("uri").toString());
		}
		if (c == 0 && asHtml) {
			println("<p><em>(no nanopub with artifact code starting with '" + getReq().getListQuerySequence() + "')</em></p>");
		}
		if (hasContinuation) {
			printContinuation();
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
			String seq = getReq().getListQuerySequence();
			printHtmlHeader("Nanopub Server: List of nanopubs " + seq);
			print("<h3>List of stored nanopubs");
			if (seq.length() > 0) {
				print(" (with artifact code starting with '" + seq + "')");
			}
			println("</h3>");
			println("<p>[ <a href=\"" + getReq().getRequestString() + ".txt\">as plain text</a> | <a href=\".\">home</a> ]</p>");
			println("<table><tbody>");
		}
	}

	private void printElement(String npUri) throws IOException {
		if (asHtml) {
			print("<tr>");
			print("<td>");
			print("<a href=\"" + TrustyUriUtils.getArtifactCode(npUri) + "\">get</a> (");
			print("<a href=\"" + TrustyUriUtils.getArtifactCode(npUri) + ".trig\">trig</a>,");
			print("<a href=\"" + TrustyUriUtils.getArtifactCode(npUri) + ".nq\">nq</a>,");
			print("<a href=\"" + TrustyUriUtils.getArtifactCode(npUri) + ".xml\">xml</a>)");
			print("</td>");
			print("<td>");
			print("<a href=\"" + TrustyUriUtils.getArtifactCode(npUri) + ".txt\">show</a> (");
			print("<a href=\"" + TrustyUriUtils.getArtifactCode(npUri) + ".trig.txt\">trig</a>,");
			print("<a href=\"" + TrustyUriUtils.getArtifactCode(npUri) + ".nq.txt\">nq</a>,");
			print("<a href=\"" + TrustyUriUtils.getArtifactCode(npUri) + ".xml.txt\">xml</a>)");
			print("</td>");
			print("<td><span class=\"code\">" + npUri + "</span></td>");
			println("</tr>");
		} else {
			println(npUri);
		}
	}

	private void printContinuation() throws IOException {
		if (asHtml) {
			println("<p><em>... and more:</em> ");
			for (char ch : "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_".toCharArray()) {
				println("<span class=\"code\"><a href=\"" + getReq().getListQuerySequence() + ch + "+\">" + ch + "</a></span>");
			}
			println("</p>");
		} else {
			println("...");
		}
	}

	private void printEnd() throws IOException {
		if (asHtml) {
			println("</tbody></table>");
			printHtmlFooter();
		}
	}

}
