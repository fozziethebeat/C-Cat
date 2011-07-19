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

import gov.llnl.ontology.util.ExtendedSet;

import com.google.common.collect.Sets;

import edu.stanford.nlp.util.IntPair;

import edu.ucla.sspace.common.ArgOptions;
import edu.ucla.sspace.matrix.ArrayMatrix;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.MatrixIO;
import edu.ucla.sspace.matrix.MatrixIO.Format;
import edu.ucla.sspace.matrix.SparseMatrix;
import edu.ucla.sspace.vector.SparseDoubleVector;

import java.io.IOException;
import java.io.IOError;
import java.io.File;
import java.util.Set;


/**
 * @author Keith Stevens
 */
public class NetworkConfluency {

    private final SynonymNetworkInfo info1;

    private final SynonymNetworkInfo info2;

    private final int numNodes;

    public NetworkConfluency(String matrixFile1,
                             String matrixFile2, 
                             Format format) throws IOException {
        info1 = new SynonymNetworkInfo(matrixFile1, format);
        info2 = new SynonymNetworkInfo(matrixFile2, format);
        numNodes = info1.convergentRate.length;
    }

    public NetworkConfluency(SparseMatrix matrix1, SparseMatrix matrix2) {
        info1 = new SynonymNetworkInfo(matrix1);
        info2 = new SynonymNetworkInfo(matrix2);
        numNodes = info1.convergentRate.length;
    }

    public double computeInitialAgreement() {
        return computeKappaScore(
                info1.edges, info2.edges, numNodes);
    }

    public double computeExtendedAgreement() {
        info1.mergeWithConfluence(info2);
        info2.mergeWithConfluence(info1);
        // Now compute the kappa score
        return computeKappaScore(
                info1.modifiedEdges, info2.modifiedEdges, numNodes);
    }

    public static void main(String[] args) throws IOException {
        ArgOptions options = new ArgOptions();
        options.addOption('f', "matrixFormat",
                          "Specifies the matrix file format that both input " +
                          "matrices use.",
                          true, "FORMAT", "Required");
        options.parseOptions(args);

        Format format = Format.valueOf(options.getStringOption('f'));
        NetworkConfluency confluency = new NetworkConfluency(
                options.getPositionalArg(0),
                options.getPositionalArg(1), 
                format);

        System.out.printf("Pre-Kappa score: %f\n",
                          confluency.computeInitialAgreement());
        System.out.printf("Post-Kappa score: %f\n", 
                          confluency.computeExtendedAgreement());
    }

    public static double computeKappaScore(Set<IntPair> edges1,
                                           Set<IntPair> edges2,
                                           int numNodes) {
        int agreeExist = 0;
        double agreeNonExist = 0;
        int disagreeIn1 = 0;
        int disagreeIn2 = 0;
        for (IntPair edge : edges1) {
            if (edges2.contains(edge))
                agreeExist++;
            else
                disagreeIn1++;
        }
        for (IntPair edge : edges2)
            if (!edges1.contains(edge))
                disagreeIn2++;
        double numEdges = numNodes * numNodes;
        agreeNonExist = numEdges - agreeExist - disagreeIn1 - disagreeIn2;

        double p0 = 1/numEdges * (agreeExist + agreeNonExist);

        double numEdges1 = edges1.size();
        double numEdges2 = edges2.size();
        double numNonEdges1 = numEdges - numEdges1;
        double numNonEdges2 = numEdges - numEdges2;
        double pe = 1/(numEdges * numEdges) *
                    ((numEdges1 * numEdges2) + (numNonEdges1 * numNonEdges2));
        return (p0 - pe) / (1 - pe);
    }

    public class SynonymNetworkInfo {

        public Matrix confluences;

        public double[] convergentRate;

        public Set<IntPair> edges;

        public Set<IntPair> modifiedEdges;

        public SynonymNetworkInfo(String matrixFile, Format format)
                throws IOException{
            this((SparseMatrix) MatrixIO.readMatrix(
                    new File(matrixFile), format));
        }

        public SynonymNetworkInfo(SparseMatrix A) {
            // First setup the convergent rate in the limit of long random walks
            // for each data point in marked in the adjacency matrix.  While
            // doing this, also create the set of edge information for this
            // graph so we can easily determine which edges exist in each graph.
            convergentRate = new double[A.rows()];
            edges = Sets.newHashSet();
            double totalDegreeCount = 0;
            for (int r = 0; r < A.rows(); ++r) {
                int[] nonZeros = A.getRowVector(r).getNonZeroIndices();
                for (int c : nonZeros)
                    edges.add(new IntPair(r, c));

                int numNonZero = nonZeros.length;
                totalDegreeCount += numNonZero;
                convergentRate[r] = numNonZero;
            }

            for (int r = 0; r < A.rows(); ++r) 
                convergentRate[r] /= totalDegreeCount;

            // Now compute the confluence with a two step random walk through
            // the adjacency matrix.
            confluences = multiply(A, A);
        }

        public void mergeWithConfluence(SynonymNetworkInfo other) {
            this.modifiedEdges = new ExtendedSet<IntPair>(this.edges);
            for (IntPair edge : other.edges) {
                // If both agree, we make no modification.
                if (this.edges.contains(edge))
                    continue;
                // If the other graph has the link and this graph has high
                // confluence, add a new link to this graph.
                int r = edge.getSource();
                int c = edge.getTarget();
                if (confluences.get(r, c) > convergentRate[r])
                    this.modifiedEdges.add(edge);
            }
        }
    }

    public static Matrix multiply(SparseMatrix a, SparseMatrix b) {
        Matrix C = new ArrayMatrix(a.rows(), b.columns());
        for (int r = 0; r < C.rows(); ++r) {
            SparseDoubleVector v = a.getRowVector(r);
            int[] nonZeros = v.getNonZeroIndices();
            for (int c = 0; c < C.columns(); ++c) {
                double sum = 0;
                for (int i : nonZeros)
                    sum += v.get(i) * b.get(i, c);
                C.set(r, c, sum);
            }
        }
        return C;
    }
}
