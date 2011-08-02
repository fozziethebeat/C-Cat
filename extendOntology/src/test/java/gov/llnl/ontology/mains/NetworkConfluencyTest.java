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

import edu.ucla.sspace.matrix.SparseMatrix;
import edu.ucla.sspace.matrix.YaleSparseMatrix;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class NetworkConfluencyTest {

    public static SparseMatrix makeMatrix(double[][] values) {
        SparseMatrix m = new YaleSparseMatrix(values.length, values.length);
        for (int r = 0; r < values.length; ++r) {
            double numNonZeros = 0;
            for (int c = 0; c < values.length; ++c)
                if (values[r][c] != 0d)
                    numNonZeros++;
            for (int c = 0; c < values.length; ++c)
                if (values[r][c] != 0d)
                    m.set(r, c, values[r][c]/numNonZeros);
        }
        return m;
    }

    @Test public void testInitialAgreement() {
        double[][] v1 = new double[][] {
            {1, 1, 0, 0, 1, 0},
            {0, 0, 1, 0, 1, 0},
            {0, 1, 0, 0, 0, 1},
            {0, 1, 1, 1, 0, 0},
            {1, 0, 1, 0, 0, 1},
            {1, 1, 0, 0, 1, 1},
        };
        double[][] v2 = new double[][] {
            {1, 0, 0, 0, 1, 0},
            {0, 0, 0, 0, 1, 0},
            {0, 1, 1, 0, 0, 1},
            {0, 1, 1, 1, 1, 0},
            {1, 0, 1, 1, 0, 1},
            {1, 1, 0, 0, 1, 1},
        };

        int agreeMent = 0;
        int edges1 = 0;
        int notEdges1 = 0;
        int edges2 = 0;
        int notEdges2 = 0;
        double numEdges = v1.length * v1.length;
        for (int r = 0; r < v1.length; ++r)
            for (int c = 0; c < v1.length; ++c) {
                if (v1[r][c] == v2[r][c])
                    agreeMent++;
                if (v1[r][c] != 0d)
                    edges1++;
                else
                    notEdges1++;
                if (v2[r][c] != 0d)
                    edges2++;
                else
                    notEdges2++;
            }

        double p0 = 1/numEdges * agreeMent;
        double pe = 1/(numEdges*numEdges) *
                    (edges1*edges2 + notEdges1*notEdges2);
        double expectedKappa = (p0 - pe) / (1 - pe);

        NetworkConfluency confluency = new NetworkConfluency(
                makeMatrix(v1), makeMatrix(v2));
        assertEquals(expectedKappa, confluency.computeInitialAgreement(), .001);
    }

    @Test public void testExtendedAgreement() {
        double[][] v1 = new double[][] {
            {0, 0, 1, 0, 0, 0},
            {1, 0, 0, 0, 0, 0},
            {1, 0, 0, 0, 0, 0},
            {0, 1, 0, 1, 1, 1},
            {1, 1, 0, 1, 1, 1},
            {1, 1, 0, 1, 1, 1},
        };
        double[][] v2 = new double[][] {
            {1, 1, 1, 0, 0, 0},
            {1, 1, 1, 0, 0, 0},
            {1, 1, 1, 0, 0, 0},
            {0, 0, 0, 1, 1, 0},
            {0, 0, 0, 1, 1, 1},
            {0, 0, 0, 1, 1, 1},
        };

        NetworkConfluency confluency = new NetworkConfluency(
                makeMatrix(v1), makeMatrix(v2));
        assertTrue(confluency.computeInitialAgreement() <
                   confluency.computeExtendedAgreement());
    }
}
