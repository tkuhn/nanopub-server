package ch.tkuhn.nanopub.server;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

public class PackagePage extends Page {

	public static final String PAGE_NAME = "package";

	public static void show(ServerRequest req, HttpServletResponse httpResp) throws IOException {
		PackagePage obj = new PackagePage(req, httpResp);
		obj.show();
	}

	private long pageNo = -1;

	public PackagePage(ServerRequest req, HttpServletResponse httpResp) throws IOException {
		super(req, httpResp);
		if (req.getExtension() != null && !req.getExtension().equals("trig")) {
			getResp().sendError(400, "Invalid extension: " + req.getExtension());
			return;
		}
		NanopubDb db = NanopubDb.get();
		synchronized(db) {
			String[] paramValues = req.getHttpRequest().getParameterValues("page");
			if (paramValues != null && paramValues.length > 0) {
				pageNo = Integer.parseInt(paramValues[0]);
			}
		}
	}

	public void show() throws IOException {
		try {
			boolean gzipped;
			String suppFormats = "application/x-gzip,application/trig,text/plain";
			String mimeType = Utils.getMimeType(getHttpReq(), suppFormats);
			if ("application/x-gzip".equals(getReq().getPresentationFormat()) || "application/x-gzip".equals(mimeType)) {
				getResp().setContentType("application/x-gzip");
				getResp().addHeader("Content-Disposition", "attachment; filename=\"package" + pageNo + ".trig.gz\"");
				gzipped = true;
			} else {
				getResp().setContentType("application/trig");
				getResp().addHeader("Content-Disposition", "attachment; filename=\"package" + pageNo + ".trig\"");
				gzipped = false;
			}
			NanopubDb.get().writePackageToStream(pageNo, gzipped, getResp().getOutputStream());
		} catch (IllegalArgumentException ex) {
			getResp().sendError(400, "Invalid argument: " + ex.getMessage());
		}
	}

}
