package ch.tkuhn.nanopub.server;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import net.trustyuri.TrustyUriUtils;

import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.openrdf.model.URI;
import org.openrdf.rio.RDFFormat;

import ch.tkuhn.nanopub.index.IndexUtils;
import ch.tkuhn.nanopub.index.NanopubIndex;

public class NanopubPage extends Page {

	public static void show(ServerRequest req, HttpServletResponse httpResp) throws IOException {
		NanopubPage obj = new NanopubPage(req, httpResp);
		obj.show();
	}

	public NanopubPage(ServerRequest req, HttpServletResponse httpResp) {
		super(req, httpResp);
	}

	public void show() throws IOException {
		Nanopub nanopub;
		String ac = getReq().getArtifactCode();
		try {
			nanopub = NanopubDb.get().getNanopub(ac);
		} catch (Exception ex) {
			getResp().sendError(500, "Internal error: " + ex.getMessage());
			ex.printStackTrace();
			return;
		}
		if (nanopub == null) {
			getResp().sendError(404, "Nanopub not found: " + ac);
			return;
		}
		boolean isIndexNanopub = IndexUtils.isIndex(nanopub);
		String ext = getReq().getExtension();
		String rf = getReq().getPresentationFormat();
		RDFFormat format = null;
		if (ext != null) {
			format = RDFFormat.forFileName("np." + ext);
			if (format == null) {
				getResp().sendError(400, "Unknown format: " + ext);
				return;
			}
		} else if (rf == null) {
			String suppFormats = "application/x-trig,text/x-nquads,application/trix";
			if (isIndexNanopub) suppFormats += ",text/html";
			String mimeType = Utils.getMimeType(getHttpReq(), suppFormats);
			if (isIndexNanopub && "text/html".equals(mimeType)) {
				showIndex(nanopub);
				return;
			}
			format = RDFFormat.forMIMEType(mimeType);
		}
		if (format == null) {
			format = RDFFormat.TRIG;
		}
		if (!format.supportsContexts()) {
			getResp().sendError(400, "Unsuitable RDF format: " + ext);
			return;
		}
		if (rf != null) {
			getResp().setContentType(rf);
		} else {
			getResp().setContentType(format.getDefaultMIMEType());
		}
		setCanonicalLink("/" + ac);
		try {
			NanopubUtils.writeToStream(nanopub, getResp().getOutputStream(), format);
		} catch (Exception ex) {
			getResp().sendError(500, "Internal error: " + ex.getMessage());
			ex.printStackTrace();
		}
	}

	private void showIndex(Nanopub np) throws IOException {
		try {
			NanopubIndex npi = IndexUtils.castToIndex(np);
			getResp().setContentType("text/html");
			printHtmlHeader("Nanopub Index");
			print("<h3>Nanopub Index</h3>");
			println("<table><tbody>");
			for (URI uri : npi.getSubIndexes()) {
				printItem(uri, true);
			}
			for (URI uri : npi.getElements()) {
				printItem(uri, false);
			}
			println("</tbody></table>");
			printHtmlFooter();
		} catch (MalformedNanopubException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void printItem(URI uri, boolean isSubIndex) throws IOException {
		String artifactCode = TrustyUriUtils.getArtifactCode(uri.toString());
		print("<tr>");
		if (isSubIndex) {
			print("<td>Includes all:</td>");
		} else {
			print("<td>Includes:</td>");
		}
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
		print("<td><span class=\"code\">" + uri + "</span></td>");
		println("</tr>");
	}

}
