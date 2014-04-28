package ch.tkuhn.nanopub.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class NanopubServlet extends HttpServlet {

	private static final long serialVersionUID = -4542560440919522982L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		ServerRequest r = new ServerRequest(req);
		if (r.isEmpty()) {
			MainPage.show(r, resp);
		} else if (r.hasArtifactCode()) {
			NanopubPage.show(r, resp);
		} else if (r.hasListQuery()) {
			ListPage.show(r, resp);
		} else if (r.getFullRequest().equals("/style/plain.css")) {
			ResourcePage.show(r, resp, "style.css", "text/css");
		} else {
			resp.sendError(400, "Invalid request: " + r.getFullRequest());
		}
		resp.getOutputStream().close();
	}

}
