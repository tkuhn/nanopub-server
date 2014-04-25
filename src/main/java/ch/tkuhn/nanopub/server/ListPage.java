package ch.tkuhn.nanopub.server;

import java.io.IOException;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;

public class ListPage extends Page {

	public static void show(ServerRequest req, HttpServletResponse httpResp) throws IOException {
		ListPage obj = new ListPage(req, httpResp);
		obj.show();
	}

	public ListPage(ServerRequest req, HttpServletResponse httpResp) {
		super(req, httpResp);
	}

	public void show() throws IOException {
		DBCollection coll = NanopubDb.get().getNanopubCollection();
		Pattern p = Pattern.compile(getReq().getListQueryRegex());
		BasicDBObject query = new BasicDBObject("_id", p);
		DBCursor cursor = coll.find(query);
		int c = 0;
		int maxListSize = ServerConf.get().getMaxListSize();
		while (cursor.hasNext()) {
			c++;
			if (c > maxListSize) {
				println("...");
				break;
			}
			String npUri = cursor.next().get("uri").toString();
			println(npUri);
		}
		getResp().setContentType("text/plain");
	}

}
