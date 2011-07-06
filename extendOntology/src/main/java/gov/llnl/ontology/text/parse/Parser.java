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

package gov.llnl.ontology.text.parse;

import gov.llnl.ontology.util.StringPair;

import java.util.List;


/**
 * An interface for building dependency parse trees from raw text.
 *
 * @author Keith Stevens
 */
public interface Parser {

    /**
     * Returns a dependency parse tree formatted in the default CoNLL format.
     * The returned string may represent multiple parse tress, espeically if the
     * given document contains multiple sentences.  Each parse tree will be
     * padded with new lines.  If {@code header} is not {@code null}, then it
     * will be first line of each parse tree, otherwise only padded parse trees
     * will be returned.
     *
     * @param header The header text that should label each parsed tree
     * @param document The raw text to be parsed
     */
    String parseText(String header, String document);

    /**
     * Returns a dependency parse tree formatted in the default CoNLL format.
     * The returned string will contain only one parse tree If {@code header} is
     * not {@code null}, then it will be first line of the parse tree, otherwise
     * only the parse tree will be returned.
     *
     * @param header The header text that should label each parsed tree
     * @param sentence The part of speech tagged words in a sentence.
     */
    String parseText(String header, StringPair[] sentence);
}
