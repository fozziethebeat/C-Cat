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


/**
 * A {@link DocumentReader} for the SemEval2010 test corpus.  This automatically
 * removes the {@code TargetSentence} tags from the document.  It uses the
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
public class SemEval2010TestDocumentReader
        extends SemEval2010TrainDocumentReader {

    public static final String CORPUS_NAME = "semeval2010_test";

    /**
     * Returns {@link #CORPUS_NAME}
     */
    public String corpusName() {
        return CORPUS_NAME;
    }
}
