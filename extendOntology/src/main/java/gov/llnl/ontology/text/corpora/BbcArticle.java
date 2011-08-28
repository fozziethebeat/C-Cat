package gov.llnl.ontology.text.corpora;

import java.util.logging.Logger;
import java.util.Set;

import gov.llnl.ontology.text.Document;
import gov.llnl.ontology.text.corpora.AbstractArticle;

/**
 * This class represents an article in the BBC News website 
 * (http://news.bbc.co.uk).
 */
public class BbcArticle extends AbstractArticle implements Document {
	private static final String BBC_ATTRIBUTE_NAME = "class";
	
	private static final String BBC_ATTRIBUTE_VALUE = "story-body";

	private static final String BBC_CORPUS_NAME = "BBC";

	/** Setup the logger */
	private static final Logger LOGGER = 
		Logger.getLogger(BbcArticle.class.getName());

	public BbcArticle(String articleURL) {
		super(BBC_ATTRIBUTE_NAME, BBC_ATTRIBUTE_VALUE, BBC_CORPUS_NAME, articleURL);
	}

	public String key() {
		return getArticleUrl();
	}

	public long id() {
		return 0;
	}

	public String title() {
		return getTitle();
	}
    
	public Set<String> categories() {
		return null;
	}
}