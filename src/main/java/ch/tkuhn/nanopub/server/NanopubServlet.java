package ch.tkuhn.nanopub.server;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openrdf.rio.RDFFormat;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;

public class NanopubServlet extends HttpServlet {

	private static final long serialVersionUID = -4542560440919522982L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String artifactCode = req.getServletPath().substring(1);
		if (!artifactCode.matches("RA[A-Za-z0-9\\-_]{43}")) {
			resp.sendError(400, "Invalid artifact code: " + artifactCode);
			return;
		}
		String nanopubString;
		try {
			BasicDBObject query = new BasicDBObject("id", artifactCode);
			DBCursor cursor = NanopubDb.get().getNanopubCollection().find(query);
			if (!cursor.hasNext()) {
				resp.sendError(404, "Nanopub not found: " + artifactCode);
				return;
			}
			nanopubString = cursor.next().get("nanopub").toString();
		} catch (Exception ex) {
			resp.sendError(500, "Internal error: " + ex.getMessage());
			return;
		}
		PrintWriter out = resp.getWriter();
		out.println(nanopubString);
		out.close();
		resp.setContentType(RDFFormat.TRIG.getDefaultMIMEType());
	}

}
