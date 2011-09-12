package gov.llnl.ontology.text.corpora.rss;

import gov.llnl.ontology.text.corpora.Article;
import gov.llnl.ontology.text.corpora.BbcArticleFactory;
import gov.llnl.ontology.text.corpora.rss.NewsHandler;

import java.io.File;
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
		if (articlesToFetch == null) {
			LOGGER.severe("No articles to fetch!");
			return;
		}

		for (Article article : articlesToFetch) {
			if (!article.fetchAndParseContent()) {
				LOGGER.warning("Could not fetch and parse article: " + article.getArticleUrl());
			}
		}
	}

	/**
	 *  Write the content of all the articles to a given directory.
	 *
	 *  @param directory - Directory to write the articles to.
	 */
	private void writeContentsToDirectory(String directory) {
		String targetDirectory = directory.charAt(directory.length() -1) == '/' ?
			                                                directory : directory + "/";

		for (Article article : articlesToFetch) {
			try {
				// Create the file
				String filename = targetDirectory + article.key();
				File fileToWrite = new File(filename);
				
				// Check that we can write to this file
				if (!fileToWrite.canWrite()) {
					LOGGER.warning("Unable to write to " + filename +
					               "! Either invalid pathname, or we are not allowed to write.");
					continue;
				}
				article.writeToFile(fileToWrite);
			} catch (IOException ioe) {
				LOGGER.severe("An IO Error occured when trying to write: '" + article.toString() + "'");
			} catch (SecurityException se) {
				LOGGER.severe("No access to the file, when trying to write article: " + article.toString());
			}
		}
	}

	public void processFeed(String outputDir) {
		parseFeed();
		fetchArticles();
		writeContentsToDirectory(outputDir);
	}

	public static void main(String[] args) {

		if (args.length < 1) {
			LOGGER.severe("Specify the output directory!");
			return;
		}

		if (args[0] == "") {
			LOGGER.severe("Output directory cannotn be empty!");
			return;
		}
			
		final String BBC_NEWS_FEED = "http://feeds.bbci.co.uk/news/rss.xml";
		RssFeed bbcFeed = new RssFeed(BBC_NEWS_FEED);
		bbcFeed.processFeed(args[0]);
	}
}