package ch.tkuhn.nanopub.server;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.nanopub.HtmlWriter;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.Nanopub2Html;
import org.nanopub.NanopubUtils;
import org.nanopub.extra.index.IndexUtils;
import org.nanopub.extra.index.NanopubIndex;
import org.nanopub.extra.security.SignatureUtils;
import org.nanopub.trusty.TrustyNanopubUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.trustyuri.TrustyUriUtils;

public class NanopubPage extends Page {

	private Logger logger = LoggerFactory.getLogger(this.getClass());

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
			logger.error(ex.getMessage(), ex);
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
			format = Rio.getParserFormatForFileName("np." + ext).orElse(null);
			if (ext.equals("stnp")) {
				format = TrustyNanopubUtils.STNP_FORMAT;
			}
			if (format == null) {
				getResp().sendError(400, "Unknown format: " + ext);
				return;
			}
		} else if (rf == null) {
			String suppFormats = "text/html,application/trig,application/x-trig,text/x-nquads,application/trix,application/ld+json";
			if (isIndexNanopub) {
				suppFormats += ",text/html";
			} else {
				suppFormats += ",text/plain";
			}
			String mimeType = Utils.getMimeType(getHttpReq(), suppFormats);
			if (isIndexNanopub && "text/html".equals(mimeType) && rf == null) {
				// Show index-specific HTML representation when HTML is requested by content negotiation,
				// but not if ".html" ending is used (in the latter case, use the general HTML view).
				showIndex(nanopub);
				return;
			}
			if ("text/plain".equals(mimeType)) {
				rf = "text/plain";
			} else {
				format = Rio.getParserFormatForMIMEType(mimeType).orElse(null);
			}
		}
		if (format == null) {
			format = HtmlWriter.HTML_FORMAT;
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
		OutputStream out = null;
		try {
			out = getResp().getOutputStream();
			if (format == HtmlWriter.HTML_FORMAT) {
				String htmlString = Nanopub2Html.createHtmlString(nanopub, true);
				if (SignatureUtils.getSignatureElement(nanopub) != null) {
					// Move this feature to Nanopub2Html:
					htmlString = htmlString.replace("<div class=\"nanopub\">",
							"<p style=\"font-family: sans-serif;\">"
							+ "<span id=\"status\"></span>"
							+ "</p>\n"
							+ "<script type=\"text/javascript\" src=\"scripts/nanopub.js\"></script>\n"
							+ "<script>getStatus(\"status\", '" + nanopub.getUri().stringValue() + "');</script>\n"
							+ "<div class=\"nanopub\">");
				}
				PrintStream ps = new PrintStream(out);
				ps.print(htmlString);
			} else {
				NanopubUtils.writeToStream(nanopub, out, format);
			}
		} catch (Exception ex) {
			getResp().sendError(500, "Internal error: " + ex.getMessage());
			logger.error(ex.getMessage(), ex);
		} finally {
			if (out != null) out.close();
		}
	}

	private void showIndex(Nanopub np) throws IOException {
		try {
			NanopubIndex npi = IndexUtils.castToIndex(np);
			getResp().setContentType("text/html");
			String title, headerTitle;
			if (npi.getName() == null) {
				if (npi.isIncomplete()) {
					headerTitle = "Part of Unnamed Index";
					title = "<em>Part of Unnamed Index</em>";
				} else {
					headerTitle = "Unnamed Index";
					title = "<em>Unnamed Index</em>";
				}
			} else {
				String escapedName = StringEscapeUtils.escapeHtml(npi.getName());
				if (npi.isIncomplete()) {
					headerTitle = "Part of Index '" + escapedName + "'";
					title = "<em>Part of Index</em> '" + escapedName + "'";
				} else {
					headerTitle = escapedName + " (Nanopub Index)";
					title = escapedName + " <em>(Nanopub Index)</em>";
				}
			}
			printHtmlHeader(headerTitle);
			println("<h1>" + title + "</h1>");
			println("<p>[ <a href=\".\" rel=\"home\">home</a> ]</p>");
			println("<h3>This:</h3>");
			println("<table><tbody>");
			printItem(npi.getUri());
			println("</tbody></table>");
			if (!npi.isIncomplete()) {
				if (npi.getDescription() != null) {
					println("<h3>Description:</h3>");
					println("<p>");
					println(StringEscapeUtils.escapeHtml(npi.getDescription()));
					println("</p>");
				}
				if (npi.getCreationTime() != null) {
					println("<h3>Creation Time:</h3>");
					println("<p>" + SimpleDateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).format(npi.getCreationTime().getTime()) + "</p>");
				}
				if (!npi.getCreators().isEmpty()) {
					println("<h3>Creators:</h3>");
					println("<ul>");
					for (IRI uri : npi.getCreators()) {
						println("<li><a href=\"" + uri + "\" rel=\"nofollow\">" + uri + "</a></li>");
					}
					println("</ul>");
				}
			}

			List<IRI> superseding = new ArrayList<>();
			List<IRI> derivedFrom = new ArrayList<>();
			for (Statement st : np.getPubinfo()) {
				if (!st.getSubject().equals(np.getUri())) continue;
				if (!(st.getObject() instanceof IRI)) continue;
				if (st.getPredicate().equals(Nanopub.SUPERSEDES)) {
					superseding.add((IRI) st.getObject());
				} else if (st.getPredicate().stringValue().equals("http://www.w3.org/ns/prov#wasDerivedFrom")) {
					derivedFrom.add((IRI) st.getObject());
				}
			}
			if (!superseding.isEmpty()) {
				println("<h3>Supersedes:</h3>");
				println("<ul>");
				for (IRI uri : superseding) {
					println("<li><a href=\"" + uri + "\" rel=\"nofollow\">" + uri + "</a></li>");
				}
				println("</ul>");
			}
			if (!derivedFrom.isEmpty()) {
				println("<h3>Derived From:</h3>");
				println("<ul>");
				for (IRI uri : derivedFrom) {
					println("<li><a href=\"" + uri + "\" rel=\"nofollow\">" + uri + "</a></li>");
				}
				println("</ul>");
			}

			if (!npi.getSeeAlsoUris().isEmpty()) {
				println("<h3>See Also:</h3>");
				println("<ul>");
				for (IRI uri : npi.getSeeAlsoUris()) {
					println("<li><a href=\"" + uri + "\" rel=\"nofollow\">" + uri + "</a></li>");
				}
				println("</ul>");
			}
			if (npi.getAppendedIndex() != null) {
				println("<h3>Appends:</h3>");
				println("<table><tbody>");
				printItem(npi.getAppendedIndex());
				println("</tbody></table>");
			}
			if (!npi.getSubIndexes().isEmpty()) {
				println("<h3>Includes as Sub-Indexes:</h3>");
				println("<table><tbody>");
				for (IRI uri : npi.getSubIndexes()) {
					printItem(uri);
				}
				println("</tbody></table>");
			}
			if (!npi.getElements().isEmpty()) {
				println("<h3>Includes as Elements:</h3>");
				println("<table><tbody>");
				for (IRI uri : npi.getElements()) {
					printItem(uri);
				}
				println("</tbody></table>");
			}
			printHtmlFooter();
		} catch (MalformedNanopubException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void printItem(IRI uri) throws IOException {
		String artifactCode = TrustyUriUtils.getArtifactCode(uri.toString());
		print("<tr>");
		print("<td>");
		printAltLinks(artifactCode);
		print("</td>");
		print("<td><span class=\"code\">" + uri + "</span></td>");
		println("</tr>");
	}

}
