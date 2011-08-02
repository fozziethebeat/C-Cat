/*
 * Copyright (c) 2011, Lawrence Livermore National Security, LLC. Produced at
 * the Lawrence Livermore National Laboratory. Written by Keith Stevens,
 * kstevens@cs.ucla.edu OCEC-10-073 All rights reserved. 
 *
 * This file is part of the C-Cat package and is covered under the terms and
 * conditions therein.
 *
 * The C-Cat package is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation and distributed hereunder to you.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND NO REPRESENTATIONS OR WARRANTIES,
 * EXPRESS OR IMPLIED ARE MADE.  BY WAY OF EXAMPLE, BUT NOT LIMITATION, WE MAKE
 * NO REPRESENTATIONS OR WARRANTIES OF MERCHANT- ABILITY OR FITNESS FOR ANY
 * PARTICULAR PURPOSE OR THAT THE USE OF THE LICENSED SOFTWARE OR DOCUMENTATION
 * WILL NOT INFRINGE ANY THIRD PARTY PATENTS, COPYRIGHTS, TRADEMARKS OR OTHER
 * RIGHTS.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package gov.llnl.ontology.text.corpora;

import gov.llnl.ontology.text.Document;
import gov.llnl.ontology.text.DocumentReader;
import gov.llnl.ontology.text.SimpleDocument;

import edu.ucla.sspace.text.EnglishStemmer;
import edu.ucla.sspace.text.Stemmer;

import java.util.HashSet;


/**
 * A {@link DocumentReader} for the SemEval2010 test corpus.  It uses the
 * instance name as the key, the title is be just the keyterm.  The id is the
 * token index of the word that matches the title when both are stemmed.  It
 * does not generate any labels for a document.  
 *
 * </p>
 *
 * This is <b>not</b> thread safe.
 *
 * @author Keith Stevens
 */
public class SemEval2010TrainDocumentReader implements DocumentReader {

    public static final String CORPUS_NAME = "semeval2010_train";

    /**
     * An {@link EnglishStemmer} used to determine which context word is the
     * real focus word.
     */
    private final Stemmer stemmer;

    /**
     * Constructs a new {@link SemEval2010TrainDocumentReader}.
     */
    public SemEval2010TrainDocumentReader() {
        stemmer = new EnglishStemmer();
    }

    /**
     * Returns {@link #CORPUS_NAME}
     */
    public String corpusName() {
        return CORPUS_NAME;
    }

    /**
     * {@inheritDoc}
     */
    public Document readDocument(String doc) {
        // Determine where the tag starts and ends.
        int titleStart = doc.indexOf("<")+1;
        int titleEnd = doc.indexOf(">");
        int textEnd = doc.lastIndexOf("<");

        // Extract the instance name and the keyword.
        String key = doc.substring(titleStart, titleEnd);
        String keyWord = stemmer.stem(key.split("\\.")[0]);

        // Get the raw text.
        String text = doc.replaceAll("<.*?>", "");

        // Determine which token, if split by whitespace, matches the instance
        // keyword.
        String[] tokens = text.split("\\s+");
        long id = 0;
        for (int i =0; i < tokens.length; ++i)
            if (keyWord.equals(stemmer.stem(tokens[i])))
                id = i;

        return new SimpleDocument(corpusName(), text, doc, 
                                  key, id, keyWord, new HashSet<String>());
    }
}
