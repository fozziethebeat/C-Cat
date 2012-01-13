package gov.llnl.ontology.wordnet.castanet;

import gov.llnl.ontology.text.Document;

import edu.ucla.sspace.matrix.Transform;
import edu.ucla.sspace.text.IteratorFactory;
import edu.ucla.sspace.text.StringDocument;
import edu.ucla.sspace.util.BoundedSortedMap;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vsm.VectorSpaceModel;

import com.google.common.collect.Lists;

import java.io.IOError;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * @author Keith Stevens
 */
public class TransformKeywordExtractor implements KeywordExtractor {

    private final Transform transform;

    public TransformKeywordExtractor(Transform transform) {
        this.transform = transform;
    }

    /**
     * {@inheritDoc}
     */
    public Keyword[][] extractKeywords(Iterator<Document> docs,
                                       int numWords,
                                       String stopWordFile) {
		// If we have a stop word list, then use it.
		if (!stopWordFile.equals("")) {
			System.setProperty(IteratorFactory.TOKEN_FILTER_PROPERTY,
			                   "exclude=" + stopWordFile);
			IteratorFactory.setProperties(System.getProperties());
        }

        int numDocs = 0;
        VectorSpaceModel vsm = null;
        try {
            vsm = new VectorSpaceModel(transform);
            while (docs.hasNext()) {
                vsm.processDocument(
                        new StringDocument(docs.next().rawText()).reader());
                numDocs++;
            }
            vsm.processSpace(System.getProperties());
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }

        List<BoundedSortedMap<Double, String>> topKeyWords = Lists.newArrayList();
        for (int docIndex = 0; docIndex < numDocs; ++docIndex)
            topKeyWords.add(new BoundedSortedMap<Double, String>(numWords));
            
        for (String word : vsm.getWords()) {
            SparseDoubleVector wordVec = (SparseDoubleVector) vsm.getVector(word);
            for (int docIndex : wordVec.getNonZeroIndices())
                topKeyWords.get(docIndex).put(wordVec.get(docIndex), word);
        }

        Keyword[][] documentKeywords = new Keyword[numDocs][numWords];
        int docId = 0;
        for (BoundedSortedMap<Double, String> docKeyWords : topKeyWords) {
            int wordId = 0;
            for (Map.Entry<Double, String> e : docKeyWords.entrySet())
                documentKeywords[docId][wordId++] = new Keyword(e.getValue(), e.getKey());
            docId++;
        }

        return documentKeywords;
    }
}
