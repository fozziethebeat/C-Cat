
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
		FileDocument forSentenceSpace = new FileDocument(doc.getCanonicalPath());

		// DEBUG: what file are we working on now?
		System.out.println("Processing: "+doc.toString());

	    
		vsm.processDocument(fileDocument.reader());
	

		sp.processDocument(forSentenceSpace.reader());
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
	HashMap<String, Double> tf_score = new HashMap(vsm.getWords().size());
	
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
	    
	    tf_score.put(word, normalizedScore);
	    
	    // DEBUG
	    //	    System.out.println(word+"\t"+normalizedScore);
	}


	/** 
	 *  Figure out the scores based off the sentence locations *
	 */
	
	sp.setupMatrix();

	System.out.println(sp.getWords().size());

	sp.calculateSentenceScore();

	HashMap<String, Double> sentenceScore = new HashMap(sp.getWords().size());

	// Go through the Sentence space and sum up the score of a word for the entire document set

	for(String word: sp.getWords()) {
	
	    // For every word, we want to pick the highest sentence score found in 
	    // the entire document set
	    
	    double highestSentenceScore = Double.MIN_VALUE;
	    Vector wordVector = sp.getVector(word);
	    
	    for(int i = 0; i < wordVector.length(); i++) {
		
		Number scoreForDocI = wordVector.getValue(i);

		if(highestSentenceScore < scoreForDocI.doubleValue() ) {
		    highestSentenceScore = scoreForDocI.doubleValue();
		}

	    }

	    sentenceScore.put(word, new Double(highestSentenceScore));
	}
	


	// for(String word : sp.getWords()){
	    
	//     // Get the vector that has all the sentence scores for a word
	//     Vector sentenceScoreVector = sp.getVector(word);
	    
	//     double wordSentenceScore = 0.0;
	    
	//     for (int i=0; i < sentenceScoreVector.length(); i++) {
	// 	Number termDocSentenceScore = sentenceScoreVector.getValue(i);
		
	// 	wordSentenceScore += termDocSentenceScore.doubleValue();
		

	//     }
	//     sentenceScore.put(word, new Double(wordSentenceScore));	    
	    
	//     if(highestSentenceScore < wordSentenceScore){
	// 	highestSentenceScoreWord = word;
	// 	highestSentenceScore = wordSentenceScore;

	// 	// DEBUG
	// 	//		System.out.println("NEW sentence high score: "+highestSentenceScoreWord +":\t"+highestSentenceScore);

	//     }

	// }
	

	// // Now normalize the scores by the highest score
	// for(String word : sentenceScore.keySet()) {
	    
	    

	// }



	/** 
	 * Figure out the log entropy score.
	 */

	// Perform entropy transform
	 System.setProperty(VectorSpaceModel.MATRIX_TRANSFORM_PROPERTY,
			   "edu.ucla.sspace.matrix.LogEntropyTransform");


	vsm.processSpace(System.getProperties());
	HashMap<String,Double> entropyScore = new HashMap(vsm.getWords().size());
	double highestEntropyScore = Double.MIN_VALUE;
	String highestEntropyWord = "";


	// Go through each word and calculate the entropy for all of the documents (the row)
	for(String word : vsm.getWords()) {
	    
	    Vector tf_for_docs = vsm.getVector(word);
	    
	    double globalEntropy = 0.0;

	    for(int i = 0; i < tf_for_docs.length(); i++) {
		
		Number doc_term_entropy = tf_for_docs.getValue(i);
		globalEntropy += doc_term_entropy.doubleValue();
		
	    }

	    entropyScore.put(word, new Double(globalEntropy));
	    
	    if(globalEntropy > highestEntropyScore) {
		highestEntropyScore = globalEntropy;
		highestEntropyWord = word;
	    }

	    // DEBUG
	    // System.out.println(word +":\t"+globalEntropy);
	}
	
	
	// Normalize the entropy score, by the largest value in the Map
	for(String word : entropyScore.keySet()) {
	    
	    // Get the entropy score
	    Double termEntropy = entropyScore.get(word);
	    
	    double normalizedEntropy = termEntropy.doubleValue()/highestEntropyScore;
	    entropyScore.put(word, new Double(normalizedEntropy));
	    
	    // DEBUG
	    // System.out.println(word + ":\t" + normalizedEntropy);
	    
	}




	/**
	 * Now that we have all the subcomponents of the equation, let's calculate the final score 
	 */

	// Go through every word and calculate the final score
	HashMap<String, Double> finalScore = new HashMap();
	
	for(String word : sp.getWords()) {
	    
	    // Get the appropriate score
	    
	    // Get the term frequency score
	    Double word_tf_score = tf_score.get(word);
	    
	    // Get the entropy score
	    Double wordEntropyScore = entropyScore.get(word);
	   
	    // Get the Sentence location score
	    Double wordSentenceScore = sentenceScore.get(word);
	    
	    if(wordSentenceScore == null) {
		System.err.println(word);
		wordSentenceScore = new Double(1.0);
	    }


	    if(word_tf_score == null) {
		System.err.println(word);
		word_tf_score = new Double(1.0);
	    }


	    if(wordEntropyScore == null) {
		System.err.println(word);
		wordEntropyScore = new Double(1.0);
	    }


	    double finalWordScore = (0.5 + word_tf_score.doubleValue()) *
		(0.5 + wordEntropyScore.doubleValue()) * 
		wordEntropyScore.doubleValue() * 
		wordSentenceScore.doubleValue();
	    
	    finalScore.put(word, new Double(finalWordScore));
	    
	    // DEBUG
	    System.out.println(word+":\t"+finalWordScore);
	    
	}



    }


}