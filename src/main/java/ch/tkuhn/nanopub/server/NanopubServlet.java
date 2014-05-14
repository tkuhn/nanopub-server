package ch.tkuhn.nanopub.server;

import java.io.IOException;
import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.trustyuri.TrustyUriUtils;

import org.apache.commons.io.IOUtils;
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
			NanopubListPage.show(r, resp);
		} else if (r.getRequestString().equals("peers")) {
			PeerListPage.show(r, resp);
		} else if (r.getFullRequest().equals("/style/plain.css")) {
			ResourcePage.show(r, resp, "style.css", "text/css");
		} else if (r.getFullRequest().equals("/style/favicon.ico")) {
			ResourcePage.show(r, resp, "favicon.ico", "image/x-icon");
		} else {
			resp.sendError(400, "Invalid GET request: " + r.getFullRequest());
		}
		resp.getOutputStream().close();
		check();
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		ServerRequest r = new ServerRequest(req);
		if (r.isEmpty()) {
			if (!ServerConf.getInfo().isPostNanopubsEnabled()) {
				resp.sendError(405, "Posting nanopubs is not supported by this nanopub server");
				return;
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
		} else if (r.getRequestString().equals("peers")) {
			if (!ServerConf.getInfo().isPostPeersEnabled()) {
				resp.sendError(405, "Posting peers is not supported by this nanopub server");
				return;
			}
			try {
				StringWriter sw = new StringWriter();
				IOUtils.copy(req.getInputStream(), sw);
				NanopubDb.get().addPeer(sw.toString().trim());
			} catch (Exception ex) {
				resp.sendError(500, "Error adding peer: " + ex.getMessage());
			}
		} else {
			resp.sendError(400, "Invalid POST request: " + r.getFullRequest());
		}
		check();
	}

	private void check() {
		ScanPeers.check();
	}

}
