package ch.tkuhn.nanopub.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.openrdf.rio.RDFFormat;

public class NanopubServlet extends HttpServlet {

	private static final long serialVersionUID = -4542560440919522982L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String r = req.getServletPath().substring(1);
		boolean showInPlainText = false;
		if (r.endsWith(".txt")) {
			showInPlainText = true;
			r = r.replaceFirst("\\.txt$", "");
		}
		String extension = "trig";
		String artifactCode;
		if (r.matches(".*\\.[a-z]{1,10}")) {
			extension = r.replaceFirst("^.*\\.([a-z]{1,10})$", "$1");
			artifactCode = r.replaceFirst("^(.*)\\.[a-z]{1,10}$", "$1");
		} else {
			artifactCode = r;
		}
		if (!artifactCode.matches("RA[A-Za-z0-9\\-_]{43}")) {
			resp.sendError(400, "Invalid artifact code: " + artifactCode);
			return;
		}
		Nanopub nanopub;
		try {
			nanopub = NanopubDb.get().getNanopub(artifactCode);
		} catch (Exception ex) {
			resp.sendError(500, "Internal error: " + ex.getMessage());
			return;
		}
		RDFFormat format = RDFFormat.forFileName("np." + extension);
		if (format == null) {
			resp.sendError(400, "Unknown format: " + extension);
			return;
		} else if (NanopubUtils.isUnsuitableFormat(format)) {
			resp.sendError(400, "Unsuitable RDF format: " + extension);
			return;
		}
		if (showInPlainText) {
			resp.setContentType("text/plain");
		} else {
			resp.setContentType(format.getDefaultMIMEType());
		}
		try {
			NanopubUtils.writeToStream(nanopub, resp.getOutputStream(), format);
		} catch (Exception ex) {
			resp.sendError(500, "Internal error: " + ex.getMessage());
		}
		resp.getOutputStream().close();
	}

}
