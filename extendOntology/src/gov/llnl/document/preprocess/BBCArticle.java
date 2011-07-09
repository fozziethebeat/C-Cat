package gov.llnl.document.preprocess;

import java.util.Date;
import java.util.logging.Logger;

import org.htmlparser.nodes.TextNode;

import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.NodeClassFilter;


import org.htmlparser.Parser;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import org.htmlparser.Node;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.SimpleNodeIterator;

/**
 * This class represents an article in the BBC News website (http://news.bbc.co.uk).
 * 
 */
public class BBCArticle {
    
    public BBCArticle(String articleURL) {
	this.articleURL = articleURL;
    }

    public String fetchAndParseContent() {


	HasAttributeFilter storyBodyFilter = new HasAttributeFilter(ARTICLE_BODY_ATTRIBUTE_NAME, 
								    ARTICLE_BODY_ATTRIBUTE_VALUE);

	HasAttributeFilter contentIntro = new HasAttributeFilter(CONTENT_ATTRIBUTE_NAME, 
								     CONTENT_INTRO_ATTRIBUTE_VALUE);

	HasAttributeFilter contentBody = new HasAttributeFilter(CONTENT_ATTRIBUTE_NAME, 
								     CONTENT_BODY_ATTRIBUTE_VALUE);
 
	OrFilter articleContentFilter = new OrFilter(contentIntro, contentBody);
	AndFilter filter = new AndFilter(storyBodyFilter, articleContentFilter);
	NodeList storyNodes = null;
	
	try {
	    // Attempt to fetch and parse the article.
	    Parser parser = new Parser(articleURL);
	    storyNodes = parser.parse(storyBodyFilter);

	} catch(ParserException pe) {
	    StringBuffer errorMessage = new StringBuffer();
	    errorMessage.append("Article URL = ").append(articleURL).append(" could not be parsed and fetched");
	    LOGGER.warning(errorMessage.toString());
	    return "";
	}


	NodeClassFilter TextFilter = new NodeClassFilter(TextNode.class);
	NodeList storyText = new NodeList();
	SimpleNodeIterator iter = storyNodes.elementAt(0).getChildren().elements();

	// Go through the nodelist and find all the text nodes.
	while(iter.hasMoreNodes()) {
	    Node nextNode = iter.nextNode();
	    
	    System.out.println(nextNode);
	    if(nextNode.getClass() ==  TextNode.class) {
		storyText.add(nextNode);
	    }
	}

	return storyText.toHtml();
    }
    
    public Date getDate() {
	return null;
    }

    /** Setup the logger */
    private static final Logger LOGGER = 
        Logger.getLogger(BBCArticle.class.getName());
    
    private Date articleDate;
    private final String articleURL;

    private static final String ARTICLE_BODY_ATTRIBUTE_NAME = "class";
    private static final String ARTICLE_BODY_ATTRIBUTE_VALUE = "story-body";


    private static final String CONTENT_ATTRIBUTE_NAME = "id";
    private static final String CONTENT_INTRO_ATTRIBUTE_VALUE = "story_continues_1";
    private static final String CONTENT_BODY_ATTRIBUTE_VALUE = "story_continues_2";
    

    public static void main(String[] args) {
	BBCArticle testArticle = new BBCArticle("http://www.bbc.co.uk/news/world-13986092");
	System.out.println(testArticle.fetchAndParseContent());
    }


}