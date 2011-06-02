
package gov.llnl.ontology.wordnet.castanet;


import edu.ucla.sspace.vsm.VectorSpaceModel;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.vector.Vector;

import edu.ucla.sspace.text.FileDocument;



import java.io.File;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;
import java.util.Set;
import java.util.Comparator;


import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.basis.StringBasisMapping;
import edu.ucla.sspace.vector.AmortizedSparseVector;
import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.util.Duple;

import edu.ucla.sspace.vector.DoubleVector;

import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.ling.HasWord;

public class Autosummary {



    private static final Logger LOGGER = 
        Logger.getLogger(Autosummary.class.getName());

    
    /**
     * Returns a Map with all the words as keys and their Sakai Et Al. scores as values. 
     *
     */
    public static Map<String, Double> calculateSakaiEtAlScore(String directory, String stopWordsFile) throws IOException{
	

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
	    return null;
	}

	
	
	// Remove any stop words, if required.
	if(!stopWordsFile.equals("")){
	    LOGGER.info("setting the stop word file = "+stopWordsFile);
	    System.setProperty(IteratorFactory.TOKEN_FILTER_PROPERTY, "exclude="+stopWordsFile);
	    IteratorFactory.setProperties(System.getProperties());
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
	    System.out.println(word+"\t"+normalizedScore);
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


	// TODO: figure out which one is better, vsm or sp?
	
	for(String word : vsm.getWords()) {
	    
	    // Get the appropriate score
	    
	    // Get the term frequency score
	    Double word_tf_score = tf_score.get(word);
	    
	    // Get the entropy score
	    Double wordEntropyScore = entropyScore.get(word);
	   
	    // Get the Sentence location score
	    Double wordSentenceScore = sentenceScore.get(word);

	    // DEBUG
	    // if(word_tf_score == null && wordSentenceScore == null) {

	    // 	LOGGER.warning("Could not find the word: \""+word+"\" in either vsm or SentenceSpace.");
	    // }

	    
	    if(word_tf_score == null && word_tf_score == null){

		// DEBUG
		//		LOGGER.warning("Vector Space Model couldn't find word: \""+word+"\". Giving these guys the default score of 1.0 then.");

		// word_tf_score = new Double(0.0);
		// wordEntropyScore = new Double(0.0);

		continue;

	    }else if (wordSentenceScore == null) {
		

		// DEBUG
		//		LOGGER.warning("Could not find word: \"" + word + "\" in the SentenceSpace. Giving this a default score of 1.0");

		wordSentenceScore = new Double(1.0);
	    }
	    

	    double finalWordScore = (0.5 + word_tf_score.doubleValue()) *
		(0.5 + wordEntropyScore.doubleValue()) * 
		wordEntropyScore.doubleValue() * 
		wordSentenceScore.doubleValue();
	    
	    finalScore.put(word, new Double(finalWordScore));
	    
	    // DEBUG
	    //	    System.out.println(word+":\t"+finalWordScore);
	    
	}


	return finalScore;
    }


    /***
     * Recalculates the scores based on what words were selected. In this measure, words that were selected will 
     * have their scores boosted, based on how many keywords there were.
     */
    public static Map<String, Double> recalculateSelectedScores(Map<String, Double> wordScores, Set<String> wordsSelected, int numWordsShown) {
	
	Map<String, Double> result = new HashMap(wordScores);
	
	// Go through each selected word and update its score in 
	Iterator<String> iter = wordsSelected.iterator();
	
	while(iter.hasNext()) {
	    
	    String term = iter.next();
	    Double originalScore = wordScores.get(term);
	    
	    Double newScore = new Double(originalScore.doubleValue() * 0.5 * numWordsShown + 1.0);
	    result.put(term, newScore);
	}
	
	return result;
    }

    
    /**
     * Generates the summary of a list of sentences, by returning the top m sentences of the list.
     */
    public static List< Duple<String, Double> > topSentences(List<String> sentences, 
							     Map<String, Double> keywordScores ) {
	 

	List< Duple<String, Double> > topSentences = new ArrayList(sentences.size());	
	
	// For every sentence...
	for(String sentence : sentences) {

	    // Tokenize the sentences into words.
	    Iterator sentenceIter = IteratorFactory.tokenize(sentence);
	    DoubleVector sentenceVector = new AmortizedSparseVector();
	    DoubleVector keywordVector = new AmortizedSparseVector();
	    
	    // Create a basis mapping that will keep track of what location of the DoubleVector matches what word.
	    StringBasisMapping termBasis = new StringBasisMapping();
	
	    
	    // Fill the vector with words from the sentence
	    // TODO: use a POS tagger to only get the nouns
	    while(sentenceIter.hasNext()) {
		String term = (String) sentenceIter.next();
		int termDimension = termBasis.getDimension(term);
		
		// Get the score for the current term
		Double termScore = keywordScores.get(term);
		
		// DEBUG: Make sure that the extracted word form the sentence is actually in keywordScores.
		if(termScore == null) {
		    //		    LOGGER.warning("Couldn't find a score for the term = " + term + "!");
		    continue;
		}
		
		// Add the term to the sentence vector
		sentenceVector.set(termDimension, termScore.doubleValue());
	    }

	    
	    // Create a new vector that represents the keywords.
	    for(String keyword : keywordScores.keySet()) {
		// Figure out where to put this keyword
		int keywordDimension  = termBasis.getDimension(keyword);
		
		// Get the score for the keyword.
		Double keywordScore = keywordScores.get(keyword);
		
		// Add the keyword to the keywordVector
		keywordVector.set(keywordDimension, keywordScore.doubleValue());

	    }

	    
	    // Make sure that the sentenceVector and keywordVector both match in the number of dimensions
	    if(sentenceVector.length() != termBasis.numDimensions()) {
		// Set the last position to be equal to zero, so that the vector will update its known length.
		sentenceVector.set(termBasis.numDimensions() - 1, 0.0);
	    }


	    // Make sure that the sentenceVector and keywordVector both match in the number of dimensions
	    if(keywordVector.length() != termBasis.numDimensions()) {
		// Set the last position to be equal to zero, so that the vector will update its known length.
		keywordVector.set(termBasis.numDimensions() - 1, 0.0);
	    }

	    
	    // Calculate the cosine similarity 
	    double cosineSimilarity = Similarity.getSimilarity(Similarity.SimType.COSINE, sentenceVector.toArray(),
							       keywordVector.toArray());
	    
	    Duple<String, Double> sentencePair = new Duple(sentence, new Double(cosineSimilarity));
	    topSentences.add(sentencePair);
	}

	
	DupleComparator comparator = new DupleComparator();

	// Sort the sentences by their scores.
	Collections.sort(topSentences, comparator);
	Collections.reverse(topSentences);

	return topSentences;
	
    }


    public static void autosummarize(String path, String stopWordFile) throws IOException {

	// Extract keywords from a list of files in a folder
	Map extractedKeywords = Castanet.extractKeywordsFromDocument(path, 30, stopWordFile);
	    
	// Figure out the scores for the keywords
	Map<String, Double> keywordScores = Autosummary.calculateSakaiEtAlScore(path, stopWordFile);
	    	    
	// Find a file in the test folder
	// Here we are going to use the first file
	List<File> files = (List)extractedKeywords.get("files");
	
	// For every file, we are going to generate an automated summary.
	for(File file : files) {

	    FileDocument fileDocument = new FileDocument(file.getCanonicalPath());
	    DocumentPreprocessor processor = new DocumentPreprocessor(fileDocument.reader());
	    List<String> sentencesInDoc = new LinkedList();

	    /** Create a sentence string for every sentence in the document. **/
	    for(List<HasWord> sentence: processor) {
		
		// Take the words and put them together in a sentence.
		String sentenceToProcess = "";
		
		for (HasWord word : sentence) {
		    sentenceToProcess += word.word() + ' ';
		}
		
		sentencesInDoc.add(sentenceToProcess);
	    }

	    
	    // Rank the sentences in the document
	    // TODO: update the scores, boosting the rating for keywords that were actually selected.
	    List<Duple<String, Double>> topSentences = Autosummary.topSentences(sentencesInDoc, keywordScores);

	    System.out.println("\n\nGENERATED SUMMARY FOR FILE ="+file.toString() + "\n\n");
	    
	    
	    // Print out the 10 sentences
	    for(int i = 0; i < 10 && i < topSentences.size(); i++) {
		Duple<String, Double> iDuple = topSentences.get(i);
		
		System.out.println(iDuple.x + "\t" +iDuple.y);
	    }

	
	    
	}



	// ----------------------------------------------------
	
	    
	    
    }
    

    public static void main(String[] args) {
	
	/**  Test out the sentence rating function **/

	// Assuming that the first argument is the directory, and second was stopwords file
	String STOPWORD_FILE = args[1];
	String PATH = args[0];

	// Verify the arguments
	LOGGER.info("path = "+args[0]+", stopwords file = "+args[1]);
	
	try{
	    
	    Autosummary.autosummarize(PATH, STOPWORD_FILE);
	    
	}catch(IOException ioe) {
	    LOGGER.warning("Problem with reading!");


	}
	
	//	this.topSentences(
	

    }

    

}



class DupleComparator implements Comparator<Duple<String, Double>> {
    public int compare(Duple<String, Double> o1, Duple<String, Double> o2) {
	return o1.y.compareTo(o2.y);
    }
    

    public boolean equals(Object ob) {
	return (ob instanceof DupleComparator);
    }
}