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

package gov.llnl.ontology.text.tag;

import gov.llnl.ontology.util.StreamUtil;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.postag.POSTaggerME;

import opennlp.tools.util.Sequence;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.IOError;

import java.util.List;


/**
 * @author Keith Stevens
 */
public class OpenNlpMEPOSTagger implements POSTagger {

    public static final String DEFAULT_MODEL =
        "models/OpenNLP/en-pos-maxent.bin";

    private final POSTagger tagger;

    public OpenNlpMEPOSTagger() {
        this(DEFAULT_MODEL, true);
    }

    public OpenNlpMEPOSTagger(String modelPath, boolean loadFromJar) {
        try {
            InputStream in = (loadFromJar)
                ? StreamUtil.fromJar(this.getClass(), modelPath)
                : new FileInputStream(modelPath);
            tagger = new POSTaggerME(new POSModel(in));
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    public List<String> tag(List<String> sentence) {
        return tagger.tag(sentence);
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    public String tag(String sentence) {
        return tagger.tag(sentence);
    }

    /**
     * {@inheritDoc}
     */
    public String[] tag(String[] sentence) {
        return tagger.tag(sentence);
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    public Sequence[] topKSequences(List<String> sentence) {
        return tagger.topKSequences(sentence);
    }

    /**
     * {@inheritDoc}
     */
    public Sequence[] topKSequences(String[] sentence) {
        return tagger.topKSequences(sentence);
    }
}
