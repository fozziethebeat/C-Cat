package gov.llnl.ontology.wordnet.castanet;

import gov.llnl.ontology.wordnet.castanet.Keyword;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.logging.Logger;


import edu.ucla.sspace.text.FileDocument;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vsm.VectorSpaceModel;

/**
 *  A wrapper around S-Space's {@code VectorSpaceModel} class. This class allows you to 
 *  interface directly with {@code File} objects as well as access the document column and
 *  word row vectors.
 *
 *  @author thuang513@gmail.com (Terry Huang)
 */
public class FileVectorSpaceModel extends VectorSpaceModel {
	
	/**
	 * The logger used to record all output
	 */
	private static final Logger LOGGER = Logger.getLogger(FileVectorSpaceModel.class.getName());

	/**
	 *  Used to track if the vector space has been processed yet.
	 */
	private boolean hasBeenProcessed = false;

	/**
	 *  Used to keep track of which column maps to which file object.
	 */
	ConcurrentMap<File, Integer> documentToColumnIndex;

	/**
	 * The name prefix used with {@link #getName()}
	 */
	private static final String VSM_SSPACE_NAME = "file-vector-space-model";

	/**
	 *  Used to keep track of the documents.
	 */
	private final AtomicInteger internalDocumentCounter;

	public FileVectorSpaceModel() throws IOException{
		super();
		documentToColumnIndex = new ConcurrentHashMap();
		internalDocumentCounter = new AtomicInteger(0);
	}

	/**
	 * {@inheritDoc}
	 */
	public String getSpaceName() {
		return VSM_SSPACE_NAME;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @param properties {@inheritDoc} See this class's {@link VectorSpaceModel
	 *        javadoc} for the full list of supported properties.
	 */
	public void processSpace(Properties properties) {
		super.processSpace(properties);
		hasBeenProcessed = true;
	}	

	/*
	public DoubleVector getDocumentVector(File file) {
		// Check that the space has already been processed.
		if (!hasBeenProcessed) {
			LOGGER.warning("The vector space has not yet been processed!");
			throw new IllegalStateException("Vector Space has not yet been processed.");
		}

		// Figure out which column has the file
		Integer documentColumnPosition = documentToColumnIndex.get(file);
		return wordSpace.getColumnVector(documentColumnPosition.intValue());
	}
	*/

	/**
	 *  Gets the top keywords of a based of the score in the word space.
	 */
	public List<Keyword> getRankedWordsOfFile(File file) {
		if (file == null) {
			LOGGER.warning("The file is null!");
			return null;
		}

		// Go through all the word scores, figure out what word they represent
		// and add them to the list.
		List<Keyword> returnList = new ArrayList();
		Integer documentColumnPosition = documentToColumnIndex.get(file);

		for (String word : getWords()) {
			Vector wordVector = getVector(word);
			returnList.add(new Keyword(word,
			                           wordVector
			                           .getValue(documentColumnPosition.intValue())
			                           .doubleValue()));
		}

	  Collections.sort(returnList);
	  Collections.reverse(returnList);

	  return returnList;
	}

	/**
	 *  DEPRECATED! don't use this for this class, or else we can't keep track
	 *  of the files!
	 */
	public void processDocument(BufferedReader document) {
		LOGGER.severe("Call processFile instead of processDocument for FileVectorSpaceModel!");
		throw new IllegalStateException("Call processFile() instead of processDocument!");
	}

	/**
	 * Tokenizes the document using the {@link IteratorFactory} and updates the
	 * term-document frequency counts.
	 *
	 * <p>
	 *
	 * This method is thread-safe and may be called in parallel with separate
	 * documents to speed up overall processing time.
	 *
	 * @param document {@inheritDoc}
	 */
	public void processFile(File file) throws IOException {
		if (!file.exists()) {
			LOGGER.severe(file.getCanonicalPath() + " does not exist!");
			return;
		}
		
		FileDocument fileDocument = new FileDocument(file.getCanonicalPath());
		LOGGER.info("Processing: " + file.getCanonicalPath());
		super.processDocument(fileDocument.reader());
		
		Integer docLocation = internalDocumentCounter.incrementAndGet();
		documentToColumnIndex.put(file, docLocation);

		// Keep track of which document is in which column.
		// TODO(thuang513): CAUSES COMPILE ERROR!
		//		documentToColumnIndex.put(file, Integer.valueOf(super.documentCounter.get()));
	}
}