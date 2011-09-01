package gov.llnl.ontology.mains;

import edu.ucla.sspace.matrix.SparseMatrix;
import edu.ucla.sspace.matrix.YaleSparseMatrix;
import edu.ucla.sspace.vector.SparseDoubleVector;


/**
 * @author Keith Stevens
 */
public class ChineseWhispersTest {

    public static void main(String[] args) {
        double[][] adjacencyData = {
            {0, 1, 1, 0, 0, 0, 0},
            {1, 0, 0, 1, 0, 0, 0},
            {1, 0, 0, 0, 1, 1, 0},
            {0, 1, 0, 0, 1, 0, 1},
            {0, 0, 1, 1, 0, 1, 0},
            {0, 0, 1, 0, 1, 0, 1},
            {0, 0, 0, 1, 0, 1, 0},
        };
        SparseMatrix A = new YaleSparseMatrix(7, 7);
        for (int r = 0; r < 7; ++r)
            for (int c = 0; c < 7; ++c)
                if (adjacencyData[r][c] != 0d)
                    A.set(r, c, adjacencyData[r][c]);
        SparseMatrix D = new YaleSparseMatrix(7, 7);
        for (int r = 0; r < 7; ++r)
            D.set(r, r, 1);

        for (int i = 0; i < 4; ++i) {
            for (int r = 0; r < 7; ++r) {
                SparseDoubleVector row = D.getRowVector(r);
                int[] nonZeros = row.getNonZeroIndices();
                double maxValue = 0;
                int maxColumn = 0;
                for (int c = 0; c < 7; ++c) {
                    double s = 0;
                    for (int nonZero : nonZeros)
                        s += row.get(nonZero) * A.get(nonZero, c);
                    if (s > maxValue ||
                        (s > 0 && s == maxValue && Math.random() > .50)) {
                        maxValue = s;
                        maxColumn = c;
                    }
                }

                for (int nonZero : nonZeros)
                    D.set(r, nonZero, 0);
                D.set(r, maxColumn, maxValue);
                System.out.printf("%d, %d -> %f\n", r, maxColumn, maxValue);
            }
        }
    }
}

