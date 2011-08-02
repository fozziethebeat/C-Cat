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

import gov.llnl.ontology.text.Document;
import gov.llnl.ontology.text.DocumentReader;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class SemEval2010TestDocumentReaderTest {

    public static final String TEST_SENT = 
        "<class.n.4>It is reported that , the joint-investment agreement between the US Westinghouse Company and the Shanghai Electric Group included the joint manufacture of key components of 300,000 kW thermal power generating units , and simultaneously expanding co-operation in various areas of 600,000 kW units .  <TargetSentence>Co-operation between the US General Electric Capital Company and the Shanghai Electric Company included investment in building 4 sets of 100,000 kW class turbine generating units .  </TargetSentence>This afternoon , Brown also visited Shanghai 's AMP Linking Company , Ltd. , which was jointly invested and built by the Shanghai Electric Component Company and the US AMP China Limited Company , situated in Shanghai 's Caohejing high-tech development district .  </class.n.4>";

    @Test public void testRead() {
        DocumentReader reader = new SemEval2010TestDocumentReader();
        Document doc = reader.readDocument(TEST_SENT);
        assertEquals("class.n.4", doc.key());
        assertFalse(doc.rawText().contains("class.n.4"));
        assertFalse(doc.rawText().contains("TargetSentence"));
        assertTrue(doc.rawText().contains("Ltd."));
        assertEquals("semeval2010_test", doc.sourceCorpus());
    }
}
