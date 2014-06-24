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
		setCanonicalLink("/" + ac);
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
			getResp().addHeader("Content-Disposition", "filename=\"" + ac + "." + format.getDefaultFileExtension() + "\"");
		}
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
			String title = "Nanopub Index";
			if (npi.isIncomplete()) title = "Part of " + title;
			printHtmlHeader(title);
			println("<h1>" + title + "</h1>");
			println("<p>[ <a href=\".\" rel=\"home\">home</a> ]</p>");
			println("<h3>This:</h3>");
			println("<table><tbody>");
			printItem(npi.getUri());
			println("</tbody></table>");
			if (npi.getAppendedIndex() != null) {
				println("<h3>Appends:</h3>");
				println("<table><tbody>");
				printItem(npi.getAppendedIndex());
				println("</tbody></table>");
			}
			if (!npi.getSubIndexes().isEmpty()) {
				println("<h3>Includes as Sub-Indexes:</h3>");
				println("<table><tbody>");
				for (URI uri : npi.getSubIndexes()) {
					printItem(uri);
				}
				println("</tbody></table>");
			}
			if (!npi.getElements().isEmpty()) {
				println("<h3>Includes as Elements:</h3>");
				println("<table><tbody>");
				for (URI uri : npi.getElements()) {
					printItem(uri);
				}
				println("</tbody></table>");
			}
			printHtmlFooter();
		} catch (MalformedNanopubException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void printItem(URI uri) throws IOException {
		String artifactCode = TrustyUriUtils.getArtifactCode(uri.toString());
		print("<tr>");
		print("<td>");
		printGetLinks(artifactCode);
		print("</td><td>");
		printShowLinks(artifactCode);
		print("</td>");
		print("<td><span class=\"code\">" + uri + "</span></td>");
		println("</tr>");
	}

}
