package ch.tkuhn.nanopub.server;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.commonjava.mimeparse.MIMEParse;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.nanopub.Nanopub;

public class Utils {

	private Utils() {}  // no instances allowed

	public static String getMimeType(HttpServletRequest req, String supported) {
		List<String> supportedList = Arrays.asList(StringUtils.split(supported, ','));
		String mimeType = supportedList.get(0);
		try {
			mimeType = MIMEParse.bestMatch(supportedList, req.getHeader("Accept"));
		} catch (Exception ex) {}
		return mimeType;
	}

	public static final IRI PROTECTED_NANOPUB = SimpleValueFactory.getInstance().createIRI("http://purl.org/nanopub/x/ProtectedNanopub");

	// TODO: Use the method from NanopubServerUtils from nanopub library once new release is out:

	public static boolean isProtectedNanopub(Nanopub np) {
		for (Statement st : np.getPubinfo()) {
			if (!st.getSubject().equals(np.getUri())) continue;
			if (!st.getPredicate().equals(RDF.TYPE)) continue;
			if (st.getObject().equals(PROTECTED_NANOPUB)) return true;
		}
		return false;
	}

	public static String urlEncode(Object o) {
		try {
			return URLEncoder.encode((o == null ? "" : o.toString()), "UTF-8");
		} catch (UnsupportedEncodingException ex) {
			throw new RuntimeException(ex);
		}
	}

}
