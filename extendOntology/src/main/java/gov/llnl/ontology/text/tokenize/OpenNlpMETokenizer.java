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

import java.io.IOError;
import java.io.IOException;


/**
 * A wrapper around the {@link TokenizerME} {@link Tokenizer} so that it can be
 * loaded with a no argument constructor using a predefined model.
 *
 * @author Keith Stevens
 */
public class OpenNlpMETokenizer extends TokenizerAdaptor {

    public static final String DEFAULT_MODEL =
        "models/OpenNLP/en-token.bin";

    /**
     * Loads the model configuration from {@link #DEFAULT_MODEL}
     */
    public OpenNlpMETokenizer() {
        this(DEFAULT_MODEL, true);
    }

    /**
     * Loads a {@link TokenizerME} model from {@code modelPath}.  If {@code
     * loadFromJar} is true, the binary file will found within the running class
     * path.
     */
    public OpenNlpMETokenizer(String modelPath, boolean loadFromJar) {
        super(loadModel(modelPath, loadFromJar));
    }

    /**
     * Returns a {@link Tokenizer} stored in the file specified by {@code
     * modelPath}.  This is static so that the constructor can simply pass the
     * loaded {@link Tokenizer} to the constructor of the {@link
     * TokenizerAdaptor}.
     */
    public static Tokenizer loadModel(String modelPath, boolean loadFromJar) {
        try {
            return new TokenizerME(new TokenizerModel(
                    (loadFromJar)
                    ? StreamUtil.fromJar(OpenNlpMETokenizer.class, modelPath)
                    : StreamUtil.fromPath(modelPath)));
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }
}
