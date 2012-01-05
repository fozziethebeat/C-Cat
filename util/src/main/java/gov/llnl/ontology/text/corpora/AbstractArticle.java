package gov.llnl.ontology.text.corpora;

import gov.llnl.ontology.text.Document;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.Parser;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.util.SimpleNodeIterator;
import org.htmlparser.Node;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.SimpleNodeIterator;
import org.htmlparser.visitors.TextExtractingVisitor;

/**
 * This class represents an abstract article. Implementations of future {@link Article}s
 * should use this as a base class. Given the <b>attribute name</b> and 
 * <b>attribute value</b> of the <div> that surrounds the main content of the article, 
 * the fetchAndParse() will grab all the content of the articles.
 *
 * For Example:
 * <pre>
 * {@code 
 *    <div id="content">
 *          ...... (Article Content) .......
 *    </div>
 * }
 * </pre>
 * 
 * In this example the attribute name = "id", and attribute value = "content".
 * 
 * @author Terry Huang
 */
public abstract class AbstractArticle implements Article {

	/** 
	 *  Setup the logger 
	 */
	private static final Logger LOGGER = 
		Logger.getLogger(AbstractArticle.class.getName());
		
	/**
	 *  The URL location of the article.
	 */ 
	private String articleUrl;

	/**
	 *  Description of the article.
	 */
	private String description;

	/**
	 *  The raw content of the URL's HTML
	 */ 
	private String rawHTML;

	/**
	 *  The title of the article.
	 */ 
	private String articleTitle;
    
	/**
	 *  The content of the article as a list of strings.
	 */ 
	private List<String> content;
    
	/**
	 * Constant values that specify where the main content of the article is
	 * located.
	 */
	private static String ARTICLE_BODY_ATTRIBUTE_NAME;
	private static String ARTICLE_BODY_ATTRIBUTE_VALUE;

	/**
	 *  Filter that is used to accept only text nodes.
	 */ 
	private static final NodeClassFilter textFilter = 
		new NodeClassFilter(TextNode.class);

	/**
	 *  Filter that is used to find the main content of the article.
	 */
	private static HasAttributeFilter storyBodyFilter;
	private static final TagNameFilter titleFilter = new TagNameFilter("title");

	/**
	 *  Filter that is used to find HTML paragraph elements (<p>). The idea of 
	 *  searching for <p> tags is that most sites surround the sentences of their
	 *  content in <p> tags.
	 */
	private static final TagNameFilter pFilter = new TagNameFilter("p");

	/**
	 * The corpus name for any {@link Document} returned by this class.
	 */
	private static String CORPUS_NAME;

	protected AbstractArticle(String attributeName, 
	                          String attributeValue,
	                          String corpusName,
	                          String articleUrl) {
		ARTICLE_BODY_ATTRIBUTE_NAME = attributeName;
		ARTICLE_BODY_ATTRIBUTE_VALUE = attributeValue;
		storyBodyFilter = new HasAttributeFilter(ARTICLE_BODY_ATTRIBUTE_NAME,
		                                         ARTICLE_BODY_ATTRIBUTE_VALUE);
		
		this.content = new ArrayList<String>();
		this.articleUrl = articleUrl;
		CORPUS_NAME = corpusName;
	}

	/**
	 *  Getters for the article URL.
	 */
	public String getArticleUrl() {
		return articleUrl;
	}

	public String getDescription() {
		return this.description;
	}

	public String getTitle() {
		return this.articleTitle;
	}

	/**
	 *  Setters for the article URL.
	 */
	public void setArticleUrl(String articleUrl) {
		this.articleUrl = articleUrl;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setTitle(String title) {
		this.articleTitle = title;
	}

	public String sourceCorpus() {
		return CORPUS_NAME;
	}

	public String rawText() {
		StringBuilder result = new StringBuilder();
		for (String s : getContent()) {
			result.append(s);
		}
		return result.toString();
	}

	public String originalText() {
		return rawHTML;
	}

	public String key() {
		return articleUrl;
	}

	public long id() {
		return 0;
	}

	public String title() {
		return articleTitle;
	}
    
	public Set<String> categories() {
		return null;
	}
	
	/**
	 *  Getter for the content of the article, as a list of sentences (Strings).  
	 */
	public List<String> getContent() {
		return Collections.unmodifiableList(content);
	}
	
	protected boolean fetch() {
		StringBuilder contentStore = new StringBuilder();
		BufferedReader in = null;
		
		try {
			URL urlObject = new URL(articleUrl);
			in = new BufferedReader(new InputStreamReader(urlObject.openStream()));
			String lineRead;
			while ( (lineRead = in.readLine()) != null) {
				contentStore.append(lineRead);
			} 
		} catch (MalformedURLException badUrl) {
			LOGGER.warning("Bad URL format: " + badUrl.toString());
			return false;
		} catch (IOException ioe) {
			LOGGER.warning("Could not fetch article: " + articleUrl);
			return false;
		}
		
		rawHTML = contentStore.toString();
		return true;
	}
	
	/**
	 *  Fetches the content of the article and parses the article, extracting
	 *  all the text.
	 *
	 * @return The status of the fetch (true if successful, false if not).
	 */
	public boolean fetchAndParseContent() {
		NodeList mainNode = null;
		NodeList storyNodes = null;
		NodeList titleNode = null;
		
		// Try to fetch the content of the article
		if (!fetch())
			return false;
		
		try {
			// Attempt to fetch and parse the article.
			Parser parser = new Parser(rawHTML, null);
			mainNode = parser.parse(null);
			
			// Grab the main content
			storyNodes = mainNode.extractAllNodesThatMatch(storyBodyFilter, true);
			
			// Grab the title
			extractTitle(mainNode);
			
			// Grab the date 
			extractDate(mainNode);
			
		} catch(ParserException pe) {
			StringBuffer errorMessage = new StringBuffer();
			errorMessage.append("Article URL = ").append(articleUrl).append(" could not be fetched");
			LOGGER.warning(errorMessage.toString());
			return false;
		}
		
		SimpleNodeIterator storyBodyIter =
			storyNodes.elementAt(0).getChildren().elements();
		
		// Go through the nodelist and find all the text nodes.
		while(storyBodyIter.hasMoreNodes()) {
			Node nextNode = storyBodyIter.nextNode();
			if (pFilter.accept(nextNode)) {
				NodeList textNodes = extractTextNodes(nextNode.getChildren());
				SimpleNodeIterator nodeIter = textNodes.elements();
				while (nodeIter.hasMoreNodes()) {
					Node innerNode = nodeIter.nextNode();
					content.add(StringEscapeUtils.unescapeHtml(innerNode.toHtml()));
				}
			}
		}
		return true;
	}
	
	public Date getDate() {
		return null;
	}
	
	/**
	 *  Writes the content of the article into a File.
	 */
	public void writeToFile(File outFile) throws IOException {
		FileWriter out = new FileWriter(outFile);
		
		for (String sentence : getContent()) {
			out.write(new StringBuffer(sentence).append('\n').toString());
		}
		
		// Do some clean up work.
		out.flush();
		out.close();
	}
	
	/** 
	 *	Extracts {@link Node}s that are only text.
	 *  @return A list of text {@link Node}.
	 */
	protected NodeList extractTextNodes(NodeList nodes) {
		return nodes.extractAllNodesThatMatch(textFilter, true);
	}
	
	/**
	 *  Extract the title from articles.
	 */
	protected void extractTitle(NodeList rootNode) throws ParserException {
		NodeList titleNode = rootNode.extractAllNodesThatMatch(titleFilter, true);
		NodeList titleTextNode = extractTextNodes(titleNode);
		this.setTitle(StringEscapeUtils
		              .unescapeHtml(titleTextNode.toHtml()));
	}
	
	/**
	 *  Extract the date from the articles
	 */
	protected void extractDate(NodeList rootNode) throws ParserException {	

	}
	
	public boolean equal(Object o) {
		if (o instanceof AbstractArticle) {
			return key().equals(((AbstractArticle)o).key());
		} else {
			return false;
		}
	}
	
	public int hashCode() {
		return key().hashCode(); 
	}
}