package gov.llnl.ontology.wordnet.castanet;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 *  All classes that implement this interface must be able to extracts keywords from a
 *  set of documents.
 *
 *  @author thuang513@gmail.com (Terry Huang)
 */

public interface KeywordExtractor {
	
	/**
	 *  Given a directory filled with files, this method will extract the top N keywords
	 *  from the files. Returns a map containing (filename, List of keywords in ranking order).
	 */
	Map<File, List<Keyword>> extractKeywordsFromFiles(File docFolder, int topNKeywords, String stopWordsLoc);

}