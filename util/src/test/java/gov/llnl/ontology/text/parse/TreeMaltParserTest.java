package gov.llnl.ontology.text.parse;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

import java.io.*;
import java.util.jar.*;

import edu.ucla.sspace.dependency.DependencyRelation;
import edu.ucla.sspace.dependency.DependencyTreeNode;
import edu.ucla.sspace.dependency.SimpleDependencyTreeNode;


/**
 * @author Keith Stevens
 */
public class TreeMaltParserTest {

    @Test public void testLinearParser() {
        TreeParser parser = new TreeMaltLinearParser();
        DependencyTreeNode[] tree = new DependencyTreeNode[10];
        tree[0] = new SimpleDependencyTreeNode("the", "DT", 0);
        tree[1] = new SimpleDependencyTreeNode("quick", "JJ", 1);
        tree[2] = new SimpleDependencyTreeNode("brown", "JJ", 2);
        tree[3] = new SimpleDependencyTreeNode("fox", "NN", 3);
        tree[4] = new SimpleDependencyTreeNode("jumped", "VBD", 4);
        tree[5] = new SimpleDependencyTreeNode("over", "IN", 5);
        tree[6] = new SimpleDependencyTreeNode("the", "DT", 6);
        tree[7] = new SimpleDependencyTreeNode("lazy", "JJ", 7);
        tree[8] = new SimpleDependencyTreeNode("dog", "NN", 8);
        tree[9] = new SimpleDependencyTreeNode(".", ".", 9);

        DependencyTreeNode[] parsed = parser.parse(tree);

        for (DependencyTreeNode node : parsed) {
            System.out.println(node.toString());
            for (DependencyRelation rel: node.neighbors())
                System.out.println(rel.toString());
        }
    }
}
