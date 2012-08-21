package gov.llnl.ontology.text.parse;

import edu.ucla.sspace.dependency.DependencyRelation;
import edu.ucla.sspace.dependency.SimpleDependencyRelation;
import edu.ucla.sspace.dependency.SimpleDependencyTreeNode;

import java.util.List;


/**
 * @author Keith Stevens
 */
public class Link {
    public int dep;
    public int head;
    public String rel;

    public Link(int dep, String rel, int head) {
        this.dep = dep;
        this.rel = rel;
        this.head = head;
    }

    public static void addLinksToTree(List<SimpleDependencyTreeNode> tree,
                                      List<Link> links) {
        for (Link link : links) {
            // Skip the root node, which always has a head id of 0.
            if (link.head == 0)
                continue;
            SimpleDependencyTreeNode dep = tree.get(link.dep);
            SimpleDependencyTreeNode head = tree.get(link.head-1);
            DependencyRelation rel = new SimpleDependencyRelation(head, link.rel, dep);
            dep.addNeighbor(rel);
            head.addNeighbor(rel);
        }
    }
}
