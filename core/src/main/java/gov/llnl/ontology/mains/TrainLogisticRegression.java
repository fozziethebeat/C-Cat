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

package gov.llnl.ontology.mains;

import gov.llnl.ontology.mapreduce.table.EvidenceTable;
import gov.llnl.ontology.util.Counter;
import gov.llnl.ontology.util.MahoutSparseVector;
import gov.llnl.ontology.util.MRArgOptions;
import gov.llnl.ontology.wordnet.OntologyReader;
import gov.llnl.ontology.wordnet.SynsetRelations;
import gov.llnl.ontology.wordnet.SynsetRelations.HypernymStatus;
import gov.llnl.ontology.wordnet.WordNetCorpusReader;

import edu.ucla.sspace.basis.StringBasisMapping;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.util.SerializableUtil;

import org.apache.mahout.classifier.OnlineLearner;
import org.apache.mahout.classifier.sgd.AdaptiveLogisticRegression;
import org.apache.mahout.classifier.sgd.AdaptiveLogisticRegression.Wrapper;
import org.apache.mahout.classifier.sgd.CrossFoldLearner;
import org.apache.mahout.classifier.sgd.L1;
import org.apache.mahout.classifier.sgd.OnlineLogisticRegression;
import org.apache.mahout.ep.State;

import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;

import java.io.File;
import java.util.Iterator;
import java.util.Map;


/**
 * @author Keith Stevens
 */
public class TrainLogisticRegression {

    public static void main(String[] args) throws Exception {
        MRArgOptions options = new MRArgOptions();
        options.addOption('w', "wordnetDir",
                          "Specifies the wordnet directory",
                          true, "PATH", "Required");
        options.addOption('b', "basisMapping",
                          "Specifies a serialzied basis mapping",
                          true, "FILE", "Required");
        options.addOption('n', "numPasses",
                          "Specifies the number of training passes to make. " +
                          "(Default: 5)",
                          true, "INT", "Optional");
        options.parseOptions(args);

        if (options.numPositionalArgs() != 1 ||
            !options.hasOption('w') ||
            !options.hasOption('b')) {
            System.out.println(
                    "usage: java TrainLogisticRegression [OPTIONS] <out>\n" +
                    options.prettyPrint());
            System.exit(1);
        }

        EvidenceTable table = options.evidenceTable();
        Scan scan = new Scan();
        table.setupScan(scan, options.sourceCorpus());

        StringBasisMapping basis = SerializableUtil.load(new File(
                    options.getStringOption('b')));
        basis.setReadOnly(true);
        int numDimensions = basis.numDimensions();

        AdaptiveLogisticRegression model = new AdaptiveLogisticRegression(
                2, numDimensions, new L1());

        int numPasses = options.getIntOption('n');
        for (int i = 0; i < numPasses; ++i) {
            Iterator<Result> resultIter = table.iterator(scan);
            while (resultIter.hasNext()) {
                Result row = resultIter.next();

                HypernymStatus status = table.getHypernymStatus(row);
                if (status == HypernymStatus.TERMS_MISSING ||
                    status == HypernymStatus.NOVEL_HYPONYM ||
                    status == HypernymStatus.NOVEL_HYPERNYM)
                    continue;

                // Extract a CompactSparse vector representing the number of
                // dependency paths.
                SparseDoubleVector vector =
                    new CompactSparseVector(numDimensions);
                Counter<String> pathCounts = table.getDependencyPaths(row);
                for (Map.Entry<String, Integer> entry : pathCounts) {
                    int dimension = basis.getDimension(entry.getKey());
                    if (dimension >= 0)
                        vector.set(dimension, entry.getValue());
                }

                int classLabel = (status == HypernymStatus.KNOWN_HYPERNYM)
                    ? 1
                    : 0;
                model.train(classLabel, new MahoutSparseVector(
                            vector, numDimensions));
            }
        }

        State<Wrapper, CrossFoldLearner> best = model.getBest();
        if (best == null) {
            System.err.println("The Learner could not be learned");
            System.exit(1);
        }

        OnlineLearner classifier =
            best.getPayload().getLearner().getModels().get(0);
        SerializableUtil.save(classifier,
                              new File(options.getPositionalArg(0)));
    }
}

