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
import org.nanopub.extra.server.ServerInfo.ServerInfoException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.tkuhn.nanopub.server.NanopubDb.NotTrustyNanopubException;
import ch.tkuhn.nanopub.server.NanopubDb.OversizedNanopubException;

public class NanopubServlet extends HttpServlet {

	private static final long serialVersionUID = -4542560440919522982L;

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			ServerRequest r = new ServerRequest(req);
			if (r.isEmpty()) {
				MainPage.show(r, resp);
			} else if (r.hasArtifactCode()) {
				NanopubPage.show(r, resp);
			} else if (r.getRequestString().equals(NanopubListPage.PAGE_NAME)) {
				NanopubListPage.show(r, resp);
			} else if (r.getRequestString().equals(PeerListPage.PAGE_NAME)) {
				PeerListPage.show(r, resp);
			} else if (r.getRequestString().equals(PackagePage.PAGE_NAME)) {
				PackagePage.show(r, resp);
			} else if (r.getFullRequest().equals("/style/plain.css")) {
				ResourcePage.show(r, resp, "style.css", "text/css");
			} else if (r.getFullRequest().equals("/style/favicon.ico")) {
				ResourcePage.show(r, resp, "favicon.ico", "image/x-icon");
			} else {
				resp.sendError(400, "Invalid GET request: " + r.getFullRequest());
			}
		} finally {
			resp.getOutputStream().close();
			req.getInputStream().close();
		}
		check();
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			ServerRequest r = new ServerRequest(req);
			if (r.isEmpty()) {
				if (!ServerConf.getInfo().isPostNanopubsEnabled()) {
					resp.sendError(405, "Posting nanopubs is not supported by this nanopub server");
					return;
				}
				Nanopub np = null;
				try {
					np = new NanopubImpl(req.getInputStream(), Rio.getParserFormatForMIMEType(req.getContentType(), RDFFormat.TRIG));
				} catch (Exception ex) {
					resp.sendError(400, "Error reading nanopub: " + ex.getMessage());
				}
				if (np != null) {
					String code = TrustyUriUtils.getArtifactCode(np.getUri().toString());
					try {
						if (NanopubDb.get().getNanopub(code) == null) {
							NanopubDb.get().loadNanopub(np);
						}
						resp.setHeader("Location", TrustyUriUtils.getArtifactCode(np.getUri().toString()));
						resp.setStatus(201);
					} catch (NotTrustyNanopubException ex) {
						resp.sendError(400, "Nanopub is not trusty: " + ex.getMessage());
					} catch (OversizedNanopubException ex) {
						resp.sendError(400, "Nanopub is too large: " + ex.getMessage());
					} catch (Exception ex) {
						resp.sendError(500, "Error storing nanopub: " + ex.getMessage());
					}
				}
			} else if (r.getRequestString().equals(PeerListPage.PAGE_NAME)) {
				if (!ServerConf.getInfo().isPostPeersEnabled()) {
					resp.sendError(405, "Posting peers is not supported by this nanopub server");
					return;
				}
				try {
					StringWriter sw = new StringWriter();
					IOUtils.copy(req.getInputStream(), sw);
					NanopubDb.get().addPeer(sw.toString().trim());
					resp.setStatus(201);
				} catch (ServerInfoException ex) {
					resp.sendError(400, "Invalid peer URL: " + ex.getMessage());
				} catch (IOException ex) {
					resp.sendError(500, "Error adding peer: " + ex.getMessage());
				}
			} else {
				resp.sendError(400, "Invalid POST request: " + r.getFullRequest());
			}
		} finally {
			resp.getOutputStream().close();
			req.getInputStream().close();
		}
		check();
	}

	@Override
	public void init() throws ServletException {
		logger.info("Init");
		check();
	}

	private void check() {
		ScanPeers.check();
		LoadFiles.check();
	}

}
