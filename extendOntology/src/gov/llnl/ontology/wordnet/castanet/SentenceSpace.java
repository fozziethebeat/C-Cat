package gov.llnl.ontology.wordnet.castanet;



import edu.ucla.sspace.matrix.Matrices;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.MatrixFile;
import edu.ucla.sspace.matrix.MatrixBuilder;
import edu.ucla.sspace.matrix.MatrixIO;
import edu.ucla.sspace.matrix.MatrixIO.Format;
import edu.ucla.sspace.matrix.SVD;
import edu.ucla.sspace.matrix.Transform;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.util.*;
import edu.ucla.sspace.util.SparseArray;
import edu.ucla.sspace.util.SparseIntHashArray;

import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.CompactSparseVector;

// import edu.sspace.common.*;




import java.io.BufferedReader;
import java.io.File;
import java.io.IOError;
import java.io.IOException;


import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import java.util.concurrent.atomic.AtomicInteger;

import java.util.logging.Logger;



import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.ling.HasWord;




public class SentenceSpace{

   protected static final Logger LOG = 
        Logger.getLogger(SentenceSpace.class.getName());

    /**
     * A mapping from a word to the row index in the that word-document matrix
     * that contains occurrence counts for that word.
     * TODO: Replace this with a basis mapping.
     */
    private final ConcurrentMap<String,Integer> termToIndex;

    /**
     * The counter for recording the current, largest word index in the
     * word-document matrix.  Subclasses can use this for any reporting.
     */
    protected final AtomicInteger termIndexCounter;
    
    /**
     * The counter for recording the current number of documents observed.
     * Subclasses can use this for any reporting.
     */
    protected final AtomicInteger documentCounter;


    /**
     * The builder used to construct the term-document matrix as new documents
     * are processed.
     */
    private final MatrixBuilder termDocumentMatrixBuilder;


    /**
     * The word space of the term document based word space model.  If the word
     * space is reduced, it is the left factor matrix of the SVD of the
     * word-document matrix.  This matrix is only available after the {@link
     * #processSpace(Transform) processSpace}
     * method has been called.
     */
    protected Matrix wordSpace;

    /**
     * Keeps track of how many sentences are in a document. The location of the sentence count 
     * corresponds to which document in the matrix row. 
     */
    private List<Integer> sentenceCounter;

    
    public SentenceSpace() throws IOException {
	this(new ConcurrentHashMap<String, Integer>(),  Matrices.getMatrixBuilderForSVD());

    }

    public SentenceSpace(ConcurrentMap<String, Integer> termToIndex, 
			 MatrixBuilder termDocumentMatrixBuilder) throws IOException{
	
	this.termToIndex = termToIndex;
        termIndexCounter = new AtomicInteger(0);
        documentCounter = new AtomicInteger(0);
	
        this.termDocumentMatrixBuilder = termDocumentMatrixBuilder;
	
        wordSpace = null;
	
	sentenceCounter = new ArrayList();
    }

    public void processDocument(BufferedReader document) throws IOException {
	
	// Create a Map that will store the sentence location count.
	// For every word stored in this Map (the key), the value is what is the sentence location
	// for this word
	Map<String,Integer> termSentence = new HashMap<String,Integer>(1000);

	// Tokenize the document by sentences.
	DocumentPreprocessor processor = new DocumentPreprocessor(document);
	
	int sentenceCount = 0;


	for(List<HasWord> sentence : processor) {
	    
	    sentenceCount++;
	   

	    // Go through each word and record what sentence number they are from.
	    for (HasWord word : sentence) {
		
		// We're looking for the first occurrence of the word. So if theres already a record then we don't want it.
		if(!termSentence.containsKey(word.word())){
		    addTerm(word.word());
		    termSentence.put(word.word(), sentenceCount);
		}
		
	    }
	}


	document.close();

	sentenceCounter.add(new Integer(sentenceCount));

	// Check that we actually loaded in some terms before we increase the
        // documentIndex. This is done after increasing the document count since
        // some configurations may need the document order preserved, for
        // example, if each document corresponds to some cluster assignment.
        if (termSentence.isEmpty())
            return;


	// Now that we have a count of what words appear in what sentence, we will write this to the Matrix
        // Get the total number of terms encountered so far, including any new
        // unique terms found in the most recent document
        int totalNumberOfUniqueWords = termIndexCounter.get();

	// Create a column with all the known words
        SparseArray<Integer> documentColumn =  new SparseIntHashArray(totalNumberOfUniqueWords);

	// For every unique word encountered, add it to the column of the matrix

        for (Map.Entry<String,Integer> e : termSentence.entrySet())
            documentColumn.set(termToIndex.get(e.getKey()), e.getValue());

        // Update the term-document matrix with the results of processing the
        // document.
        termDocumentMatrixBuilder.addColumn(documentColumn);

    }

    public MatrixFile getMatrixFile() {
	
	// No more messing with the matrix data
	termDocumentMatrixBuilder.finish();

	File termDocumentMatrix = termDocumentMatrixBuilder.getFile();

	return new MatrixFile(termDocumentMatrix, termDocumentMatrixBuilder.getMatrixFormat());
	
    }


    public void setupMatrix() throws IOException{
	
	MatrixFile processedSpace = getMatrixFile();
	wordSpace = MatrixIO.readMatrix(processedSpace.getFile(), processedSpace.getFormat());

    }


    public String getSpaceName() {

	return "SentenceSpace";
    }


    

    /**
     * {@inheritDoc}
     */
    public Vector getVector(String word) {
        // determine the index for the word
        Integer index = termToIndex.get(word);
        
        return (index == null)
            ? null
            : wordSpace.getRowVector(index.intValue());
    }




    /**
     * Adds the term to the list of terms and gives it an index, or if the term
     * has already been added, does nothing.
     * TODO: Replace this with a basis mapping.
     */
    private void addTerm(String term) {
        Integer index = termToIndex.get(term);

        if (index == null) {

            synchronized(this) {
                // recheck to see if the term was added while blocking
                index = termToIndex.get(term);
                // if some other thread has not already added this term while
                // the current thread was blocking waiting on the lock, then add
                // it.
                if (index == null) {
                    index = Integer.valueOf(termIndexCounter.getAndIncrement());
                    termToIndex.put(term, index);
                }
            }
        }
    }
    
    public Set<String> getWords() {
        return Collections.unmodifiableSet(termToIndex.keySet());
    }

    public int getSentenceCountFor(int document) {
	if (document < 0 || document >= sentenceCounter.size()) 
	    return -1;
	else
	    return sentenceCounter.get(document);
    }



    private DoubleVector calculateScoreForDocument(int doc) {
	
	double docDouble = (double) doc;
	DoubleVector documentWords = wordSpace.getColumnVector( doc);
	    
	// Go through every word in the document and perform the right calculations
	Integer sentenceMax = sentenceCounter.get(new Integer(doc));

	DoubleVector newScores = new CompactSparseVector(documentWords.length());
	
	
	for(int j = 0; j < documentWords.length(); j++) {
		
	    double currentSentenceLoc = documentWords.get(j);
	    double newScore = (1 + sentenceMax.doubleValue() - currentSentenceLoc) / sentenceMax.doubleValue();
	    
	    newScores.set(j, newScore);
	   
	}

	
	return newScores;


    }

    private void printScores() {
	


	for(String word : getWords()) {
	    
	    Vector wordVector = getVector(word);
	    
	    System.out.print(word +": \t\t");
	    for (int j = 0; j < wordVector.length(); j++) {
		
		System.out.print(wordVector.getValue(j) +"\t");
		
		
	    }

	    System.out.println();


	}


    }


    /*** 
     *  Calculate the scores for each word's sentence location based on the Sakai Et Al. method. 
     */
    public void calculateSentenceScore() {
	
	// Go down the column, normalize and add everything.
	
	// Get the column (all the words in a document).
	
	for(int i = 0; i < wordSpace.columns(); i++) {
	    
	    DoubleVector newScores = calculateScoreForDocument(i);
	    wordSpace.setColumn(i, newScores);

	}

	// DEBUG
	printScores();
    }




}