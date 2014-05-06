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
			long c = NanopubDb.get().getNanopubCollection().count();
			String url = ServerConf.getInfo().getPublicUrl();
			println("<p>Public URL: <span class=\"code\"><a href=\"" + url + "\">" + url + "</a></span></p>");
			println("<p>Administrator: " + escapeHtml(ServerConf.getInfo().getAdmin()) + "</p>");
			println("<p>Number of stored nanopubs: " + c + "</p>");
			println("<p><a href=\"+\">List of stored nanopubs</a></p>");
			printHtmlFooter();
		}
		getResp().setContentType(format);
	}

}
