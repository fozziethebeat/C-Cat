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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


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
  private Map<String, List<Synset>> relations;

  /**
   * The {@link Attribute}s associated with this {@link Synset}.
   */
  private Map<String, Attribute> attributes;

  /**
   * The total number of {@link Relation}s that this {@link Synset} has with
   * others.
   */
  private int numRelations;

  /**
   * The name, which uniquely identifies the {@link Synset}.
   */
  private String name;

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
  public BaseSynset(int offset, PartsOfSpeech pos) {
    this.offset = offset;
    relations = new HashMap<String, List<Synset>>();
    attributes = new HashMap<String, Attribute>();
    relatedForms = new HashMap<Synset, RelatedForm>();
    examples = new ArrayList<String>();
    lemmas = new ArrayList<Lemma>();
    frameIds = new int[0];
    lemmaIds = new int[0];

    this.pos = pos;
    this.definition = "";
    this.name = "";
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
    return senseKey;
  }

  /**
   * {@inheritDoc}
   */
  public void setSenseKey(String senseKey) {
    this.senseKey = senseKey;
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
  public List<Synset> getRelations(String relation) {
    List<Synset> r = relations.get(relation);
    return (r == null) ? new ArrayList<Synset>() : r;
  }

  /**
   * {@inheritDoc}
   */
  public List<Synset> getRelations(Relation relation) {
    return getRelations(relation.toId());
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
    List<Synset> parents = getParents();
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
  public List<Synset> getParents() {
    return getRelations(Relation.HYPERNYM);
  }

  /**
   * {@inheritDoc}
   */
  public List<Synset> getChildren() {
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
   * Resets the offset for this synset.
   */
  public void setId(int newOffset) {
    offset = newOffset;
  }

  /**
   * Sets the dictionary definition for this {@link Synset}.
   */
  public void setDefinition(String definition) {
    this.definition = definition;
  }

  /**
   * Sets the sense name for this {@link Synset}.
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Sets the set of terms associated with this {@link BaseSynset}.
   */
  public void addLemma(Lemma lemma) {
    lemmas.add(lemma);
  }
  /**
   * Adds an example sentence for this {@link Synset}.
   */
  public void addExample(String example) {
    examples.add(example);
  }

  /**
   * Adds the given {@link Synset} as a related {@link Synset} and the lemma
   * number for that {@link Synset} that irelated to this {@link Synset}.
   */
  public void addDerivationallyRelatedForm(Synset related, RelatedForm form) {
    relatedForms.put(related, form);
  }

  /**
   * Adds the given {@link Synset} to the set of related synsets with the given
   * relation.
   */
  public void addRelation(Relation relation, Synset synset) {
    addRelation(relation.toId(), synset);
  }

  /**
   * Adds the given {@link Synset} to the set of related synsets with the given
   * relation.
   */
  public void addRelation(String relation, Synset synset) {
    List<Synset> synsets = relations.get(relation);
    if (synsets == null) {
      synsets = new ArrayList<Synset>();
      relations.put(relation, synsets);
    }
    synsets.add(synset);
    numRelations++;
  }

  /**
   * {@inheritDoc}
   */
  public String getGloss() {
    StringBuilder sb = new StringBuilder();
    sb.append(getDefinition());
    for (String example : examples)
      sb.append(";").append(" \"").append(example).append("\" ");
    return sb.toString();
  }

  /**
   * {@inheritDoc}
   */
  public int getMaxDepth() {
    if (maxDepth > -1)
      return maxDepth;

    // Recursively compute the maximum depth from this synset to the base
    // synset.  Cache the values along the way so that the computation is only
    // done once.
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
    // synset.  Cache the values along the way so that the computation is only
    // done once.
    int bestParentDepth = Integer.MAX_VALUE;
    for (Synset parent : getParents())
      bestParentDepth = Math.min(parent.getMinDepth(), bestParentDepth);

    // Store the best depth found so far, or 0 if this synset has no parents.
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

    // Remove the other synset from the known hierarchy.
    WordNetCorpusReader.getWordNet().replaceSynset(synset, this);

    // For any relation held by the other synset, add those relations to the
    // current synset. Also, For any synsets pointing to the other synset,
    // change their relation to point to this synset.
    for (Relation relation : Relation.values()) {
      // Create the list of related synsets if this synset does not already have
      // one for this relation.
      List<Synset> thisRelations = relations.get(relation.toId());
      if (thisRelations == null) {
        thisRelations = new ArrayList<Synset>();
        relations.put(relation.toId(), thisRelations);
      }

      // Iterate through all of the synsets related to the other synset.  Since
      // most relations are reflexive, we can simply inspect the relations for  
      // these related synsets and replace the mapping from the related synset
      // to the other synset with this current synset.
      for (Synset related : synset.getRelations(relation)) {
        // Add the relation to this synset.
        thisRelations.add(related);
        numRelations++;

        // If there is no reflexive version, skip inspection.
        if (relation.reflexive() == null)
          continue;

        // Find the synsets that the related synset points to for this relation.
        // Replace the mapping from that sysnet to the other synset with this
        // current synset.
        List<Synset> inwardRelations = related.getRelations(
            relation.reflexive());
        int i = 0;
        for (Synset inward : inwardRelations) {
          if (inward.equals(synset))
            inwardRelations.set(i, this);
          i++;
        }
      }
    }

    // Copy over the examples and the gloss to this synset.
    for (String example : synset.getExamples())
      examples.add(example);
    this.definition += "; " + synset.getDefinition();

    // Copy over the lemmas for the other synset.  Assume this synset maintains
    // it's base lemma.
    for (Lemma otherLemma : synset.getLemmas())
      lemmas.add(otherLemma);

    // We currently lose out on the derivationally related forms.

    // For each attribute in the other synset, copy it over or merge it if this
    // synset has a similarly labeled attribute.
    for (String attributeLabel : synset.attributeLabels()) {
      Attribute attribute = attributes.get(attributeLabel);
      if (attribute == null)
        attributes.put(attributeLabel, synset.getAttribute(attributeLabel));
      else
        attribute.merge(synset.getAttribute(attributeLabel));
    }

    // Reset the min and max depth values as it will change with the merging of
    // synset parents.
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
