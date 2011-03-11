

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
        //assertEquals(3, synset.getMaxDepth());
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

    class FakeAttribute implements Attribute<String> {
        String s;

        public FakeAttribute(String s) {
            this.s = s;
        }

        public String object() {
            return s;
        }

        public void merge(Attribute<String> other) {
            s = s + " " + other;
        }
    }
}
