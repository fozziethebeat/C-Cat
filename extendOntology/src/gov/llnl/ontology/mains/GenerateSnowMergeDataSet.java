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

package gov.llnl.ontology.mains;

import gov.llnl.ontology.wordnet.Synset;
import gov.llnl.ontology.wordnet.Synset.PartsOfSpeech;
import gov.llnl.ontology.wordnet.WordNetCorpusReader;

import gov.llnl.ontology.wordnet.feature.ExtendedSnowEtAlFeatureMaker;
import gov.llnl.ontology.wordnet.feature.OntologicalFeatureMaker;
import gov.llnl.ontology.wordnet.feature.SnowEtAlFeatureMaker;
import gov.llnl.ontology.wordnet.feature.SynsetPairFeatureMaker;

import edu.ucla.sspace.common.ArgOptions;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.SemanticSpaceIO;

import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.SparseDoubleVector;

import gov.llnl.text.util.FileUtils;

import java.io.IOException;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author Keith Stevens
 */
public class GenerateSnowMergeDataSet {

  public enum DataSet {
    SNOW,
    SNOW_EXTENDED,
  }

  public static void main(String[] args) throws IOException {
    ArgOptions options = new ArgOptions();

    options.addOption('w', "wordNetDir",
                      "Specifies the word net dictionary path.",
                      true, "PATH", "Required");
    options.addOption('f', "featureSetType",
                      "Specifies the type of data set to generate.",
                      true, "SNOW|SNOW_EXTENDED", "Required");

    options.addOption('c', "senseClustering",
                      "Specifies a file that lists which synsets have " +
                      "been merged according to a known standard.",
                      true, "FILE", "Optional");
    options.addOption('s', "sspaceFiles",
                      "Specifies a series of sspace file and, optionally, " +
                      "term weights, that should be used to generate extra " +
                      "features.  Required when SNOW_EXTENDED is used.",
                      true, "(<sspace>[:<featureWeight>])[,\\1]*", "Optional");

    options.parseOptions(args);

    if (!options.hasOption('w') || !options.hasOption('f')) {
      System.out.println("usage: java GenerateSnowMergeDataSet [options]\n" +
                         options.prettyPrint());
      System.exit(1);
    }

    WordNetCorpusReader wordnet = WordNetCorpusReader.initialize(
        options.getStringOption('w'));

    DataSet setType = DataSet.valueOf(
        options.getStringOption('f').toUpperCase());

    String senseClustering = (options.hasOption('c'))
      ? options.getStringOption('c')
      : null;

    SynsetPairFeatureMaker featureMaker;
    switch (setType) {
      case SNOW:
        featureMaker = new SnowEtAlFeatureMaker(wordnet, senseClustering);
        break;
      case SNOW_EXTENDED: {
        List<String> synsetLabels = new ArrayList<String>();
        for (String item : options.getStringOption('s').split(",")) {
          String[] sspaceAndWeight = item.split(":");
          SemanticSpace sspace = SemanticSpaceIO.load(sspaceAndWeight[0]);

          Map<String, Double> termWeights = null;
          if (sspaceAndWeight.length == 2) {
            termWeights = new HashMap<String, Double>();
            for (String line : FileUtils.iterateFileLines(sspaceAndWeight[2])) {
              String tokens[] = line.split("\\s+");
              termWeights.put(tokens[0], Double.parseDouble(tokens[1]));
            }
          }

          OntologicalFeatureMaker maker = new OntologicalFeatureMaker(
              sspace, termWeights);
          maker.induceOntologicalFeatures(
              wordnet.getSynset("entity", PartsOfSpeech.NOUN, 1));
          synsetLabels.add(maker.getAttributeName());
        }

        featureMaker = new ExtendedSnowEtAlFeatureMaker(
            wordnet, senseClustering, synsetLabels);
        break;
      }
      default:
        featureMaker = null;
    }

    List<String> attributeList = featureMaker.makeAttributeList();

    for (PartsOfSpeech pos : PartsOfSpeech.values()) {

      System.out.println("Forming data set for: " + pos);

      PrintWriter writer = new PrintWriter(String.format(
            "%s-%s.arff", featureMaker.toString(), pos.toString()));

      writer.println("@relation " + featureMaker.toString());
      writer.println();

      for (int i = 0; i < attributeList.size() - 1; ++i)
        writer.printf("@attribute %s numeric\n", attributeList.get(i));
      writer.printf("@attribute class {0,1}\n");

      writer.println();
      writer.println("@data");

      for (String lemma : wordnet.wordnetTerms(pos)) {
        Synset[] synsets = wordnet.getSynsets(lemma, pos);

        for (int i = 0; i < synsets.length; ++i) {
          Synset sense1 = synsets[i];

          for (int j = i+1; j < synsets.length; ++j) {
            Synset sense2 = synsets[j];

            DoubleVector featureVector = featureMaker.makeFeatureVector(
                sense1, sense2);

            if (featureVector == null)
              continue;

            StringBuilder sb = new StringBuilder();
            if (featureVector instanceof SparseDoubleVector) {
              SparseDoubleVector sfv = (SparseDoubleVector) featureVector;
              sb.append("{");
              for (int index : sfv.getNonZeroIndices())
                sb.append(String.format("%d %f,", index, sfv.get(index)));
              sb.append("}");
            } else {
              for (int index = 0; index < featureVector.length() - 1; ++index)
                sb.append(featureVector.get(index)).append(",");
              sb.append((int) featureVector.get(featureVector.length()-1));
            }

            writer.println(sb.toString());
          }
        }
      }
      writer.close();
    }
  }
}

