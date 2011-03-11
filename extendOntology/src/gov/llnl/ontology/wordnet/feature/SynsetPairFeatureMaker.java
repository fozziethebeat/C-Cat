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

import gov.llnl.ontology.wordnet.Synset;

import edu.ucla.sspace.vector.DoubleVector;

import java.util.List;


/**
 * An interface for creating feature vectors that specify how similar two {@link
 * Synset}s are to each other.  Similarity may be measured based on feature
 * vectors that have been applied to each {@link Synset}, their depth in the
 * hierarchy, or any other measure.
 *
 * @author Keith Stevens
 */
public interface SynsetPairFeatureMaker {

  /**
   * Returns a list of attribute labels.  This list must have the same length as
   * the feature vector generated for every {@link Synset} pair.  
   */
  List<String> makeAttributeList();

  /**
   * Returns a feature vector that specifies how similar {@code synset1} is to
   * {@code synset2} according to a variety of measures.
   */
  DoubleVector makeFeatureVector(Synset synset1, Synset synset2);
}

