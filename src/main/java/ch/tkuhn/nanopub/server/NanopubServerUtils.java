package ch.tkuhn.nanopub.server;

public class NanopubServerUtils extends org.nanopub.extra.server.NanopubServerUtils {

	// Version numbers have the form MAJOR.MINOR (for example, 0.12 is a newer version than 0.9!)

	public static final String protocolVersion = "0.6";
	public static final int protocolVersionValue = getVersionValue(protocolVersion);

	public static final String journalVersion = "0.3";
	public static final int journalVersionValue = getVersionValue(journalVersion);

}
