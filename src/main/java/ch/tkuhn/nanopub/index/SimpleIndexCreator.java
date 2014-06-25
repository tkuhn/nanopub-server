package ch.tkuhn.nanopub.index;

import java.util.ArrayList;
import java.util.List;

import org.nanopub.NanopubCreator;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.DC;

public abstract class SimpleIndexCreator extends NanopubIndexCreator {

	private String baseUri = "http://tkuhn.ch/nanopub-server/index/";
	private String title;
	private String description;
	private List<String> authors = new ArrayList<>();
	private List<String> creators = new ArrayList<>();

	public SimpleIndexCreator() {
	}

	public void setBaseUri(String baseUri) {
		this.baseUri = baseUri;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void addAuthor(String authorUriOrOrcid) {
		authors.add(authorUriOrOrcid);
	}

	public void addCreator(String creatorUriOrOrcid) {
		creators.add(creatorUriOrOrcid);
	}

	@Override
	public String getBaseUri() {
		return baseUri;
	}

	@Override
	public void enrichIncompleteIndex(NanopubCreator npCreator) {
		npCreator.addPubinfoStatement(DC.TITLE, new LiteralImpl(title));
		for (String author : authors) {
			if (author.indexOf("://") > 0) {
				npCreator.addAuthor(new URIImpl(author));
			} else if (author.matches("[0-9]{4}-[0-9]{4}-[0-9]{4}-[0-9]{4}")) {
				npCreator.addAuthor(author);
			} else {
				throw new IllegalArgumentException("Author has to be URI or ORCID: " + author);
			}
		}
		for (String creator : creators) {
			if (creator.indexOf("://") > 0) {
				npCreator.addCreator(new URIImpl(creator));
			} else if (creator.matches("[0-9]{4}-[0-9]{4}-[0-9]{4}-[0-9]{4}")) {
				npCreator.addCreator(creator);
			} else {
				throw new IllegalArgumentException("Author has to be URI or ORCID: " + creator);
			}
		}
	}

	@Override
	public void enrichCompleteIndex(NanopubCreator npCreator) {
		enrichIncompleteIndex(npCreator);
		npCreator.addPubinfoStatement(DC.DESCRIPTION, new LiteralImpl(description));
	}

}
