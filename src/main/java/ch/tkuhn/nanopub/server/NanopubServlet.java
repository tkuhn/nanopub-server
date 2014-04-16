package ch.tkuhn.nanopub.server;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openrdf.rio.RDFFormat;

import com.mongodb.BasicDBObject;

public class NanopubServlet extends HttpServlet {

	private static final long serialVersionUID = -4542560440919522982L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String artifactCode = req.getServletPath().substring(1);
		BasicDBObject query = new BasicDBObject("id", artifactCode);
		String nanopubString = NanopubDb.get().getNanopubCollection().find(query).next().get("nanopub").toString();
		PrintWriter out = resp.getWriter();
		out.println(nanopubString);
		out.close();
		resp.setContentType(RDFFormat.TRIG.getDefaultMIMEType());
	}

}
