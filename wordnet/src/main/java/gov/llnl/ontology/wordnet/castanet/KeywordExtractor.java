package gov.llnl.ontology.wordnet.castanet;

import gov.llnl.ontology.text.Document;

import java.util.Iterator;


/**
 *  All classes that implement this interface must be able to extracts keywords from a
 *  set of documents.
 *
 *  @author thuang513@gmail.com (Terry Huang)
 */

public interface KeywordExtractor {
	
	/**
     *  Given a directory filled with files, this method will extract the top N
     *  keywords from the files. Returns a map containing (filename, List of
     *  keywords in ranking order).
	 */
	Keyword[][] extractKeywords(Iterator<Document> docs,
                                int topNKeywords,
                                String stopWordsLoc);
}
