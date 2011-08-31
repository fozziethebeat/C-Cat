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

package gov.llnl.ontology.wordnet.feature;

import gov.llnl.ontology.wordnet.Attribute;
import gov.llnl.ontology.wordnet.DoubleVectorAttribute;
import gov.llnl.ontology.wordnet.Lemma;
import gov.llnl.ontology.wordnet.Synset;
import gov.llnl.ontology.wordnet.Synset.PartsOfSpeech;
import gov.llnl.ontology.wordnet.OntologyReader;
import gov.llnl.ontology.wordnet.WordNetCorpusReader;

import edu.ucla.sspace.common.SemanticSpace;

import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.SparseVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.Vectors;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class applies semantic vectors to every {@link Synset} that is reachable
 * from some root {@link Synset}.  This is an implementation of:
 *
 *   </p style="font-family:Garamond, Georgia, serif">Pantel, P., "Inducing
 *   ontological co-occurrence vectors, " in <i> Proceedings of the 43rd Annual
 *   Meeting on Association for Computational Linguistics</i>
 *
 * </p>
 *
 * Given a {@link SemanticSpace} and a root {@link Synset}, this algorithm will
 * recursively traverse all nodes reachable by the root node.  All leaf nodes
 * will be given a feature vector that is initially the semantic vector found in
 * the {@link SemanticSpace} for any of the leaf {@link Synset}'s lemmas.  After
 * all leaf nodes have been given a feature vector, parent nodes will get a
 * feature vector that is an fuzzy intersection of it's children's feature
 * vectors.  Once every node has been given a feature vector, the leaf nodes are
 * disambiguated by setting any features shared by alternative {@link
 * Synset}'s parent's feature vector to zero.
 *
 * </p>
 *
 * For example, if there were two {@link Synset}s for "cat", one for feline pets
 * and one for cool guys, then the feature vector for the feline cat {@link
 * Synset} and feature vector for the cool guy {@link Synset} would initially
 * share the same feature vector.  If "pet" is the parent of feline cat, it
 * would get a feature vector that has all features shared by the feline cat
 * {@link Synset}, a dog {@link Synset}, and any other pet based {@link
 * Sysnet}s.  Similarity for the parent of the cool guy {@link Synset}.  Then,
 * for any non zero feature in the parent of the cool guy {@link Synset} that is
 * shared with the feline pet {@link Synset}, the feature in the feline pet's
 * feature vector is set to 0, thus saving only the features relevant to feline
 * pets and dropping any features related to cool guys.
 *
 * @author Keith Stevens
 */
public class OntologicalFeatureMaker {

  /**
   * The {@link SemanticSpace} that will provide the initial feature vectors for
   * lemma {@link Synset}s.
   */
  private final SemanticSpace sspace;

  /**
   * A mapping from terms to their weight.  This weight may be based on
   * frequency, or some other metric.
   */
  private final Map<String, Double> termWeights;

  /**
   * The attribute name for feature vector created by this {@link
   * OntologicalFeatureMaker}.
   */
  private final String semanticAttributeName;

  /**
   * The attribute name for {@link Synset} weights created by this {@link
   * OntologicalFeatureMaker}.
   */
  private final String scoreAttributeName;

  /**
   * Creates a new {@link OntologicalFeatureMaker} from a {@link SemanticSpace}.
   * Feature weights for each term are assumed to be the sum of the term's
   * features.
   */
  public OntologicalFeatureMaker(SemanticSpace sspace) {
    this(sspace, null);
  }

  /**
   * Creates a new {@link OntologicalFeatureMaker} from a {@link SemanticSpace}.
   * If {@link termWeight} is null, then feature weights for each term are
   * assumed to be the sum of the term's features.  Otherwise, the values in
   * {@link termWeights} are used.
   */
  public OntologicalFeatureMaker(SemanticSpace sspace,
                                 Map<String, Double> termWeights) {
    this.sspace = sspace;

    semanticAttributeName = sspace.getSpaceName();
    scoreAttributeName = semanticAttributeName + "Score";

    double totalWeight = 0;
    if (termWeights == null) {
      this.termWeights = new HashMap<String, Double>();

      for (String term : sspace.getWords()) {
        double termWeight = 0;
        DoubleVector vector  = Vectors.asDouble(sspace.getVector(term));
        if (vector instanceof SparseVector) {
          SparseVector sv = (SparseVector) vector;
          for (int index : sv.getNonZeroIndices())
            termWeight += vector.get(index);
        } else {
          for (int index = 0; index < vector.length(); ++index)
            termWeight += vector.get(index);
        }
        totalWeight += termWeight;
        this.termWeights.put(term, termWeight);
      }
    } else {
      this.termWeights = termWeights;
      for (Double weight : termWeights.values())
        totalWeight += weight;
    }

    for (String term : this.termWeights.keySet())
      this.termWeights.put(term, this.termWeights.get(term) / totalWeight);
  }

  /**
   * Returns the string needed to access the feature vectors created.
   */
  public String getAttributeName() {
    return semanticAttributeName;
  }

  /**
   * Creates feature vectors for every {@link Synset} reachable by {@code root}.
   */
  public void induceOntologicalFeatures(Synset root) {
    Set<Synset> leafNodes = new HashSet<Synset>();
    applyOntologicalFeatures(root, leafNodes);
    disambiguateLeafNodes(leafNodes);
  }

  /**
   * Recursively traverses the {@link Synset} hierarchy based at {@code root}.
   * Leaf nodes are given feature vectors from {@code sspace}.  Internal nodes
   * are given an intersection of features from their child nodes.
   */
  public void applyOntologicalFeatures(Synset root, Set<Synset> leafNodes) {
    Set<Synset> children = root.getChildren();

    // If the synset is a leaf node, get the semantic vector for the first
    // represented lemma in the word space.
    if (children == null || children.size() == 0) {
      for (Lemma lemma : root.getLemmas()) {
        Vector lemmaVector = sspace.getVector(lemma.getLemmaName());
        if (lemmaVector != null) {
          root.setAttribute(
              semanticAttributeName,
              new DoubleVectorAttribute(Vectors.asDouble(lemmaVector)));

          double weight = termWeights.get(lemma.getLemmaName());
          root.setAttribute(scoreAttributeName, new ScoreAttribute(weight));
          leafNodes.add(root);
          return;
        }
      }
      return;
    }

    // If the synset is not a leaf node, index features for each of the children
    // nodes and then construct the intersection of the children features.
    for (Synset child : children)
      applyOntologicalFeatures(child, leafNodes);

    SparseDoubleVector featureCounts = new CompactSparseVector(
        sspace.getVectorLength());
    double parentScore = 0;

    // Traverse every child for this parent node.  Get the child's feature
    // vector and create a count for the number of children that share each
    // feature.
    for (Synset child : children) {
      DoubleVectorAttribute attribute =
        (DoubleVectorAttribute) child.getAttribute(semanticAttributeName);
      if (attribute == null)
        continue;

      DoubleVector childVector = attribute.object();

      // Increase the parents score by bubbling up each child's score.
      ScoreAttribute scoreAttribute = (ScoreAttribute) child.getAttribute(
          scoreAttributeName);
      parentScore += scoreAttribute.object();

      // Traverse the child's feature vector to count how many non zero features
      // the child has.
      if (childVector instanceof SparseVector) {
        SparseVector sv = (SparseVector) childVector;
        for (int index : sv.getNonZeroIndices())
          featureCounts.add(index, 1);
      } else {
        for (int i = 0; i < childVector.length(); ++i)
          if (childVector.get(i) != 0d)
            featureCounts.add(i, 1);
      }
    }

    // Create the parents feature vector.
    SparseDoubleVector parentVector = new CompactSparseVector(
        sspace.getVectorLength());
    for (int index : featureCounts.getNonZeroIndices()) {
      // Reject any feature that does not pass a threshold.
      if (featureCounts.get(index) < Math.min(3, children.size()))
        continue;

      // Sum the featue values from each child for the valid features.
      for (Synset child : children) {
        // Get the child's feature vector or skip the child if one was not
        // generated.
        DoubleVectorAttribute attribute =
          (DoubleVectorAttribute) child.getAttribute(semanticAttributeName);
        if (attribute == null)
          continue;
        DoubleVector childVector = attribute.object();

        // Add the child's feature value weighted by the child's weight.
        ScoreAttribute scoreAttribute = (ScoreAttribute) child.getAttribute(
            scoreAttributeName);
        double childScore = scoreAttribute.object();

        parentVector.add(index, childVector.get(index) * childScore);
      }
    }

    // Set the nodes feature vector and weight attributes.
    root.setAttribute(
        semanticAttributeName, new DoubleVectorAttribute(parentVector));
    root.setAttribute(scoreAttributeName, new ScoreAttribute(parentScore));
  }

  /**
   * Disambiguates each leaf node in the hierarchy by removing features shared
   * by alternative word senses for each {@link Synset}.
   */
  private void disambiguateLeafNodes(Set<Synset> leafNodes) {
    OntologyReader wordnet = WordNetCorpusReader.getWordNet();

    // Traverse each leaf node.
    for (Synset leaf : leafNodes) {
      // Create a list of features held by this nodes parent's.
      BitSet parentFeature = new BitSet();
      DoubleVectorAttribute attribute =
        (DoubleVectorAttribute) leaf.getAttribute(semanticAttributeName);

      // Reject any leaf nodes that lack a feature vector.
      if (attribute == null)
        continue;
      DoubleVector leafVector = attribute.object();

      // Traverse each possible parent of this node.
      for (Synset parent : leaf.getParents()) {
        // Get the parents feature vector.  Since this node has a feture vector,
        // we know that it's parent must also have a feature vector.
        attribute = 
          (DoubleVectorAttribute) parent.getAttribute(semanticAttributeName);
        DoubleVector parentVector = attribute.object();

        // Count the number of non zero values in the parent's feature vector.
        if (parentVector instanceof SparseVector) {
          SparseVector sv = (SparseVector) parentVector;
          for (int index : sv.getNonZeroIndices())
            parentFeature.set(index);
        } else {
          for (int index = 0; index < parentVector.length(); ++index)
            if (parentVector.get(index) != 0d)
              parentFeature.set(index);
        }
      }

      // Inspect each alternative sense for each lemma for this synset.  For
      // each alternative, get that alternative's parent's feature vector.
      // Remove any features from this nodes feature vector that is shared by
      // this alternative parent's feature vector.
      PartsOfSpeech pos = leaf.getPartOfSpeech();
      List<Lemma> lemmas = leaf.getLemmas();

      // Traverse each lemma.
      for (Lemma lemma : lemmas) {
        Synset[] alternatives = wordnet.getSynsets(lemma.getLemmaName(), pos);

        // Traverse each alternative sense.
        for (Synset alternative : alternatives) {

          // Get the parents for each aternative sense.
          for (Synset altParent : alternative.getParents()) {
            // Get the feature vector for the altnerative parent, reject any
            // parents that lack a feature vector.
            Attribute altAttribute =
              altParent.getAttribute(semanticAttributeName);
            if (altAttribute == null)
            continue;
            DoubleVector altVector = (DoubleVector) altAttribute.object();

            // Remove any features shared with this parent and the current node
            // from the current node's feature vector.
            if (altVector instanceof SparseVector) {
              SparseVector sv = (SparseVector) altVector;
              for (int index : sv.getNonZeroIndices())
                if (!parentFeature.get(index))
                  leafVector.set(index, 0);
            } else {
              for (int index = 0; index < altVector.length(); ++index)
                if (altVector.get(index) != 0d && !parentFeature.get(index))
                  leafVector.set(index, 0);
            }
          }
        }
      }
    }
  }

  /**
   * A simple {@link Attribute} that stores a wieght for each {@link Synset}.
   */
  private static class ScoreAttribute implements Attribute<Double> {

    /**
     * The {@link Sysnet}'s weight.
     */
    private double score;

    /**
     * Creates a new {@link ScoreAttribute}.
     */
    public ScoreAttribute(double score) {
      this.score = score;
    }

    /**
     * Sums the weights.
     */
    public void merge(Attribute<Double> other) {
      this.score += other.object();
    }

    /**
     * Returns the weight.
     */
    public Double object() {
      return score;
    }
  }
}

