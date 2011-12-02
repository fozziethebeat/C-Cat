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

import gov.llnl.ontology.text.Annotation;
import gov.llnl.ontology.text.Sentence;
import gov.llnl.ontology.wordnet.LinkedMockReader;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.List;


/**
 * @author Keith Stevens
 */
public class PageRankCentralityDisambiguationTest
        extends LeskWordSenseDisambiguationTest {

    public static final String[][] SYNSET_DATA =
    {
        { "cat.n.2", "the the the the the", "N" },
        { "cat.n.3", "cute lady", "N" },
        { "cat.n.1", "fluffy cute pet ", "N" },
        { "brown.n.1", "a color", "N"},
        { "brown.n.2", "cut but ugly pet", "N" },
        { "chicken.n.1", "fluffy cute beast", "N" },
        { "chicken.n.2", "a the", "N" },
        { "like.n.1", "fluffy machine pet that is not cute", "" },
    };

    public static final String[][] SYNSET_LINKS =
    {
        { "cat.n.1", "brown.n.1" },
        { "cat.n.1", "brown.n.2" },
        { "cat.n.1", "chicken.n.1" },
        { "cat.n.1", "chicken.n.2" },
        { "cat.n.2", "chicken.n.2" },
        { "cat.n.3", "brown.n.2" },
    };

    @Test public void testDisambiguation() {
        WordSenseDisambiguation wsdAlg = new PageRankCentralityDisambiguation();
        Sentence sentences = getSentences(TEST_SENTENCE, TEST_POS);
        LinkedMockReader reader = new LinkedMockReader(SYNSET_DATA);
        for (String[] synsetLink : SYNSET_LINKS)
            reader.connectSynsets(synsetLink[0], synsetLink[1], "r");

        wsdAlg.setup(reader);
        Sentence sent = wsdAlg.disambiguate(sentences);

        Sentence expected = sentences;
        assertEquals(expected.numTokens(), sent.numTokens());
        assertEquals(expected.start(), sent.start());
        assertEquals(expected.end(), sent.end());

        Annotation word = sent.getAnnotation(1);
        assertNotNull(word);
        assertEquals(SYNSET_DATA[2][0], word.sense());
    }
}
