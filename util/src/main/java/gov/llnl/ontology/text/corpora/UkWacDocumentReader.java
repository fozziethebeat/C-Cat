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

import gov.llnl.ontology.text.DocumentReader;
import gov.llnl.ontology.text.SimpleDocument;

import org.apache.commons.lang3.StringEscapeUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.HashSet;


/**
 * A {@link DocumentReader} for the parsed UkWac corpus.  Documents are expected
 * to deliminated with "<text>" tags in an xml format.  Each sentence is
 * expected to be in the UkWac CoNLL format.  The url in the {@code id}
 * attribute of {@code text} is the key and text, and it's hash value is the id
 * for each document.
 *
 * </p>
 *
 * This is <b>not</b> thread safe.
 *
 * @author Keith Stevens
 */
public class UkWacDocumentReader implements DocumentReader {

    public static final String CORPUS_NAME = "ukwac";

    /**
     * Returns {@link #CORPUS_NAME}
     */
    public String corpusName() {
        return CORPUS_NAME;
    }

    /**
     * {@inheritDoc}
     */
    public gov.llnl.ontology.text.Document readDocument(String doc) {
        String[] lines = doc.split("\\n");

        // Find the title.
        int titleStart = lines[0].indexOf("id=\"")+4;
        int titleEnd = lines[0].lastIndexOf("\">");
        String key = lines[0].substring(titleStart, titleEnd);
        long id = key.hashCode();
        
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i < lines.length-1; ++i) {
            // Skip empty lines and xml tags.
            if (lines[i].length() == 0 ||
                lines[i].endsWith("s>"))
                continue;

            lines[i] = StringEscapeUtils.unescapeHtml4(lines[i]);
            String[] toks = lines[i].split("\\s+");
            builder.append(toks[0]).append(" ");
        }

        return new SimpleDocument(corpusName(), builder.toString(), doc, 
                                  key, id, key, new HashSet<String>());
    }
}
