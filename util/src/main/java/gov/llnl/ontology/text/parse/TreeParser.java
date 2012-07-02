package gov.llnl.ontology.text.parse;

import edu.ucla.sspace.dependency.DependencyTreeNode;


/**
 * @author Keith Stevens
 */
public interface TreeParser {

    /**
     * Parses a unconnected dependency tree and fills in relational links
     * between nodes in the returned structure.  The given tree should be
     * already part of speech tagged and contain any known lemmas.  The returned
     * structure will be a new structure.
     */
    DependencyTreeNode[] parse(DependencyTreeNode[] tree);
}
