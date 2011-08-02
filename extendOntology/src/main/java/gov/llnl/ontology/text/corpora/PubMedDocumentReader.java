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
import gov.llnl.ontology.text.SimpleDocument;

import com.google.common.collect.Sets;

import java.io.IOError;
import java.io.IOException;
import java.io.StringReader;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * A {@link DocumentReader} for the PubMed corpus.  PubMed is formatted as a
 * series of documents in a single xml file.  this {@link DocumentReader} works
 * as a {@link DefaultHandler} for the {@link SAXParser} and will read one full
 * document per call to {@link #readDocument}.  Text in {@code NameOfSubstance}
 * tags are the document labels, text in {@code ArticleTitle} is the title, text
 * in {@code PMID} serves as the id and key value, and text in {@code Abstract}
 * is the raw document text.
 * 
 * </p>
 *
 * This is <b>not</b> thread safe.
 *
 * @author Keith Stevens
 */
public class PubMedDocumentReader extends DefaultHandler 
                                  implements DocumentReader {

   /**
     * The set of abstract text tags which can be ignored 
     */
    private static Set<String> ignorableAbstractTags = 
        Sets.newHashSet("CopyrightInformation");

    /**
     * Boolean markers indicating where in the xml document the parser is.
     */
    private boolean inTitle;
    private boolean inAbstract;
    private boolean inIgnoreAbstract;
    private boolean inChemicalName;
    private boolean inPMID;

    /**
     * The raw abstract text of the document.
     */
    private String docText;

    /**
     * The pubmed id, as a string.
     */
    private String key;

    /**
     * The pubmed id, as a long.
     */
    private long id;

    /**
     * The abstract's title.
     */
    private String title;

    /**
     * The set of chemical names associated with the abstract.
     */
    private Set<String> labels = Sets.newHashSet();

    /**
     * A simple builder for collecting text in tags.
     */
    private StringBuilder b;

    /**
     * The internal xml parser.
     */
    private SAXParser saxParser;

    /**
     * Creates a new {@link PubMedDocumentReader}
     */
    public PubMedDocumentReader() {
        b = new StringBuilder();
        SAXParserFactory saxfac = SAXParserFactory.newInstance();
        saxfac.setValidating(false);

        // Ignore a number of xml features, like dtd grammars and validation.
        try {
            saxfac.setFeature("http://xml.org/sax/features/validation", false);
            saxfac.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            saxfac.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            saxfac.setFeature("http://xml.org/sax/features/external-general-entities", false);
            saxfac.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            saxParser = saxfac.newSAXParser();
        } catch (SAXNotRecognizedException e1) {
            throw new RuntimeException(e1);
        }catch (SAXNotSupportedException e1) {
            throw new RuntimeException(e1);
        } catch (ParserConfigurationException e1) {
            throw new RuntimeException(e1);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Document readDocument(String originalText) {
        // Reset all the tracking parameters.
        inAbstract = false;
        inTitle = false;
        inPMID = false;
        inTitle = false;
        labels.clear();
        b.setLength(0);

        // Parse!
        try {
            saxParser.parse(new InputSource(new StringReader(originalText)), this);
        } catch (SAXException se) {
            throw new RuntimeException(se);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }

        // Return the discovered content.
        return new SimpleDocument("pubmed", docText, originalText,
                                  key, id, title, labels);
    }

    @Override
    public void startElement(String uri, String localName, String name, Attributes atts)
            throws SAXException {
        if ("Abstract".equals(name)) {
            inAbstract = true;
        } else if (inAbstract) {
            if ("p".equals(name)) {
                b.append("\n\n");
            } else if (ignorableAbstractTags.contains(name)) {
                inIgnoreAbstract = true;
            }
        } else if ("NameOfSubstance".equals(name)) {
            inChemicalName = true;
        } else if ("PMID".equals(name)) {
            inPMID = true;
        } else if ("ArticleTitle".equals(name)) {
            inTitle = true;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        String s = new String(ch, start, length);
        if (inTitle || inPMID || inChemicalName ||
            (inAbstract && !inIgnoreAbstract))
            b.append(s);
    }

    @Override
    public void endElement(String uri, String localName, String name)
            throws SAXException {
        if ("Abstract".equals(name)) {
            inAbstract = false;
            docText = b.toString().trim();
            b.setLength(0);
        } if (inAbstract && ignorableAbstractTags.contains(name)) {
            inIgnoreAbstract = false;
        } else if ("ArticleTitle".equals(name)) {
            inTitle = false;
            title = b.toString().trim();
            b.setLength(0);
        } else if ("PMID".equals(name)) {
            inPMID = false;
            key = b.toString().trim();
            id = Long.parseLong(key);
            b.setLength(0);
        } else if ("NameOfSubstance".equals(name)) {
            inChemicalName = false;
            labels.add(b.toString().trim());
            b.setLength(0);
        }
    }
}
