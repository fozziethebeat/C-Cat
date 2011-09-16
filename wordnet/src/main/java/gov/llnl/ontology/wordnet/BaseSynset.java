/*
 * Copyright (c) 2010, Lawrence Livermore National Security, LLC. Produced at
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

import edu.ucla.sspace.util.Duple;
import edu.ucla.sspace.util.HashMultiMap;
import edu.ucla.sspace.util.MultiMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;


/**
 * The core {@link Synset} implementation when interfacing directly with the
 * word net dictionary files.  This implementation satisfies all of the
 * requirements of a {@link Synset} and allows the {@link Synset} to be
 * re-serialized in the proper word net form.
 *
 * @author Keith Stevens
 */
public class BaseSynset implements Synset {

    /**
     * The {@link Relation}s associated with this {@link Synset}.
     */
    private MultiMap<String, Synset> relations;

    /**
     * The {@link Attribute}s associated with this {@link Synset}.
     */
    private Map<String, Attribute> attributes;

    private Map<String, String> morphyMap;

    /**
     * The total number of {@link Relation}s that this {@link Synset} has with
     * others.
     */
    private int numRelations;

    /**
     * The sense key which uniquely identifies the {@link Synset}.
     */
    private String senseKey;

    /**
     * The sense number for this {@link Synset}.
     */
    private int senseNumber;

    /**
     * The {@link PartsOfSpeech} tag for this {@link Synset}
     */
    private PartsOfSpeech pos;

    /**
     * This is the byte offset number used to identify this {@link Synset} when
     * loading and saving the WordNet dictionary.
     */
    private int offset;

    /**
     * This records the nubmer of bytes used to store the offset number in a
     * string format.
     */
    private int offsetSize;

    /**
     * The definitions for this {@link Synset}.
     */
    private String definition;

    /**
     * The set of example sentences for this {@link Synset}.
     */
    private List<String> examples;

    /**
     * The {@link Lemma}s associated with this {@link Synset}.
     */
    private List<Lemma> lemmas;

    /**
     * The list of possible sense keys.
     */
    private List<String> senseKeys;

    /**
     * A mapping from related {@link Synset}s to the lemma to which they are
     * related.
     */
    private Map<Synset, RelatedForm> relatedForms;

    /**
     * The frame indices for all verb frames for this {@link Synset}.
     */
    private int[] frameIds;

    /**
     * The lemma indices for all verb frames for this {@link Synset}.
     */
    private int[] lemmaIds;

    /**
     * The maximum depth from this {@link Synset} to the root of the hierarchy.
     */
    private int maxDepth;

    /**
     * The minimum depth from this {@link Synset} to the root of the hierarchy.
     */
    private int minDepth;

    /**
     * Creates a {@link BaseSynset} with a byte offset value.
     */
    public BaseSynset(String synsetName) {
        String[] namePosId = synsetName.split("\\.");
        senseKeys = new ArrayList<String>();
        lemmas = new ArrayList<Lemma>();
        lemmas.add(new BaseLemma(this, namePosId[0], "", 0, 0, ""));
        pos = WordNetCorpusReader.POS_MAP.get(namePosId[1]);
        senseNumber = Integer.parseInt(namePosId[2]);

        relations = new HashMultiMap<String, Synset>();
        attributes = new HashMap<String, Attribute>();
        relatedForms = new HashMap<Synset, RelatedForm>();
        morphyMap = new HashMap<String, String>();
        examples = new ArrayList<String>();
        frameIds = new int[0];
        lemmaIds = new int[0];

        this.offset = 0;
        this.definition = "";
        this.senseKey = "";
        this.numRelations = 0;
        this.minDepth = -1;
        this.maxDepth = -1;
        this.offsetSize = -1;
    }

    /**
     * Creates a {@link BaseSynset} with a byte offset value.
     */
    public BaseSynset(int offset, PartsOfSpeech pos) {
        this.offset = offset;
        this.pos = pos;

        relations = new HashMultiMap<String, Synset>();
        attributes = new HashMap<String, Attribute>();
        relatedForms = new HashMap<Synset, RelatedForm>();
        morphyMap = new HashMap<String, String>();
        examples = new ArrayList<String>();
        lemmas = new ArrayList<Lemma>();
        senseKeys = new ArrayList<String>();
        frameIds = new int[0];
        lemmaIds = new int[0];

        this.definition = "";
        this.senseKey = "";
        this.senseNumber = 0;
        this.numRelations = 0;
        this.minDepth = -1;
        this.maxDepth = -1;
        this.offsetSize = -1;
    }

    /**
     * Creates an empty {@link BaseSynset}.
     */
    public BaseSynset(PartsOfSpeech pos) {
        this(-1, pos);
    }

    /**
     * {@inheritDoc}
     */
    public void addMorphyMapping(String original, String lemma) {
        morphyMap.put(original, lemma);
    }

    /**
     * {@inheritDoc}
     */
    public int getSenseNumber() {
        return senseNumber;
    }

    /**
     * {@inheritDoc}
     */
    public void setSenseNumber(int senseNumber) {
        this.senseNumber = senseNumber;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return lemmas.get(0).getLemmaName().toLowerCase() + "." +
               pos + "." + senseNumber;
    }

    /**
     * {@inheritDoc}
     */
    public String getSenseKey() {
        return (senseKeys.size() > 0) ? senseKeys.get(0) : "";
    }

    /**
     * {@inheritDoc}
     */
    public String getSenseKey(String base) {
        if (senseKeys.size() == 1)
            return senseKeys.get(0);

        String lemma = morphyMap.get(base);
        lemma = (lemma == null) ? base : lemma;
        for (String senseKey : senseKeys)
            if (senseKey.startsWith(lemma))
                return senseKey;
        return "";
    }

    /**
     * {@inheritDoc}
     */
    public List<String> getSenseKeys() {
        return senseKeys;
    }

    /**
     * {@inheritDoc}
     */
    public void addSenseKey(String senseKey) {
        senseKeys.add(senseKey);
    }

    /**
     * {@inheritDoc}
     */
    public String getDefinition() {
        return definition;
    }

    /**
     * {@inheritDoc}
     */
    public PartsOfSpeech getPartOfSpeech() {
        return pos;
    }

    /**
     * {@inheritDoc}
     */
    public List<Lemma> getLemmas() {
        return lemmas;
    }

    /**
     * {@inheritDoc}
     */
    public List<String> getExamples() {
        return examples;
    }

    /**
     * {@inheritDoc}
     */
    public RelatedForm getDerivationallyRelatedForm(Synset synset) {
        return relatedForms.get(synset);
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getKnownRelationTypes() {
        return relations.keySet();
    }

    /**
     * {@inheritDoc}
     */
    public Collection<Synset> allRelations() {
        return relations.values();
    }

    /**
     * {@inheritDoc}
     */
    public Set<Synset> getRelations(String relation) {
        Set<Synset> r = relations.get(relation);
        return (r == null) ? new HashSet<Synset>() : r;
    }

    /**
     * {@inheritDoc}
     */
    public Set<Synset> getRelations(Relation relation) {
        return getRelations(relation.toString());
    }

    /**
     * {@inheritDoc}
     */
    public int getNumRelations() {
        return numRelations;
    }

    /**
     * {@inheritDoc}
     */
    public List<List<Synset>> getParentPaths() {
        List<List<Synset>> parentPaths = new ArrayList<List<Synset>>();
        Set<Synset> parents = getParents();
        if (parents == null || parents.size() == 0) {
            List<Synset> path = new ArrayList<Synset>();
            path.add(this);
            parentPaths.add(path);
        } else {
            for (Synset parent : parents) {
                for (List<Synset> ancestorList : parent.getParentPaths()) {
                    ancestorList.add(this);
                    parentPaths.add(ancestorList);
                }
            }
        }
        return parentPaths;
    }

    /**
     * {@inheritDoc}
     */
    public Set<Synset> getParents() {
        return getRelations(Relation.HYPERNYM);
    }

    /**
     * {@inheritDoc}
     */
    public Set<Synset> getChildren() {
        return getRelations(Relation.HYPONYM);
    }

    /**
     * {@inheritDoc}
     */
    public int getId() {
        return offset;
    }

    /**
     * {@inheritDoc}
     */
    public int[] getFrameIds() {
        return frameIds;
    }

    /**
     * {@inheritDoc}
     */
    public int[] getLemmaIds() {
        return lemmaIds;
    }

    /**
     * {@inheritDoc}
     */
    public void setFrameInfo(int[] frameIds, int[] lemmaIds) {
        this.frameIds = frameIds;
        this.lemmaIds = lemmaIds;
    }

    /**
     * {@inheritDoc}
     */
    public void setId(int newOffset) {
        offset = newOffset;
    }

    /**
     * {@inheritDoc}
     */
    public void setDefinition(String definition) {
        this.definition = definition;
    }

    /**
     * {@inheritDoc}
     */
    public void addLemma(Lemma lemma) {
        lemmas.add(lemma);
    }
    /**
     * {@inheritDoc}
     */
    public void addExample(String example) {
        examples.add(example);
    }

    /**
     * {@inheritDoc}
     */
    public void addDerivationallyRelatedForm(Synset related, RelatedForm form) {
        relatedForms.put(related, form);
    }

    /**
     * {@inheritDoc}
     */
    public boolean addRelation(Relation relation, Synset synset) {
        if (relation == null || synset == null)
            return false;

        return addRelation(relation.toString(), synset);
    }

    /**
     * {@inheritDoc}
     */
    public boolean addRelation(String relation, Synset synset) {
        if (relation == null || synset == null)
            return false;

        boolean added = relations.put(relation.intern(), synset);
        if (added)
            numRelations++;
        return added;
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeRelation(Relation relation, Synset synset) {
        return removeRelation(relation.toString(), synset);
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeRelation(String relation, Synset synset) {
        boolean removed = relations.remove(relation, synset);
        if (removed)
            numRelations--;
        return removed;
    }

    /**
     * {@inheritDoc}
     */
    public String getGloss() {
        StringBuilder sb = new StringBuilder();
        sb.append(getDefinition());
        for (String example : examples)
            sb.append(" ; ").append(" \"").append(example).append("\" ");
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    public int getMaxDepth() {
        if (maxDepth > -1)
            return maxDepth;

        // Recursively compute the maximum depth from this synset to the base
        // synset.  Cache the values along the way so that the computation is
        // only done once.
        int bestParentDepth = -1;
        for (Synset parent : getParents())
            bestParentDepth = Math.max(parent.getMaxDepth(), bestParentDepth);
        maxDepth = 1 + bestParentDepth;
        return maxDepth;
    }

    /**
     * {@inheritDoc}
     */
    public int getMinDepth() {
        if (minDepth > -1)
            return minDepth;

        // Recursively compute the minimum depth from this synset to the base
        // synset.  Cache the values along the way so that the computation is
        // only done once.
        int bestParentDepth = Integer.MAX_VALUE;
        for (Synset parent : getParents())
            bestParentDepth = Math.min(parent.getMinDepth(), bestParentDepth);

        // Store the best depth found so far, or 0 if this synset has no
        // parents.
        maxDepth = (bestParentDepth == Integer.MAX_VALUE) 
            ? 0
            : 1 + bestParentDepth;
        return maxDepth;
    }

    /**
     * {@inheritDoc}
     */
    public void setAttribute(String attributeName, Attribute attribute) {
        attributes.put(attributeName, attribute);
    }

    /**
     * {@inheritDoc}
     */
    public Attribute getAttribute(String attributeName) {
        return attributes.get(attributeName);
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> attributeLabels() {
        return attributes.keySet();
    }

    /**
     * {@inheritDoc}
     */
    public void merge(Synset synset) {
        // Reject any merges between synsets of different parts of speech.
        if (pos != synset.getPartOfSpeech())
            throw new IllegalArgumentException(
                    "Cannot merge synsets with different parts of speech.");

        Set<Duple<String, Synset>> toRemove =
            new HashSet<Duple<String, Synset>>();
        for (String relation : getKnownRelationTypes()) {
            Set<Synset> links = relations.get(relation);
            if (links.contains(synset))
                toRemove.add(new Duple<String, Synset>(relation, synset));
        }
        for (Duple<String, Synset> r : toRemove)
            relations.remove(r.x, r.y);

        // For any relation held by the other synset, add those relations to the
        // current synset. Also, For any synsets pointing to the other synset,
        // change their relation to point to this synset.
        for (String relation : synset.getKnownRelationTypes()) {
            // Iterate through all of the synsets related to the other synset.
            // Since most relations are reflexive, we can simply inspect the
            // relations for these related synsets and replace the mapping
            // from the related synset to the other synset with this current
            // synset.
            for (Synset related : synset.getRelations(relation)) {
                // If the related synset is actuall this synset, don'tmake
                // the link as it would create a cycle.
                if (related == this)
                    continue;

                // Add the relation to this synset.
                relations.put(relation, related);
                numRelations++;

                // If there is no reflexive version, skip inspection.
                Relation rel = Relation.fromId(relation);
                if (rel == null || rel.reflexive() == null)
                    continue;

                // Find the synsets that the related synset points to for this
                // relation.  Replace the mapping from that sysnet to the other
                // synset with this current synset.
                Set<Synset> inwardRelations = related.getRelations(
                        rel.reflexive());
                if (inwardRelations.contains(synset)) {
                    inwardRelations.remove(synset);
                    inwardRelations.add(this);
                }
            }
        }

        // Copy over the examples and the gloss to this synset.
        for (String example : synset.getExamples())
            examples.add(example);
        this.definition += "; " + synset.getDefinition();

        // Copy over the lemmas for the other synset.  Assume this synset
        // maintains it's base lemma.
        for (Lemma otherLemma : synset.getLemmas())
            lemmas.add(otherLemma);

        // We currently lose out on the derivationally related forms.  
 
        // For each attribute in the other synset, copy it over or merge it if
        // this synset has a similarly labeled attribute.
        for (String attributeLabel : synset.attributeLabels()) {
            Attribute attribute = attributes.get(attributeLabel);
            Attribute other = synset.getAttribute(attributeLabel);
            if (attribute == null)
                attributes.put(attributeLabel, other);
            else if (attribute != other)
                attribute.merge(other);
        }

        // Reset the min and max depth values as it will change with the merging
        // of synset parents.
        this.minDepth = -1;
        this.maxDepth = -1;
    }

    /**
     * Returns this {@link Synset}s name and gloss.
     */
    public String toString() {
        return String.format("%s: %s\n", getName(), getDefinition());
    }
}
