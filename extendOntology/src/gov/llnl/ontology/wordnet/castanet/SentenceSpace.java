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

import edu.ucla.sspace.util.LoggerUtil;
import edu.ucla.sspace.util.SparseArray;
import edu.ucla.sspace.util.SparseIntHashArray;

import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.Vector;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOError;
import java.io.IOException;

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

import edu.sspace.common.SemanticSpace;


public class SentenceSpace implements SemanticSpace{

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
     * If true, the first token in each document is considered to be a document
     * header.
     */
    private final boolean readHeaderToken;

    /**
     * The word space of the term document based word space model.  If the word
     * space is reduced, it is the left factor matrix of the SVD of the
     * word-document matrix.  This matrix is only available after the {@link
     * #processSpace(Transform) processSpace}
     * method has been called.
     */
    protected Matrix wordSpace;

    

    public SentenceSpace(MatrixBuilder termDocumentMatrixBuilder) throws IOException{
	
	this.termToIndex = termToIndex;
        termIndexCounter = new AtomicInteger(0);
        documentCounter = new AtomicInteger(0);
	
        this.termDocumentMatrixBuilder = termDocumentMatrixBuilder;
	
        wordSpace = null;

    }

    public void processDocument(BufferedReader document) throws IOException {
	
	// Create a matrix


	// Create a Map that will store the sentence location count.
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
		    
		    termSentece.put(word.word(), sentenceCount);
		}

		

	    }
	    
	    

	    
	}

	

    }

    String public getSpaceName() {

	return "SentenceSpace";
    }

    String Vector getVector(String word) {
	
    }

    
}