package ch.tkuhn.nanopub.server;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.commonjava.mimeparse.MIMEParse;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.openrdf.rio.RDFFormat;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;

public class NanopubServlet extends HttpServlet {

	private static final long serialVersionUID = -4542560440919522982L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		ServerRequest r = new ServerRequest(req);
		if (r.isEmpty()) {
			showMainPage(resp);
		} else if (r.hasArtifactCode()) {
			showArtifact(r, resp);
		} else if (r.hasListQuery()) {
			showList(r, resp);
		} else {
			resp.sendError(400, "Invalid request: " + r);
		}
		resp.getOutputStream().close();
	}

	private static void showMainPage(HttpServletResponse resp) throws IOException {
		ServletOutputStream out = resp.getOutputStream();
		out.println("<!DOCTYPE HTML>");
		out.println("<html><body>");
		out.println("<h1>Nanopub Server</h1>");
		long c = NanopubDb.get().getNanopubCollection().count();
		out.println("<p>Number of stored nanopubs: " + c + "</p>");
		out.println("<p><a href=\"+\">List of stored nanopubs (up to " + ServerConf.get().getMaxListSize() + ")</a></p>");
		out.println("</body></html>");
		resp.setContentType("text/html");
	}

	private static void showArtifact(ServerRequest r, HttpServletResponse resp) throws IOException {
		Nanopub nanopub;
		try {
			nanopub = NanopubDb.get().getNanopub(r.getArtifactCode());
		} catch (Exception ex) {
			resp.sendError(500, "Internal error: " + ex.getMessage());
			ex.printStackTrace();
			return;
		}
		if (nanopub == null) {
			resp.sendError(404, "Nanopub not found: " + r.getArtifactCode());
			return;
		}
		RDFFormat format = null;
		if (r.getExtension() != null) {
			format = RDFFormat.forFileName("np." + r.getExtension());
			if (format == null) {
				resp.sendError(400, "Unknown format: " + r.getExtension());
				return;
			}
		} else if (r.getPresentationFormat() == null) {
			String suppFormats = "application/x-trig,text/x-nquads,application/trix";
			format = RDFFormat.forMIMEType(getMimeType(r.getHttpRequest(), suppFormats));
		}
		if (format == null) {
			format = RDFFormat.TRIG;
		}
		if (!format.supportsContexts()) {
			resp.sendError(400, "Unsuitable RDF format: " + r.getExtension());
			return;
		}
		if (r.getPresentationFormat() != null) {
			resp.setContentType(r.getPresentationFormat());
		} else {
			resp.setContentType(format.getDefaultMIMEType());
		}
		try {
			NanopubUtils.writeToStream(nanopub, resp.getOutputStream(), format);
		} catch (Exception ex) {
			resp.sendError(500, "Internal error: " + ex.getMessage());
			ex.printStackTrace();
		}
	}

	private static void showList(ServerRequest r, HttpServletResponse resp) throws IOException {
		DBCollection coll = NanopubDb.get().getNanopubCollection();
		Pattern p = Pattern.compile(r.getListQueryRegex());
		BasicDBObject query = new BasicDBObject("_id", p);
		DBCursor cursor = coll.find(query);
		int c = 0;
		int maxListSize = ServerConf.get().getMaxListSize();
		while (cursor.hasNext()) {
			c++;
			if (c > maxListSize) {
				resp.getOutputStream().println("...");
				break;
			}
			String npUri = cursor.next().get("uri").toString();
			resp.getOutputStream().println(npUri);
		}
		resp.setContentType("text/plain");
	}

	private static String getMimeType(HttpServletRequest req, String supported) {
		List<String> supportedList = Arrays.asList(StringUtils.split(supported, ','));
		String mimeType = supportedList.get(0);
		try {
			mimeType = MIMEParse.bestMatch(supportedList, req.getHeader("Accept"));
		} catch (Exception ex) {}
		return mimeType;
	}

}
