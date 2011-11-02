package gov.llnl.ontology.wordnet.castanet;

import gov.llnl.ontology.wordnet.castanet.FileVectorSpaceModel;
import gov.llnl.ontology.wordnet.castanet.Keyword;
import gov.llnl.ontology.wordnet.castanet.KeywordExtractor;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.text.IteratorFactory;
import edu.ucla.sspace.vsm.VectorSpaceModel;


/**
 *  Keyword extractor that uses TF-IDF to score the keywords and then extracts the keywords
 *  based on this score.
 *
 *  @author thuang513@gmail.com (Terry Huang)
 */
public class TfidfKeywordExtractor implements KeywordExtractor {
	
	/**
	 * The logger used to record all output
	 */
	private static final Logger LOGGER =
		Logger.getLogger(TfidfKeywordExtractor.class.getName());

	private static KeywordExtractor instance = null;

	private static final String TFIDF_TRANSFORM_NAME = "edu.ucla.sspace.matrix.TfIdfTransform";

	/**
	 *  Enforce the singleton property.
	 */
	private TfidfKeywordExtractor() {}

	/**
	 *  Enforce the singleton property.
	 */
	public static KeywordExtractor getInstance() {
		if (instance == null) {
			return new TfidfKeywordExtractor();
		}
		return instance;
	}

	public Map<File, List<Keyword>> extractKeywordsFromFiles(File docFolder,
	                                                        int topN,
	                                                        String stopWordsLoc) {
		// Check that the docFolder is actually a folder.
		if (!docFolder.isDirectory()) {
			LOGGER.warning(docFolder.getAbsolutePath() + " is not a directory!");
			return null;
		}

		// If we have a stop word list, then use it.
		if (!stopWordsLoc.equals("")) {
			LOGGER.info("Setting the stop words file = " + stopWordsLoc);
			System.setProperty(IteratorFactory.TOKEN_FILTER_PROPERTY,
			                   "exclude=" + stopWordsLoc);
			IteratorFactory.setProperties(System.getProperties());
		}

		// Process the documents into the space.
		FileVectorSpaceModel fileVsm = null;
		File[] listOfFiles = docFolder.listFiles();
		try {
			fileVsm = new FileVectorSpaceModel();
			for (File doc : listOfFiles) {
				fileVsm.processFile(doc);
			}
		} catch (IOException ioe) {
			LOGGER.severe("An I/O exception occured: " + ioe.toString());
		}

		// Apply TFIDF transform
		System.setProperty(VectorSpaceModel.MATRIX_TRANSFORM_PROPERTY,
		                   TFIDF_TRANSFORM_NAME);
		fileVsm.processSpace(System.getProperties());
		
		LOGGER.info("Done with TF-IDF Transform...");

		// Go through all the files and get their top keywords.
		Map<File, List<Keyword>> keywords = new HashMap<File, List<Keyword>>();
		for (File doc : listOfFiles) {
			keywords.put(doc, fileVsm.getRankedWordsOfFile(doc).subList(0, topN));
		}

		return keywords;
	}
}