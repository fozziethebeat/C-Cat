
package gov.llnl.ontology.wordnet.castanet;

import edu.ucla.sspace.vsm.VectorSpaceModel;
import edu.ucla.sspace.common.SemanticSpace;



import java.util.logging.Logger;

public class Autosummary {

    private static final Logger LOGGER = 
        Logger.getLogger(Autosummary.class.getName());

    public static void calculateSakaiEtAlScore(File directory) {
	

	// Set up all the subcomponents of the score

	// Get the term frequency
	VectorSpaceModel vsm = new VectorSpaceModel();

	// Get the idf scores
	//	VectorSpaceModel idf = new VectorSpaceModel();

	// Get the sentence locations, as well as the total number of sentences in a document
	SentenceSpace sp = new SentenceSpace();

	// Get the entropy scores
	SemanticSpace entropy = new VectorSpaceModel();

	// For every document in the directory, process it in the different spaces

	File dir = new File(folderLocation);
	
	// Make sure folderLocation is a working directory
	if(!dir.isDirectory() || !dir.exists()) {
	    LOGGER.info("calculateSakaiEtAlScore: Invalid Directory!");
	    return;
	}

	for(File doc: dir.listFiles()) {
	    FileDocument fileDocument = new FileDocument(doc.getCanonicalPath());
	    
	    vsm.processDocument(fileDocument.reader());
	    //	    idf.processDocument(fileDocument.reader());
	    sp.processDocument(fileDocument.reader());
	    entropy.processDocument(fileDocument.reader());
	}

	// Perform/setup the appropriate transformations
	
	// Perform entropy transform
	System.setProperty(VectorSpaceModel.MATRIX_TRANSFORM_PROPERTY,
			   "edu.ucla.sspace.matrix.LogEntropyTransform");

	entropy.processSpace(System.getProperties());
	
	// Perform TFIDF transform
	//	System.setProperty(VectorSpaceModel.MATRIX_TRANSFORM_PROPERTY,
	//		   "edu.ucla.sspace.matrix.LogEntropyTransform");
	
	//	vsm.processSpace(System.getProperties());

	// Figure out Tf(t[i], S): how many times the word t[i] appears in the document set
	

    }


}