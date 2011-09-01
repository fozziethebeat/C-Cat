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

import java.util.HashSet;


/**
 * A {@link DocumentReader} for the SenseEval 2007 corpus.  This automatically
 * removes the {@code head} tags from the document.  It uses the
 * instance name as the key, the title is just the keyterm.  The id is the
 * token index of the word that matches the title when both are stemmed.  It
 * does not generate any labels for a document.  
 *
 * </p>
 *
 * This is <b>not</b> thread safe.
 *
 * @author Keith Stevens
 */
public class SenseEval2007DocumentReader implements DocumentReader {

    public static final String CORPUS_NAME = "senseEval2007";

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
        return readDocument(doc, corpusName());
    }

    /**
     * {@inheritDoc}
     */
    public Document readDocument(String doc, String corpusName) {
        // Determine the indices for the instance id.
        int idStart = doc.indexOf("id=\"")+4;
        int idEnd = doc.indexOf("\" corpus");
        int docEnd = doc.lastIndexOf("</instance>");

        // Extract the instance id and the keyword.
        if (idStart < 0 || idEnd < 0)
            return null;

        String key = doc.substring(idStart, idEnd);
        String keyWord = key.split("\\.")[0];
        int headStart = doc.indexOf("<head>");
        int headEnd = doc.indexOf("</head>") + 7;
        if (headStart < 0 || headEnd < 0)
            return null;

        // Extract the raw text.
        int titleEnd = doc.indexOf(">");
        if (titleEnd < 0)
            return null;

        String docTextPre = doc.substring(titleEnd+1, headStart).trim();
        String docTextPost = doc.substring(headEnd, docEnd).trim();
        String text = docTextPre + " " + keyWord + " " + docTextPost;

        // The keyword is simply the token in the head tag, which comes after
        // the first block of text, so determine the number of tokens in that
        // and use it as the id.
        long id = docTextPre.split("\\s+").length;
        return new SimpleDocument(corpusName, text, doc, 
                                  key, id, keyWord, new HashSet<String>());
    }
}

