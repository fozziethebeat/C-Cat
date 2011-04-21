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


public class SentenceCountSpace{

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

    
    public SentenceSpace() throws IOException {
	this(false, new ConcurrentMap<String, Integer>(),  Matrices.getMatrixBuilderForSVD());

    }

    public SentenceSpace(boolean readHeaderToken, ConcurrentMap<String, Integer> termToIndex, 
			 MatrixBuilder termDocumentMatrixBuilder) throws IOException{
	
	this.readHeaderToken = readHeaderToken;
	this.termToIndex = termToIndex;
        termIndexCounter = new AtomicInteger(0);
        documentCounter = new AtomicInteger(0);
	
        this.termDocumentMatrixBuilder = termDocumentMatrixBuilder;
	
        wordSpace = null;

    }

    public void processDocument(BufferedReader document) throws IOException {
	
	// Create a Map that will store the sentence location count.
	Map<String,Integer> termSentence = new HashMap<String,Integer>(1000);

        // If the first token is to be interpreted as a document header read it.
        if (readHeaderToken)
            handleDocumentHeader(docCount, documentTokens.next());



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
		    termSentece.put(word.word(), sentenceCount);
		}
		
	    }
	}


	document.close();

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

        // Convert the Map count to a SparseArray

	// Create a column with all the known words
        SparseArray<Integer> documentColumn =  new SparseIntHashArray(totalNumberOfUniqueWords);

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


    public void setupMatrix(){
	
	MatrixFile processedSpace = getMatrixFile();
	wordSpace = MatrixIO.readMatrix(processedSpace.getFile(), processedSpace.getFormat());

    }


    String public getSpaceName() {

	return "SentenceCountSpace";
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


    /**
     * Subclasses should override this method if they need to utilize a header
     * token for each document.  Implementations of this method <b>must</b> be
     * thread safe.  The default action is a no-op.
     *
     * @param docIndex The document id assigned to the current document
     * @param documentName The name of the current document.
     */
    protected void handleDocumentHeader(int docIndex, String header) {
    }

    
}