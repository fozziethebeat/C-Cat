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
public class PubMedDocumentReaderTest {

    public static final String DOCUMENT_ONE =
        "<MedlineCitation Owner=\"NLM\" Status=\"MEDLINE\">" +
        "<PMID>12345</PMID>" +
        "<DateCreated>" +
        "<Year>111</Year>" +
        "<Month>-1</Month>" +
        "<Day>66</Day>" +
        "</DateCreated>" +
        "<DateCompleted>" +
        "<Year>2111</Year>" +
        "<Month>13</Month>" +
        "<Day>-1</Day>" +
        "</DateCompleted>" +
        "<Article PubModel=\"Print\">" +
        "<Journal>" +
        "<ISSN IssnType=\"Print\">xxxx-xxx</ISSN>" +
        "<JournalIssue CitedMedium=\"Print\">" +
        "<Volume>55</Volume>" +
        "<PubDate>" +
        "<Year>2009</Year>" +
        "</PubDate>" +
        "</JournalIssue>" +
        "<Title>Something about chickens</Title>" +
        "<ISOAbbreviation>chicken chicken chicken</ISOAbbreviation>" +
        "</Journal>" +
        "<ArticleTitle>CHICKEN</ArticleTitle>" +
        "<Pagination>" +
        "<MedlinePgn>1-99</MedlinePgn>" +
        "</Pagination>" +
        "<Abstract>" +
        "<AbstractText>And once there was a chicken.  </AbstractText>" +
        "</Abstract>" +
        "<Affiliation>Chicken Inc.</Affiliation>" +
        "<AuthorList CompleteYN=\"Y\">" +
        "<Author ValidYN=\"Y\">" +
        "<LastName>Christensen</LastName>" +
        "<ForeName>Lars P</ForeName>" +
        "<Initials>LP</Initials>" +
        "</Author>" +
        "</AuthorList>" +
        "<Language>eng</Language>" +
        "<PublicationTypeList>" +
        "<PublicationType>Journal Article</PublicationType>" +
        "<PublicationType>Review</PublicationType>" +
        "</PublicationTypeList>" +
        "</Article>" +
        "<MedlineJournalInfo>" +
        "<Country>United States</Country>" +
        "<MedlineTA>Adv Food Nutr Res</MedlineTA>" +
        "<NlmUniqueID>9001271</NlmUniqueID>" +
        "</MedlineJournalInfo>" +
        "<ChemicalList>" +
        "<Chemical>" +
        "<RegistryNumber>0</RegistryNumber>" +
        "<NameOfSubstance>Anti-Chicken Agents</NameOfSubstance>" +
        "</Chemical>" +
        "<Chemical>" +
        "<RegistryNumber>0</RegistryNumber>" +
        "<NameOfSubstance>Non-geese</NameOfSubstance>" +
        "</Chemical>" +
        "</ChemicalList>" +
        "<CitationSubset>IM</CitationSubset>" +
        "<MeshHeadingList>" +
        "<MeshHeading>" +
        "<DescriptorName MajorTopicYN=\"N\">Pro Chicken Authorities</DescriptorName>" +
        "<QualifierName MajorTopicYN=\"N\">featured use</QualifierName>" +
        "</MeshHeading>" +
        "</MeshHeadingList>" +
        "<NumberOfReferences>348</NumberOfReferences>" +
        "</MedlineCitation>";

    public static final String DOCUMENT_TWO =
        "<MedlineCitation Owner=\"NLM\" Status=\"MEDLINE\">" +
        "<PMID>62345</PMID>" +
        "<DateCreated>" +
        "<Year>111</Year>" +
        "<Month>-1</Month>" +
        "<Day>66</Day>" +
        "</DateCreated>" +
        "<DateCompleted>" +
        "<Year>2111</Year>" +
        "<Month>13</Month>" +
        "<Day>-1</Day>" +
        "</DateCompleted>" +
        "<Article PubModel=\"Print\">" +
        "<Journal>" +
        "<ISSN IssnType=\"Print\">xxxx-xxx</ISSN>" +
        "<JournalIssue CitedMedium=\"Print\">" +
        "<Volume>55</Volume>" +
        "<PubDate>" +
        "<Year>2009</Year>" +
        "</PubDate>" +
        "</JournalIssue>" +
        "<Title>Something not about chickens</Title>" +
        "<ISOAbbreviation>colorful chickens infect america</ISOAbbreviation>" +
        "</Journal>" +
        "<ArticleTitle>CHICKENS ATTACK</ArticleTitle>" +
        "<Pagination>" +
        "<MedlinePgn>1-99</MedlinePgn>" +
        "</Pagination>" +
        "<Abstract>" +
        "<AbstractText>A flock of chickens have attacked new york.</AbstractText>" +
        "</Abstract>" +
        "<Affiliation>Chicken Inc.</Affiliation>" +
        "<AuthorList CompleteYN=\"Y\">" +
        "<Author ValidYN=\"Y\">" +
        "<LastName>Christensen</LastName>" +
        "<ForeName>Lars P</ForeName>" +
        "<Initials>LP</Initials>" +
        "</Author>" +
        "</AuthorList>" +
        "<Language>eng</Language>" +
        "<PublicationTypeList>" +
        "<PublicationType>Journal Article</PublicationType>" +
        "<PublicationType>Review</PublicationType>" +
        "</PublicationTypeList>" +
        "</Article>" +
        "<MedlineJournalInfo>" +
        "<Country>United States</Country>" +
        "<MedlineTA>Adv Food Nutr Res</MedlineTA>" +
        "<NlmUniqueID>9001271</NlmUniqueID>" +
        "</MedlineJournalInfo>" +
        "<ChemicalList>" +
        "<Chemical>" +
        "<RegistryNumber>0</RegistryNumber>" +
        "<NameOfSubstance>Human-Chicken Resistance</NameOfSubstance>" +
        "</Chemical>" +
        "<Chemical>" +
        "<RegistryNumber>0</RegistryNumber>" +
        "<NameOfSubstance>Pro-Fowl</NameOfSubstance>" +
        "</Chemical>" +
        "</ChemicalList>" +
        "<CitationSubset>IM</CitationSubset>" +
        "<MeshHeadingList>" +
        "<MeshHeading>" +
        "<DescriptorName MajorTopicYN=\"N\">Anti Chicken Authorities</DescriptorName>" +
        "<QualifierName MajorTopicYN=\"N\">featured use</QualifierName>" +
        "</MeshHeading>" +
        "</MeshHeadingList>" +
        "<NumberOfReferences>348</NumberOfReferences>" +
        "</MedlineCitation>";

    @Test public void readDocument() {
        DocumentReader reader = new PubMedDocumentReader();
        Document doc = reader.readDocument(DOCUMENT_ONE);
        assertEquals("pubmed", doc.sourceCorpus());
        assertEquals(12345, doc.id());
        assertEquals("12345", doc.key());
        assertEquals("CHICKEN", doc.title());
        assertEquals("And once there was a chicken.", doc.rawText());
        assertEquals(DOCUMENT_ONE, doc.originalText());
        assertEquals(2, doc.categories().size());
        assertTrue(doc.categories().contains("Anti-Chicken Agents"));
        assertTrue(doc.categories().contains("Non-geese"));

        doc = reader.readDocument(DOCUMENT_TWO);
        assertEquals("pubmed", doc.sourceCorpus());
        assertEquals(62345, doc.id());
        assertEquals("62345", doc.key());
        assertEquals("CHICKENS ATTACK", doc.title());
        assertEquals("A flock of chickens have attacked new york.", doc.rawText());
        assertEquals(DOCUMENT_TWO, doc.originalText());
        assertEquals(2, doc.categories().size());
        assertTrue(doc.categories().contains("Human-Chicken Resistance"));
        assertTrue(doc.categories().contains("Pro-Fowl"));
    }
}

