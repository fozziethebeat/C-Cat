package gov.llnl.ontology.text.corpora.rss;

import gov.llnl.ontology.text.corpora.BbcArticleFactory;
import gov.llnl.ontology.text.corpora.rss.NewsHandler;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class RssFeed {

	/** 
	 *  Setup the logger 
	 */
	private static final Logger LOGGER = 
		Logger.getLogger(RssFeed.class.getName());

	private String feedUrl;
	
	private List<Article> articlesToFetch;

	public RssFeed(String feedUrl) {
		this.feedUrl = feedUrl;
	}

	private void parseFeed() {
		SAXParserFactory parserFactory = SAXParserFactory.newInstance();
		NewsHandler newsHandle = new NewsHandler(new BbcArticleFactory());

		try {
			SAXParser parser = parserFactory.newSAXParser();
			parser.parse(feedUrl, newsHandle);
		} catch (IOException ioe) {
			LOGGER.severe("IO error occurred!");
		} catch (SAXException sax) {
			LOGGER.severe("Error with the parser!");
		} catch (ParserConfigurationException pce) {
			LOGGER.severe("Parser configuration error!");
		}

		LOGGER.info("Parsed " + newsHandle.getParsedArticles().size() + " articles.");
		articlesToFetch = newsHandle.getParsedArticles();
	}

	private void fetchArticles() {
		
	}

	public static void main(String[] args) {

		System.out.println(args[0]);
		final String BBC_NEWS_FEED = "http://feeds.bbci.co.uk/news/rss.xml";
		RssFeed bbcFeed = new RssFeed(BBC_NEWS_FEED);
		bbcFeed.parseFeed();
	}
}