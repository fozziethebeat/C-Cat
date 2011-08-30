package gov.llnl.ontology.text.corpora.rss;

import gov.llnl.ontology.text.corpora.Article;
import gov.llnl.ontology.text.corpora.ArticleFactory;
import gov.llnl.ontology.text.corpora.BbcArticleFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class NewsHandler extends DefaultHandler {

	/** 
	 *  Setup the logger 
	 */
	private static final Logger LOGGER = 
		Logger.getLogger(NewsHandler.class.getName());

	private static final String ITEM_TAG_NAME = "item";

	private static final String DATE_TAG_NAME = "pubDate";
	private static final String DESCRIPTION_TAG_NAME = "description";
	private static final String LINK_TAG_NAME = "link";
	private static final String TITLE_TAG_NAME = "title";

	private Article currentArticle;
	private ArticleFactory articleFactory;
	private ArrayList<Article> newsArticles = null;

	private boolean isItem = false;

	private boolean isDate = false;
	private boolean isDescription = false;
	private boolean isLink = false;
	private boolean isTitle = false;

	/* Vars that track the current article item. */	
	private String currentArticleUrl;
	private String currentTitle;
	private String currentDescription;
	private Date currentDate;
	

	public NewsHandler(ArticleFactory factory) {
		super();
		newsArticles = new ArrayList();
		articleFactory = factory;
	}

	public void startElement(String uri,
	                         String localName,
	                         String qName,
	                         Attributes attributes) throws SAXException {

		if (qName.equals(ITEM_TAG_NAME)) {
			isItem = true;
		}
		
		if (isItem && qName.equals(DATE_TAG_NAME)) {
			isDate = true;
			return;
		}

		if (isItem && qName.equals(DESCRIPTION_TAG_NAME)) {
			isDescription = true;
			return;
		}

		if (isItem && qName.equals(LINK_TAG_NAME)) {
			isLink = true;
			return;
		}

		if (isItem && qName.equals(TITLE_TAG_NAME)) {
			isTitle = true;
			return;
		}

	}
	
	public void characters(char[] ch, int start, int length) throws SAXException {
		String data = new String(ch, start, length);

		if (isDate) {
			// TODO(thuang513): Parse date
			return;
		}

		if (isDescription) {
			// Grab the description
			currentDescription = data;
			return;
		}

		if (isLink) {
			// Store the link (articleURL)
			currentArticleUrl = data;
			return;
		}

		if (isTitle) {
			// Grab the title
			currentTitle = data;
			return;
		}
	}

	public void endElement(String uri, String localName, String qName) throws SAXException {

		if (qName.equals(ITEM_TAG_NAME)) {
			isItem = false;

			// TODO(thuang513): check that the URL is valid.
			if (currentArticleUrl == "") {
				LOGGER.severe("The article URL is empty!");
			}

			// Create a new article, and fill out its details.
			currentArticle = articleFactory.createArticle(currentArticleUrl);
			currentArticle.setTitle(currentTitle);
			currentArticle.setDescription(currentDescription);
			// TODO(thuang513): set the date

			// Store the article to the created list.
			newsArticles.add(currentArticle);
			currentArticle = null;
			return;
		}

		if (isItem && qName.equals(DATE_TAG_NAME)) {
			isDate = false;
			return;
		}

		if (isItem && qName.equals(DESCRIPTION_TAG_NAME)) {
			isDescription = false;
			return;
		}

		if (isItem && qName.equals(LINK_TAG_NAME)) {
			isLink = false;
			return;
		}

		if (isItem && qName.equals(TITLE_TAG_NAME)) {
			isTitle = false;
			return;
		}

	}

	/**  
	 *  Returns the list of parsed articles. The returned list
	 *  is not modifiable.
	 */
	public List<Article> getParsedArticles() {
		return Collections.unmodifiableList(newsArticles);
	}

}