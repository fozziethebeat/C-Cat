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
import gov.llnl.ontology.wordnet.Synset;
import gov.llnl.ontology.wordnet.WordNetCorpusReader;

import edu.ucla.sspace.common.Similarity;

import edu.ucla.sspace.vector.DoubleVector;

import java.util.List;


/**
 * This {@link SynsetPairFeatureMaker} extends the {@link SnowEtAlFeatureMaker}
 * by including the similarities of feature vectors for two {@link Synsets}.
 *
 * @author Keith Stevens
 */
public class ExtendedSnowEtAlFeatureMaker extends SnowEtAlFeatureMaker {

  /**
   * The list of attribute labels that correspond to {@link Synset} feature
   * vectors.
   */
  private final List<String> synsetVectorLabels;

  /**
   * Creates a new {@link ExtendedSnowEtAlFeatureMaker}.  No sense clustering is
   * used.
   */
  public ExtendedSnowEtAlFeatureMaker(WordNetCorpusReader wordnet,
                                      List<String> synsetVectorLabels) {
    this(wordnet, null, synsetVectorLabels);
  }

  /**
   * Creates a new {@link ExtendedSnowEtAlFeatureMaker}.
   */
  public ExtendedSnowEtAlFeatureMaker(WordNetCorpusReader wordnet,
                                      String senseMergedFilename,
                                      List<String> synsetVectorLabels) {
    super(wordnet, senseMergedFilename);
    this.synsetVectorLabels = synsetVectorLabels;
  }

  /**
   * {@inheritDoc}
   */
  protected int numExtraFeatures() {
    return synsetVectorLabels.size();
  }

  /**
   * {@inheritDoc}
   */
  protected void addExtraAttributes(List<String> attributeList) {
    attributeList.addAll(synsetVectorLabels);
  }

  /**
   * Returns a {@link DoubleVector} for the given {@link Synset} corresponding
   * to the feature vector specified by {@code attributeLabel}.
   */
  private DoubleVector getAttribute(Synset synset, String attributeLabel) {
    Attribute attribute = synset.getAttribute(attributeLabel);
    return (DoubleVector) attribute.object();
  }

  /**
   * Adds the distributional similarity score for {@code sense1} and {@code
   * sense2} for each of the specified ontological vectors associated with each
   * synset.  If either word sense lacks one of the ontological vectors, this
   * stores for that feature 0.
   */
  protected void addExtraFeature(Synset sense1, Synset sense2,
                                 DoubleVector featureVector, int index) {
    for (String vectorLabel : synsetVectorLabels) {
      DoubleVector senseVector1 = getAttribute(sense1, vectorLabel);
      DoubleVector senseVector2 = getAttribute(sense2, vectorLabel);

      // Store the similarity if both synsets have the feature vector.
      if (senseVector1 == null || senseVector2 == null)
        featureVector.set(index++, 0);
      else
        featureVector.set(index++, Similarity.cosineSimilarity(
              senseVector1, senseVector2));
    }
  }

  public String toString() {
    return "ExtendedSnowEtAlFeatureMaker";
  }
}
