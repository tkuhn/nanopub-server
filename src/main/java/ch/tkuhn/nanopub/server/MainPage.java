package ch.tkuhn.nanopub.server;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

public class MainPage extends Page {

	public static void show(ServerRequest req, HttpServletResponse httpResp) throws IOException {
		MainPage obj = new MainPage(req, httpResp);
		obj.show();
	}

	public MainPage(ServerRequest req, HttpServletResponse httpResp) {
		super(req, httpResp);
	}

	public void show() throws IOException {
		String format;
		String ext = getReq().getExtension();
		if ("json".equals(ext)) {
			format = "application/json";
		} else if (ext == null || "html".equals(ext)) {
			String suppFormats = "application/json,text/html";
			format = Utils.getMimeType(getHttpReq(), suppFormats);
		} else {
			getResp().sendError(400, "Invalid request: " + getReq().getFullRequest());
			return;
		}
		if ("application/json".equals(format)) {
			println(ServerConf.getInfo().asJson());
		} else {
			printHtmlHeader("Nanopub Server");
			println("<h1>Nanopub Server</h1>");
			String url = ServerConf.getInfo().getPublicUrl();
			if (url == null || url.isEmpty()) {
				url = "<em>(unknown)</em>";
			} else {
				url = "<a href=\"" + url + "\">" + url + "</a>";
			}
			println("<p>Public URL: <span class=\"code\">" + url + "</span></p>");
			String admin = ServerConf.getInfo().getAdmin();
			if (admin == null || admin.isEmpty()) {
				admin = "<em>(unknown)</em>";
			} else {
				admin = escapeHtml(admin);
			}
			println("<p>Administrator: " + admin + "</p>");
			println("<p>Content:");
			println("<ul>");
			long npc = NanopubDb.get().getNanopubCollection().count();
			println("<li><a href=\"+.html\">Nanopubs: " + npc + "</a></li>");
			long peerc = NanopubDb.get().getPeerCollection().count();
			println("<li><a href=\"peers.html\">Peers: " + peerc + "</a></li>");
			println("</ul>");
			println("<p>Actions:");
			println("<ul>");
			ServerInfo i = ServerConf.getInfo();
			println("<li>Post nanopubs: <em>" + (i.isPostNanopubsEnabled() ? "" : "not") + " supported</em></li>");
			println("<li>Post peers: <em>" + (i.isPostPeersEnabled() ? "" : "not") + " supported</em></li>");
			println("</ul>");
			println("<p>[ <a href=\".json\">json</a> ]</p>");
			printHtmlFooter();
		}
		getResp().setContentType(format);
	}

}
