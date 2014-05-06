package ch.tkuhn.nanopub.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.trustyuri.TrustyUriUtils;

import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.openrdf.rio.RDFFormat;

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
			resp.sendError(400, "Invalid GET request: " + r.getFullRequest());
		}
		resp.getOutputStream().close();
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (!ServerConf.get().isPushEnabled()) {
			super.doPost(req, resp);
			return;
		}
		ServerRequest r = new ServerRequest(req);
		if (!r.isEmpty()) {
			resp.sendError(400, "Invalid POST request: " + r.getFullRequest());
		}
		Nanopub np = null;
		try {
			np = new NanopubImpl(req.getInputStream(), RDFFormat.forMIMEType(req.getContentType(), RDFFormat.TRIG));
		} catch (Exception ex) {
			resp.sendError(400, "Error reading nanopub: " + ex.getMessage());
		}
		if (np != null) {
			String code = TrustyUriUtils.getArtifactCode(np.getUri().toString());
			try {
				if (NanopubDb.get().getNanopub(code) == null) {
					NanopubDb.get().loadNanopub(np);
				}
			} catch (Exception ex) {
				resp.sendError(500, "Error storing nanopub: " + ex.getMessage());
			}
		}
	}

}
