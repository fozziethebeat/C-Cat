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



package gov.llnl.ontology.wordnet;

import gov.llnl.ontology.wordnet.Synset.PartsOfSpeech;
import gov.llnl.ontology.wordnet.Synset.Relation;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.*;


/**
 * @author Keith Stevens
 */
public class BaseSynsetTest {

    @Test public void testConstructor() {
        Synset synset = new BaseSynset(PartsOfSpeech.NOUN);
        assertEquals(PartsOfSpeech.NOUN, synset.getPartOfSpeech());
    }

    @Test public void testName() {
        Synset synset = new BaseSynset(PartsOfSpeech.NOUN);
        Lemma lemma = new BaseLemma(synset, "cat", "lexname", 0, 1, "n");
        synset.addLemma(lemma);
        assertEquals("cat.n.0", synset.getName());
    }

    @Test public void testLemma() {
        Synset synset = new BaseSynset(PartsOfSpeech.NOUN);
        Lemma[] lemmas = new Lemma[] {
            new BaseLemma(synset, "cat", "lexname", 0, 1, "n"),
            new BaseLemma(synset, "feline", "lexname", 0, 2, "n"),
            new BaseLemma(synset, "pidgin", "lexname", 0, 1, "n"),
        };

        for (Lemma lemma : lemmas)
            synset.addLemma(lemma);

        List<Lemma> testLemmas = synset.getLemmas();
        assertEquals(lemmas.length, testLemmas.size());
        for (int i = 0; i < lemmas.length; ++i)
            assertEquals(lemmas[i], testLemmas.get(i));
    }

    @Test public void testSenseKey() {
        Synset synset = new BaseSynset(PartsOfSpeech.NOUN);
        synset.setSenseKey("cat");
        assertEquals("cat", synset.getSenseKey());
    }

    @Test public void testSenseNumber() {
        Synset synset = new BaseSynset(PartsOfSpeech.NOUN);
        synset.setSenseNumber(1);
        assertEquals(1, synset.getSenseNumber());
    }

    @Test public void testExample() {
        String[] examples = {"one", "two"};
        Synset synset = new BaseSynset(PartsOfSpeech.NOUN);
        for (String example : examples)
            synset.addExample(example);
        List<String> testExamples = synset.getExamples();
        assertEquals(examples.length, testExamples.size());
        for (int i = 0; i < examples.length; ++i)
            assertEquals(examples[i], testExamples.get(i));
    }

    @Test public void testDefinition() {
        Synset synset = new BaseSynset(PartsOfSpeech.NOUN);
        synset.setDefinition("1");
        assertEquals("1", synset.getDefinition());
    }

    @Test public void testGloss() {
        String[] examples = {"one", "two"};
        String definition = "what is a cat?";
        Synset synset = new BaseSynset(PartsOfSpeech.NOUN);
        for (String example : examples)
            synset.addExample(example);
        synset.setDefinition(definition);

        String gloss = synset.getGloss();
        assertTrue(gloss.contains(definition));
        for (String example : examples)
            assertTrue(gloss.contains(example));
    }

    @Test public void testFrameInfo() {
        int[] frameIds = {1, 2, 3, 4};
        int[] lemmaIds = {5, 4, 3, 2};
        Synset synset = new BaseSynset(PartsOfSpeech.NOUN);
        synset.setFrameInfo(frameIds, lemmaIds);
        assertEquals(frameIds.length, synset.getFrameIds().length);
        assertEquals(lemmaIds.length, synset.getLemmaIds().length);
        for (int i = 0; i < frameIds.length; ++i)
            assertEquals(frameIds[i], synset.getFrameIds()[i]);
        for (int i = 0; i < lemmaIds.length; ++i)
            assertEquals(lemmaIds[i], synset.getLemmaIds()[i]);
    }

    @Test public void testStringRelations() {
        Synset synset = new BaseSynset(PartsOfSpeech.NOUN);
        Synset other1 = new BaseSynset(PartsOfSpeech.NOUN);
        Synset other2 = new BaseSynset(PartsOfSpeech.NOUN);
        synset.addRelation("blah", other1);
        synset.addRelation("gah", other2);

        assertEquals(2, synset.getNumRelations());

        Set<String> relations = synset.getKnownRelationTypes();
        assertEquals(2, relations.size());
        assertTrue(relations.contains("blah"));
        assertTrue(relations.contains("gah"));

        Set<Synset> related = synset.getRelations("blah");
        assertEquals(1, related.size());
        assertTrue(related.contains(other1));

        related = synset.getRelations("gah");
        assertEquals(1, related.size());
        assertTrue(related.contains(other2));
    }

    @Test public void testRelations() {
        Synset synset = new BaseSynset(PartsOfSpeech.NOUN);
        Synset other1 = new BaseSynset(PartsOfSpeech.NOUN);
        Synset other2 = new BaseSynset(PartsOfSpeech.NOUN);
        synset.addRelation(Relation.HYPERNYM, other1);
        synset.addRelation(Relation.ANTONYM, other2);

        assertEquals(2, synset.getNumRelations());

        Set<String> relations = synset.getKnownRelationTypes();
        assertEquals(2, relations.size());
        assertTrue(relations.contains(Relation.HYPERNYM.toString()));
        assertTrue(relations.contains(Relation.ANTONYM.toString()));

        Set<Synset> related = synset.getRelations(Relation.HYPERNYM);
        assertEquals(1, related.size());
        assertTrue(related.contains(other1));

        related = synset.getRelations(Relation.ANTONYM);
        assertEquals(1, related.size());
        assertTrue(related.contains(other2));
    }

    @Test public void testRelatedForm() {
        RelatedForm relatedForm = new SimpleRelatedForm(1, 1);
        Synset synset = new BaseSynset(PartsOfSpeech.NOUN);
        Synset relatedSynset = new BaseSynset(PartsOfSpeech.VERB);

        synset.addDerivationallyRelatedForm(relatedSynset, relatedForm);

        assertEquals(relatedForm,
                     synset.getDerivationallyRelatedForm(relatedSynset));
    }

    @Test public void testParents() {
        Synset synset = new BaseSynset(PartsOfSpeech.NOUN);
        Synset other1 = new BaseSynset(PartsOfSpeech.NOUN);
        Synset other2 = new BaseSynset(PartsOfSpeech.NOUN);

        synset.addRelation(Relation.HYPERNYM, other1);
        synset.addRelation(Relation.HYPERNYM, other2);

        Set<Synset> parents = synset.getParents();
        assertEquals(2, parents.size());
        assertTrue(parents.contains(other1));
        assertTrue(parents.contains(other2));
    }

    @Test public void testChildren() {
        Synset synset = new BaseSynset(PartsOfSpeech.NOUN);
        Synset other1 = new BaseSynset(PartsOfSpeech.NOUN);
        Synset other2 = new BaseSynset(PartsOfSpeech.NOUN);

        synset.addRelation(Relation.HYPONYM, other1);
        synset.addRelation(Relation.HYPONYM, other2);

        Set<Synset> children = synset.getChildren();
        assertEquals(2, children.size());
        assertTrue(children.contains(other1));
        assertTrue(children.contains(other2));
    }

    @Test public void testStringParents() {
        Synset synset = new BaseSynset(PartsOfSpeech.NOUN);
        Synset other1 = new BaseSynset(PartsOfSpeech.NOUN);
        Synset other2 = new BaseSynset(PartsOfSpeech.NOUN);

        synset.addRelation(Relation.HYPERNYM.toString(), other1);
        synset.addRelation(Relation.HYPERNYM.toString(), other2);

        Set<Synset> parents = synset.getParents();
        assertEquals(2, parents.size());
        assertTrue(parents.contains(other1));
        assertTrue(parents.contains(other2));
    }

    @Test public void testStringChildren() {
        Synset synset = new BaseSynset(PartsOfSpeech.NOUN);
        Synset other1 = new BaseSynset(PartsOfSpeech.NOUN);
        Synset other2 = new BaseSynset(PartsOfSpeech.NOUN);

        synset.addRelation(Relation.HYPONYM.toString(), other1);
        synset.addRelation(Relation.HYPONYM.toString(), other2);

        Set<Synset> children = synset.getChildren();
        assertEquals(2, children.size());
        assertTrue(children.contains(other1));
        assertTrue(children.contains(other2));
    }

    @Test public void testParentPaths() {
        Synset synset = new BaseSynset(PartsOfSpeech.NOUN);
        Synset root = new BaseSynset(PartsOfSpeech.NOUN);
        Synset parent1 = new BaseSynset(PartsOfSpeech.NOUN);
        Synset parent11 = new BaseSynset(PartsOfSpeech.NOUN);
        Synset parent2 = new BaseSynset(PartsOfSpeech.NOUN);

        synset.addRelation(Relation.HYPERNYM, parent1);
        synset.addRelation(Relation.HYPERNYM, parent2);

        parent1.addRelation(Relation.HYPERNYM, parent11);

        parent11.addRelation(Relation.HYPERNYM, root);
        parent2.addRelation(Relation.HYPERNYM, root);

        List<List<Synset>> parentPaths = synset.getParentPaths();
        assertEquals(2, parentPaths.size());

        List<Synset> path1 = Arrays.asList(new Synset[] {
            root, parent11, parent1, synset});
        List<Synset> path2 = Arrays.asList(new Synset[] {
            root, parent2, synset});

        assertTrue((parentPaths.get(1).size() == path1.size() &&
                    parentPaths.get(0).size() == path2.size()) ||
                   (parentPaths.get(0).size() == path1.size() &&
                    parentPaths.get(1).size() == path2.size()));
        if (parentPaths.get(0).size() == path1.size()) {
            for (int i = 0; i < path1.size(); ++i)
                assertTrue(path1.get(i) == parentPaths.get(0).get(i));
            for (int i = 0; i < path2.size(); ++i)
                assertTrue(path2.get(i) == parentPaths.get(1).get(i));
        } else {
            for (int i = 0; i < path1.size(); ++i)
                assertTrue(path1.get(i) == parentPaths.get(1).get(i));
            for (int i = 0; i < path2.size(); ++i)
                assertTrue(path2.get(i) == parentPaths.get(0).get(i));
        }

        assertEquals(2, synset.getMinDepth());
    }

    @Test public void testMergedParentPaths() {
        Synset synset = new BaseSynset(PartsOfSpeech.NOUN);
        Synset root = new BaseSynset(PartsOfSpeech.NOUN);
        Synset parent1 = new BaseSynset(PartsOfSpeech.NOUN);
        Synset parent11 = new BaseSynset(PartsOfSpeech.NOUN);
        Synset parent2 = new BaseSynset(PartsOfSpeech.NOUN);

        synset.addRelation(Relation.HYPERNYM, parent1);
        synset.addRelation(Relation.HYPERNYM, parent2);

        parent1.addRelation(Relation.HYPERNYM, parent11);

        parent11.addRelation(Relation.HYPERNYM, root);
        parent2.addRelation(Relation.HYPERNYM, root);

        synset.merge(parent1);

        List<List<Synset>> parentPaths = synset.getParentPaths();
        assertEquals(2, parentPaths.size());

        assertEquals(3, parentPaths.get(0).size());
        assertEquals(3, parentPaths.get(1).size());

        assertTrue(root == parentPaths.get(0).get(0));
        assertTrue(root == parentPaths.get(1).get(0));

        if (parent11 == parentPaths.get(0).get(1)) {
            assertTrue(parent2 == parentPaths.get(1).get(1));
        } else {
            assertTrue(parent2 == parentPaths.get(0).get(1));
            assertTrue(parent11 == parentPaths.get(1).get(1));
        }

        assertTrue(synset == parentPaths.get(0).get(2));
        assertTrue(synset == parentPaths.get(1).get(2));
    }

    @Test public void testAttribute() {
        Synset synset = new BaseSynset(PartsOfSpeech.NOUN);

        Attribute attribute = new FakeAttribute("cat");
        synset.setAttribute("cattribute", attribute);

        assertEquals(1, synset.attributeLabels().size());
        assertTrue(synset.attributeLabels().contains("cattribute"));

        assertEquals(attribute, synset.getAttribute("cattribute"));
    }

    @Test (expected=IllegalArgumentException.class)
    public void testMergeBadPos() {
        Synset s1 = new BaseSynset(PartsOfSpeech.NOUN);
        Synset s2 = new BaseSynset(PartsOfSpeech.VERB);
        s1.merge(s2);
    }

    @Test public void testMergeRelationsRemoved() {
        Synset s1 = new BaseSynset(PartsOfSpeech.NOUN);
        Synset s2 = new BaseSynset(PartsOfSpeech.NOUN);
        Synset s3 = new BaseSynset(PartsOfSpeech.NOUN);

        s1.addRelation("cat", s2);
        s1.addRelation("dog", s2);
        s1.addRelation("cat", s3);

        s1.merge(s2);

        assertEquals(0, s1.getRelations("dog").size());
        assertEquals(1, s1.getRelations("cat").size());
        assertTrue(s1.getRelations("cat").contains(s3));
    }

    @Test public void testMergeRelationsAdded() {
        Synset s1 = new BaseSynset(PartsOfSpeech.NOUN);
        Synset s2 = new BaseSynset(PartsOfSpeech.NOUN);
        Synset s3 = new BaseSynset(PartsOfSpeech.NOUN);

        s2.addRelation(Relation.HYPERNYM, s3);
        s2.addRelation("dog", s3);
        s3.addRelation(Relation.HYPONYM, s2);

        s1.merge(s2);

        assertEquals(1, s1.getRelations("dog").size());
        assertTrue(s1.getRelations("dog").contains(s3));
        assertEquals(1, s1.getRelations(Relation.HYPERNYM).size());
        assertTrue(s1.getRelations(Relation.HYPERNYM).contains(s3));
        assertEquals(1, s3.getRelations(Relation.HYPONYM).size());
        assertTrue(s3.getRelations(Relation.HYPONYM).contains(s1));
    }

    @Test public void testMergExamplesAndDefinitions() {
        Synset s1 = new BaseSynset(PartsOfSpeech.NOUN);
        Synset s2 = new BaseSynset(PartsOfSpeech.NOUN);

        s1.addExample("blah");
        s2.addExample("rawr");
        s2.addExample("bloop");

        s1.setDefinition("Chickens!");
        s2.setDefinition("A parliment of owls!");

        s1.merge(s2);

        List<String> examples = s1.getExamples();
        assertEquals("blah", examples.get(0));
        assertEquals("rawr", examples.get(1));
        assertEquals("bloop", examples.get(2));

        assertEquals("Chickens!; A parliment of owls!", s1.getDefinition());
    }

    @Test public void testMergeLemmas() {
        Synset s1 = new BaseSynset(PartsOfSpeech.NOUN);
        Synset s2 = new BaseSynset(PartsOfSpeech.NOUN);

        Lemma lemma1 = new BaseLemma(s1, "cat", "lexname", 0, 1, "n");
        s1.addLemma(lemma1);

        Lemma lemma2 = new BaseLemma(s2, "mao", "lexname", 0, 1, "n");
        s2.addLemma(lemma2);

        s1.merge(s2);

        List<Lemma> lemmas = s1.getLemmas();
        assertEquals(lemma1, lemmas.get(0));
        assertEquals(lemma2, lemmas.get(1));
    }

    @Test public void testMergeAttributes() {
        Synset s1 = new BaseSynset(PartsOfSpeech.NOUN);
        Synset s2 = new BaseSynset(PartsOfSpeech.NOUN);

        Attribute attribute1 = new FakeAttribute("cat");
        s1.setAttribute("catribute", attribute1);
        Attribute attribute1a = new FakeAttribute("sat");
        s1.setAttribute("satribute", attribute1a);

        Attribute attribute2 = new FakeAttribute("bat");
        s2.setAttribute("batribute", attribute2);
        s2.setAttribute("catribute", attribute1);
        Attribute attribute2a = new FakeAttribute("mat");
        s2.setAttribute("satribute", attribute2a);

        s1.merge(s2);

        assertEquals(3, s1.attributeLabels().size());
        assertTrue(s1.attributeLabels().contains("catribute"));
        assertEquals(attribute1, s1.getAttribute("catribute"));

        assertTrue(s1.attributeLabels().contains("batribute"));
        assertEquals(attribute2, s2.getAttribute("batribute"));

        assertTrue(s1.attributeLabels().contains("satribute"));

        String result = (String) s1.getAttribute("satribute").object();
        assertEquals("sat mat", result);
    }

    @Test public void testMergeResetDepth() {
        Synset s1 = new BaseSynset(PartsOfSpeech.NOUN);
        Synset s2 = new BaseSynset(PartsOfSpeech.NOUN);

        Synset parent = new BaseSynset(PartsOfSpeech.NOUN);
        s2.addRelation(Relation.HYPERNYM, parent);

        s1.merge(s2);

        assertEquals(1, s1.getMinDepth());
        assertEquals(1, s1.getMaxDepth());
    }

    class FakeAttribute implements Attribute<String> {
        String s;

        public FakeAttribute(String s) {
            this.s = s;
        }

        public String object() {
            return s;
        }

        public void merge(Attribute<String> other) {
            s = s + " " + other.object();
        }
    }
}
