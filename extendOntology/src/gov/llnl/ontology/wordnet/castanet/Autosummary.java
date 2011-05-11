
package gov.llnl.ontology.wordnet.castanet;

import edu.ucla.sspace.vsm.VectorSpaceModel;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.vector.Vector;

import edu.ucla.sspace.text.FileDocument;



import java.io.File;
import java.io.IOException;

import java.util.HashMap;
import java.util.logging.Logger;

public class Autosummary {



    private static final Logger LOGGER = 
        Logger.getLogger(Autosummary.class.getName());

    public static void calculateSakaiEtAlScore(String directory) throws IOException{
	

	// Set up all the subcomponents of the score

	// Get the term frequency
	SemanticSpace vsm = new VectorSpaceModel();

	// Get the idf scores
	//	VectorSpaceModel idf = new VectorSpaceModel();

	// Get the sentence locations, as well as the total number of sentences in a document
	SentenceSpace sp = new SentenceSpace();

	// For every document in the directory, process it in the different spaces

	File dir = new File(directory);
	
	// Make sure dir is a working directory
	if(!dir.isDirectory() || !dir.exists()) {
	    LOGGER.info("calculateSakaiEtAlScore: Invalid Directory!");
	    return;
	}


	try{

	    for(File doc: dir.listFiles()) {
		FileDocument fileDocument = new FileDocument(doc.getCanonicalPath());
	    
		// DEBUG: what file are we working on now?
		System.out.println("Processing: "+doc.toString());

	    
		vsm.processDocument(fileDocument.reader());
		//	    idf.processDocument(fileDocument.reader());
		sp.processDocument(fileDocument.reader());
		//    entropy.processDocument(fileDocument.reader());
	    }

	} catch (IOException ioe) {
	    LOGGER.info("Error in loading an processing the documents.");
	}

	// Perform/setup the appropriate transformations
	
	 /**
	 * Figure out the term frequency in all documents Tf(t[i], S): how many times the word t[i] appears in the document set. 
	 *
	 */
	
	// Finalize the array
	vsm.processSpace(System.getProperties());
	
	// Create a Map that keeps track of Tf(t[i], S)
	HashMap<String, Double> set_term_frequency = new HashMap(vsm.getWords().size());
	
	String highest_TF_word = "";
	int highest_TF_count = -1;

	// Go through each row in vsm and count it
	for(String word : vsm.getWords()) {
	    
	    Vector v = vsm.getVector(word);
	    
	    // if(v == null) 
	    // 	System.out.println(word +"is null");

	    int set_term_frequency_for_word = 0;

	    // Go through the vector and add up all of the occurences
	    for(int i = 0; i < v.length(); i++) {
		
		Number term_freq_for_word_i = v.getValue(i);
		set_term_frequency_for_word += term_freq_for_word_i.intValue();
		
	    }
	    
	    // Add the stats
	    set_term_frequency.put(word, new Double(set_term_frequency_for_word));
	    
	    if(highest_TF_count < set_term_frequency_for_word){
		highest_TF_count = set_term_frequency_for_word;
		highest_TF_word = word;

	    }
	}

	
	// Normalize the set term frequency score
	for(String word : set_term_frequency.keySet()) {
	    
	    Double originalTFScore = set_term_frequency.get(word);
	    Double normalizedScore = new Double(originalTFScore.doubleValue()/highest_TF_count);
	    
	    set_term_frequency.put(word, normalizedScore);
	    
	    // DEBUG
	    System.out.println(word+"\t"+normalizedScore);
	}


	/** 
	 *  Figure out the scores based off the sentence locations *
	 */
	
	//	sp.setupMatrix();
	//	sp.calculateSentenceScore();


	// Perform entropy transform
	// System.setProperty(VectorSpaceModel.MATRIX_TRANSFORM_PROPERTY,
	//		   "edu.ucla.sspace.matrix.LogEntropyTransform");

	//	entropy.processSpace(System.getProperties());
	


    }


}