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

package gov.llnl.ontology.wordnet.wsd;

import gov.llnl.ontology.text.Sentence;
import gov.llnl.ontology.util.AnnotationUtil;
import gov.llnl.ontology.wordnet.GenericMockReader;

import com.google.common.collect.Lists;

import edu.stanford.nlp.pipeline.Annotation;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.List;


/**
 * @author Keith Stevens
 */
public class LeskWordSenseDisambiguationTest {

    public static final String TEST_SENTENCE =
        "the cat is brown and chicken like";
    public static final String TEST_POS =
        "D N D N C N Q";

    public static final String[][] SYNSET_DATA =
    {
        { "cat.n.2", "fluffy machine pet that is not cute", "N" },
        { "cat.n.3", "cute lady", "N" },
        { "cat.n.1", "fluffy cute pet", "N" },
        { "brown.n.1", "a color", "N"},
        { "brown.n.2", "cut but ugly pet", "N" },
        { "chicken.n.1", "fluffy cute beast", "N" },
        { "like.n.1", "fluffy machine pet that is not cute", "" },
    };

    protected List<Sentence> getSentences(String sentence, String posSent) {
        String[] tokens = sentence.split("\\s+");
        String[] pos = posSent.split("\\s+");

        Sentence sent = new Sentence(0, 1, tokens.length);
        for (int i = 0; i < tokens.length; ++i) {
            Annotation annot = new Annotation(tokens[i]);
            AnnotationUtil.setPos(annot, pos[i]);
            sent.addAnnotation(i, annot);
        }

        return Collections.singletonList(sent);
    }

    @Test public void testDisambiguation() {
        WordSenseDisambiguation wsdAlg = new LeskWordSenseDisambiguation();
        List<Sentence> sentences = getSentences(TEST_SENTENCE, TEST_POS);
        wsdAlg.setup(new GenericMockReader(SYNSET_DATA));
        wsdAlg.disambiguate(sentences);

        assertEquals(1, sentences.size());
        boolean foundCat = false;
        for (Annotation annot : sentences.get(0)) {
            if (AnnotationUtil.word(annot).equals("cat")) {
                foundCat = true;
                assertEquals(SYNSET_DATA[0][0],
                             AnnotationUtil.wordSense(annot));
            }
        }
        assertTrue(foundCat);
    }
}
