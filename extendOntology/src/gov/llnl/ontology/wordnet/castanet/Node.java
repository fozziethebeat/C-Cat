
package gov.llnl.ontology.wordnet.castanet;

import java.util.List;
import java.util.ArrayList;


import gov.llnl.ontology.wordnet.Synset;

/**
 *
 * This class is a tree node used in the internal data structure of a Castanet tree.
 *
 * @author Terry Huang
 */
public class Node {

    private Object value;
    private List children;
    private Node parent;
    

    public Node(Object value, Node parent, List children){
	this.value = value;
	this.parent = parent;

	if(children == null) this.children = new ArrayList<Node>();
	else {
	    this.children = children;
	}

    }

    public Node(Object value, Node parent){
	this(value, parent, null);
    }

    
    public Node(Object value)
    {
	this(value, null, null);
    }


    public boolean addChild(Node child){
	return children.add(child);
    }




    /* Getters for Tree Node */

    public List getChildren() {
	return children;
    }


    public Node getParent () {
	return parent;
    } 

    public Object nodeValue (){
	return value;

    }

    /* Setters for Tree Node */
    public void setChildren (List new_kids){
	children = new_kids;

    }

    public void setParent(Node new_parent) {
	parent = new_parent;
    }

}