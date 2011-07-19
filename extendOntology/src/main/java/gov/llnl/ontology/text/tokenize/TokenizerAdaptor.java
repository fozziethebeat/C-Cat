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

package gov.llnl.ontology.text.tokenize;

import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.util.Span;


/**
 * This Adaptor class simplifies extending functionality of an already existing
 * {@link Tokenizer}.  Subclasses can call the underlying {@link Tokenizer} and
 * then reprocess the the tokenized results.
 *
 * @author Keith Stevens
 */
public class TokenizerAdaptor implements Tokenizer {

    /**
     * The underlying {@link Tokenizer}
     */
    protected final Tokenizer tokenizer;

    /**
     * Creates a new {@link Tokenizer} that decorates the functionality of the
     * existing {@link Tokenizer}.
     *
     * @param tokenizer The {@link Tokenizer} to decorate
     */
    public TokenizerAdaptor(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    /**
     * {@inheritDoc}
     */
    public String[] tokenize(String sentence) {
        return tokenizer.tokenize(sentence);
    }

    /**
     * {@inheritDoc}
     */
    public Span[] tokenizePos(String sentence) {
        return tokenizer.tokenizePos(sentence);
    }
}
