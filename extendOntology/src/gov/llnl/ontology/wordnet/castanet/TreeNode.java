
package gov.llnl.ontology.wordnet.castanet;

import java.util.List;

import gov.llnl.ontology.wordnet.Synset;

/**
 *
 * This class is a tree node used in the internal data structure of a Castanet tree.
 *
 * @author Terry Huang
 */
public class TreeNode {

    private Synset synset;
    private List children;
    private TreeNode parent;
    

    public TreeNode(Synset value, TreeNode parent, List children){
	this.synset = value;
	this.parent = parent;
	this.children = children;

    }


    public boolean addChild(TreeNode child){
	return children.add(child);
    }

    public List getChildren() {
	return children;
    }


    public TreeNode getParent () {
	return parent;
    } 

    public Synset nodeValue (){
	return synset;

    }



}