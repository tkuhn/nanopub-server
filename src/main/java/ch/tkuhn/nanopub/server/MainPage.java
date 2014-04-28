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
		printHtmlHeader();
		println("<h1>Nanopub Server</h1>");
		long c = NanopubDb.get().getNanopubCollection().count();
		println("<p>Number of stored nanopubs: " + c + "</p>");
		println("<p><a href=\"+\">List of stored nanopubs (up to " + ServerConf.get().getMaxListSize() + ")</a></p>");
		printHtmlFooter();
		getResp().setContentType("text/html");
	}

}
