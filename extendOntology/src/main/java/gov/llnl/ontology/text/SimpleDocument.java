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

package gov.llnl.ontology.text;

import java.util.Set;


/**
 * A simple struct based implementation of a {@link Document}.
 * @author Keith Stevens
 */
public class SimpleDocument implements Document {

    /**
     * The corpus form which this {@link SimpleDocument} came.
     */
    private final String corpusName;

    /**
     * The cleaned string text for this {@link SimpleDocument}.
     */
    private final String docText;

    /**
     * The uncleaned string text for this {@link SimpleDocument}.
     */
    private final String originalText;

    /**
     * The unique string key for this {@link SimpleDocument}.
     */
    private final String key;

    /**
     * The unique integer id for this {@link SimpleDocument}.
     */
    private final long id;

    /**
     * The string title for this {@link SimpleDocument}.
     */
    private final String title;

    /**
     * The set of category labels applied to this {@link Document}.
     */
    private final Set<String> categories;

    /**
     * Constructs a new {@link SimpleDocument} using the given data values.
     *
     * @param corpusName the name of the corpus that this document came from
     * @param docText the cleaned text for this document
     * @param key A string based key for this document
     * @param id A unique identifier for this key
     * @param title A title for the document
     */
    public SimpleDocument(String corpusName, 
                          String docText, 
                          String originalText,
                          String key, 
                          long id, 
                          String title,
                          Set<String> categories) {
        this.corpusName = corpusName;
        this.docText = docText;
        this.originalText = originalText;
        this.key = key;
        this.id = id;
        this.title = title;
        this.categories = categories;
    }

    /**
     * {@inheritDoc}
     */
    public String sourceCorpus() {
        return corpusName;
    }

    /**
     * {@inheritDoc}
     */
    public String rawText() {
        return docText;
    }

    /**
     * {@inheritDoc}
     */
    public String originalText() {
        return originalText;
    }

    /**
     * {@inheritDoc}
     */
    public String key() {
        return key;
    }

    /**
     * {@inheritDoc}
     */
    public long id() {
        return id;
    }

    /**
     * {@inheritDoc}
     */
    public String title() {
        return title;
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> categories() {
        return categories;
    }
}
