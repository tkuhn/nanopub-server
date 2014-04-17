package ch.tkuhn.nanopub.server;

import java.io.IOException;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
		if (r.isEmpty()) {
			ServletOutputStream out = resp.getOutputStream();
			out.println("Nanopub Server");
			out.println("==============");
			out.println("");
			long c = NanopubDb.get().getNanopubCollection().count();
			out.println("Number of stored nanopubs: " + c);
			resp.setContentType("text/plain");
		} else if (artifactCode.matches("RA[A-Za-z0-9\\-_]{43}")) {
			Nanopub nanopub;
			try {
				nanopub = NanopubDb.get().getNanopub(artifactCode);
			} catch (Exception ex) {
				resp.sendError(500, "Internal error: " + ex.getMessage());
				ex.printStackTrace();
				return;
			}
			if (nanopub == null) {
				resp.sendError(404, "Nanopub not found: " + artifactCode);
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
				ex.printStackTrace();
			}
		} else if (r.matches("[A-Za-z0-9\\-_]{0,45}\\+")) {
			DBCollection coll = NanopubDb.get().getNanopubCollection();
			Pattern p = Pattern.compile(r.substring(0, r.length()-1) + ".*");
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
		} else {
			resp.sendError(400, "Invalid request: " + r);
		}
		resp.getOutputStream().close();
	}

}
