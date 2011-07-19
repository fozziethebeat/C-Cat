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

import edu.ucla.sspace.text.Stemmer;

import opennlp.tools.tokenize.Tokenizer;


/**
 * A decorator for stemming each token returned by an existing {@link
 * Tokenizer}, given a {@link Stemmer}.
 *
 * @author Keith Stevens
 */
public class StemmingTokenizer extends TokenizerAdaptor {

    /**
     * The {@link Stemmer} used to stem each tokenized word.
     */
    private final Stemmer stemmer;

    /**
     * Creates a new {@link StemmingTokenizer} that decorates the existing
     * {@link Tokenizer}.
     *
     * @param tokenizer A {@link Tokenizer} to decorate
     * @param stemmer The {@link Stemmer} used to stem each token
     */
    public StemmingTokenizer(Tokenizer tokenizer, Stemmer stemmer) {
        super(tokenizer);
        this.stemmer = stemmer;
    }

    /**
     * {@inheritDoc}
     */
    public String[] tokenize(String sentence) {
        String[] tokens = tokenizer.tokenize(sentence);
        for (int i = 0; i < tokens.length; ++i)
            tokens[i] = stemmer.stem(tokens[i]);
        return tokens;
    }
}
