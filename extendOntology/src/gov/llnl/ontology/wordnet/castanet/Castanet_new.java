package gov.llnl.ontology.wordnet.castanet;

import gov.llnl.ontology.wordnet.Synset;
import gov.llnl.ontology.wordnet.Synset.PartsOfSpeech;

/**
 *  This class represents the Castanet tree.
 */
public class CastanetTree {
    
    /** Adds a word to the Castanet tree. */
    public void addWord(String word, PartsOfSpeech pos, int senseNum) {}

    /** Get the root to the tree **/
    public Node getRoot() {}
    
    /**
     * Follows the {@code path} given starting from {@code rootNode}. 
     * Returns null if the path is invalid.
     */
    public Node getNodeAtPath(Node rootNode, String[] path) { }

     /** 
     *  Takes a {@link Synset}, then creates a Tree based off of its ontology starting from
     *  the root. 
     */
    private Node createOntologyChain(Synset s) {}

    /**
     * Merges two chains of nodes together based of their node values.
     */
    private Node mergeOntologyTrees(Node first, Node second) { }
}