package ch.tkuhn.nanopub.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

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
		getResp().setContentType(resourceType);
		InputStream in = null;
		OutputStream out = null;
		try {
			in = NanopubServlet.class.getResourceAsStream(resourceName);
			out = getResp().getOutputStream();
			IOUtils.copy(in, out);
		} finally {
			if (in != null) in.close();
			if (out != null) out.close();
		}
	}

}
