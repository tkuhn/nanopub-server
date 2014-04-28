package ch.tkuhn.nanopub.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import javax.servlet.http.HttpServletResponse;

public class ResourcePage extends Page {

	public static void show(ServerRequest req, HttpServletResponse httpResp, String resourceName,
			String resourceType) throws IOException {
		ResourcePage obj = new ResourcePage(req, httpResp, resourceName, resourceType);
		obj.show();
	}

	private String resourceName, resourceType;

	public ResourcePage(ServerRequest req, HttpServletResponse httpResp, String resourceName,
			String resourceType) {
		super(req, httpResp);
		this.resourceName = resourceName;
		this.resourceType = resourceType;
	}

	public void show() throws IOException {
		InputStream rs = NanopubServlet.class.getResourceAsStream(resourceName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(rs));
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(getResp().getOutputStream()));
		String line;
		while ((line = reader.readLine()) != null) {
			writer.write(line + "\n");
		}
		reader.close();
		writer.close();
		getResp().setContentType(resourceType);
	}

}
