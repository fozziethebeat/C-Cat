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

package gov.llnl.ontology.text.sentsplit;

import gov.llnl.ontology.util.StreamUtil;

import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;

import opennlp.tools.util.Span;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.IOError;


/**
 * A wrapper around the {@link SentenceDetectorME} {@link SentenceDetector} so
 * that it can be loaded with a no argument constructor using a predefined
 * model.
 *
 * @author Keith Stevens
 */
public class OpenNlpMESentenceSplitter implements SentenceDetector {

    public static final String DEFAULT_MODEL =
        "models/OpenNLP/en-sent.bin";

    /**
     * The internal {@link SentenceDetector}
     */
    private final SentenceDetector detector;

    /**
     * Loads the model configuration from {@link #DEFAULT_MODEL}
     */
    public OpenNlpMESentenceSplitter() {
        this(DEFAULT_MODEL, true);
    }

    /**
     * Loads a {@link SentenceDetectorME} model from {@code modelPath}.  If
     * {@code loadFromJar} is true, the binary file will found within the
     * running class path.
     */
    public OpenNlpMESentenceSplitter(String modelPath, boolean loadFromJar) {
        try {
            InputStream in = (loadFromJar)
                ? StreamUtil.fromJar(this.getClass(), modelPath)
                : new FileInputStream(modelPath);
            detector = new SentenceDetectorME(new SentenceModel(in));
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String[] sentDetect(String sentence) {
        return detector.sentDetect(sentence);
    }

    /**
     * {@inheritDoc}
     */
    public Span[] sentPosDetect(String sentence) {
        return detector.sentPosDetect(sentence);
    }
}

