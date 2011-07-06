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

import gov.llnl.ontology.util.StreamUtil;

import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

import opennlp.tools.util.Span;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.IOError;


/**
 * @author Keith Stevens
 */
public class OpenNlpMETokenizer implements Tokenizer {

    public static final String DEFAULT_MODEL =
        "models/OpenNLP/en-token.bin";

    private final Tokenizer tokenizer;

    public OpenNlpMETokenizer() {
        this(DEFAULT_MODEL, true);
    }

    public OpenNlpMETokenizer(String modelPath, boolean loadFromJar) {
        try {
            InputStream in = (loadFromJar)
                ? StreamUtil.fromJar(this.getClass(), modelPath)
                : new FileInputStream(modelPath);
            tokenizer = new TokenizerME(new TokenizerModel(in));
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
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
