package gov.llnl.document.preprocess;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

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

/**
 * This class represents an article in the BBC News website 
 * (http://news.bbc.co.uk).
 */
public class BBCArticle implements Article {

    /** Setup the logger */
    private static final Logger LOGGER = 
        Logger.getLogger(BBCArticle.class.getName());

    /**
     *  The last modified date of the article. 
     */
    private Date articleDate;

    /**
     *  The URL location of the article.
     */ 
    private final String articleURL;


    /**
     * Constant values that specify where the main content of the article is
     * located.
     */
    private static final String ARTICLE_BODY_ATTRIBUTE_NAME = "class";
    private static final String ARTICLE_BODY_ATTRIBUTE_VALUE = "story-body";

    /**
     *  Filter that is used to accept only text nodes.
     */ 
    private final static NodeClassFilter textFilter = 
	new NodeClassFilter(TextNode.class);

    /**
     *  Filter that is used to find the main content of the article.
     */
    private final static HasAttributeFilter storyBodyFilter =
	new HasAttributeFilter(ARTICLE_BODY_ATTRIBUTE_NAME, 
			       ARTICLE_BODY_ATTRIBUTE_VALUE);

    /**
     *  Filter that is used to find HTML paragraph elements (<p>). The idea of 
     *  searching for <p> tags is that most sites surround the sentences of their
     *  content in <p> tags.
     */
    private final static TagNameFilter pFilter = new TagNameFilter("p");

    /**
     *  The content of the article as a list of strings.
     */ 
    private List<String> content;

    public BBCArticle(String articleURL) {
	this.articleURL = articleURL;
	this.content = new ArrayList<String>();
    }

    /**
     *  Getter for the content of the article, as a list of sentences (Strings).
     */
    public List<String> getContent() {
	return Collections.unmodifiableList(content);
    }

    public String getArticleURL() {
	return articleURL;
    }

    /** 
     *	Extracts {@link Node}s that are only text.
     *  @return A list of text {@link Node}.
     */
    private NodeList extractTextNodes(NodeList nodes) {
        return nodes.extractAllNodesThatMatch(textFilter);
    }

    /**
     *  Fetches the content of the article and parses the article, extracting
     *  all the text.
     *
     * @return The status of the fetch (true if successful, false if not).
     */
    public boolean fetchAndParseContent() {
	NodeList storyNodes = null;

	try {
	    // Attempt to fetch and parse the article.
	    Parser parser = new Parser(articleURL);
	    storyNodes = parser.parse(storyBodyFilter);
	} catch(ParserException pe) {
	    StringBuffer errorMessage = new StringBuffer();
	    errorMessage.append("Article URL = ")
		.append(articleURL).append(" could not be parsed and fetched");
	    LOGGER.warning(errorMessage.toString());
	    return false;
	}

	SimpleNodeIterator iter =
	    storyNodes.elementAt(0).getChildren().elements();
	
	// Go through the nodelist and find all the text nodes.
	while(iter.hasMoreNodes()) {
	    Node nextNode = iter.nextNode();
	
	    if (pFilter.accept(nextNode)) {
		NodeList textNodes = extractTextNodes(nextNode.getChildren());
		SimpleNodeIterator nodeIter = textNodes.elements();

		while (nodeIter.hasMoreNodes()) {
		    Node innerNode = nodeIter.nextNode();
		    content.add(innerNode.toHtml());
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


    public static void main(String[] args) {
	BBCArticle testArticle =
	    new BBCArticle("http://www.bbc.co.uk/news/world-13986092");
	testArticle.fetchAndParseContent();
	System.out.println(testArticle.getContent());
	
	File toWrite = null;
	try {
	    toWrite = new File(args[0]);
	    if (!toWrite.createNewFile() && !toWrite.setWritable(true)) {
		LOGGER.warning("Couldn't open and create the file: " + toWrite);
	    }

	    testArticle.writeToFile(toWrite);
	} catch (SecurityException se) {
	    LOGGER.warning("Security issue with opening and creating file: " +
			   toWrite);
	} catch (IOException ioe) {
	    LOGGER.warning("Error with writing to file: " + toWrite);
	}


	
    }


}