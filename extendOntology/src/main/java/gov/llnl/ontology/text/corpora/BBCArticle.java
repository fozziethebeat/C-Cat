package gov.llnl.ontology.text.corpora;

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

import gov.llnl.ontology.text.Document;

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
public class BBCArticle extends AbstractArticle implements Document {

    /** Setup the logger */
    private static final Logger LOGGER = 
        Logger.getLogger(BBCArticle.class.getName());

    public BBCArticle(String articleURL) {
	super("class", "story-body", "BBC", articleURL);
    }

    public String key() {
	return getArticleURL();
    }

    public long id() {
	return 0;
    }

    public String title() {
	return "";
    }
    
    public Set<String> categories() {
	return null;
    }

    public static void main(String[] args) {
	BBCArticle testArticle =
	    new BBCArticle("http://www.bbc.co.uk/news/world-13986092");
	testArticle.fetchAndParseContent();
	System.out.println(testArticle.rawText());
	
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