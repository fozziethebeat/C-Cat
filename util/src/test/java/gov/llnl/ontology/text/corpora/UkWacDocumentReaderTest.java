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
public class UkWacDocumentReaderTest {

    public static final String INPUT =
        "<text id=\"ukwac:http://observer.html\">\n" +
        "<s>\n" +
        "Hooligans   hooligan    NNS 1   4   NMOD\n" +
        ",   ,   ,   2   4   P\n" +
        "unbridled   unbridled   JJ  3   4   NMOD\n" +
        "passion passion NN  4   0   ROOT\n" +
        "-   -   :   5   4   P\n" +
        "and and CC  6   4   CC\n" +
        "no  no  DT  7   9   NMOD\n" +
        "executive   executive   JJ  8   9   NMOD\n" +
        "boxes   box NNS 9   4   COORD\n" +
        ".   .   SENT    10  0   ROOT\n" +
        "</s>\n" +
        "<s>\n" +
        "Portsmouth  Portsmouth  NP  1   2   SBJ\n" +
        "are are VBP 2   0   ROOT\n" +
        "a   a   DT  3   4   NMOD\n" +
        "reminder    reminder    NN  4   2   PRD\n" +
        "of  of  IN  5   4   NMOD\n" +
        "how how WRB 6   8   ADV\n" +
        "football    football    NN  7   8   SBJ\n" +
        "used    use VVD 8   5   PMOD\n" +
        "to  to  TO  9   10  VMOD\n" +
        "be  be  VB  10  8   OBJ\n" +
        "Vi&#7879;t  Vi&#7879;t  NP  16  0   ROOT\n" +
        "the the DT  12  14  NMOD\n" +
        "</s>\n" +
        "</text>";
        
    @Test
    public void testReader() {
        DocumentReader reader = new UkWacDocumentReader();
        Document doc = reader.readDocument(INPUT);
        assertEquals("ukwac:http://observer.html", doc.key());
        assertEquals("ukwac:http://observer.html", doc.title());
        assertEquals(22, doc.rawText().split("\\s+").length);
        assertEquals(INPUT, doc.originalText());
        assertEquals(UkWacDocumentReader.CORPUS_NAME, doc.sourceCorpus());
    }
}
