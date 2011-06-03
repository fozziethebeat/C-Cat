
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

import java.util.TreeSet;
import java.util.Set;

public class Node {

    
    private transient Synset value;
    private transient List children;

    private Node parent;
    private Set<String> keywordSet;

    public Node(Synset value, Node parent, List children, Set<String> keywords){
	this.value = value;
	this.parent = parent;

	if(children == null) this.children = new ArrayList<Node>();
	else {
	    this.children = children;
	}

	keywordSet = keywords;
    }

    public Node(Synset value, Node parent, List children){
	this(value, parent, children, new TreeSet());
    }

    public Node(Synset value, Node parent){
	this(value, parent, null);
    }

    
    public Node(Synset value)
    {
	this(value, null, null);
    }


    public boolean addChild(Node child){
	return children.add(child);
    }




    /* Getters for Tree Node */

    public Set<String> getKeywordSet() {
	return keywordSet;
    }

    public List getChildren() {
	return children;
    }


    public Node getParent () {
	return parent;
    } 

    public Synset nodeValue (){
	return value;

    }

    /* Setters for Tree Node */
    public void setChildren (List new_kids){
	children = new_kids;

    }

    public void setParent(Node new_parent) {
	parent = new_parent;
    }

    /** Equals will check that the Synset have the same sense and lemma */
    public boolean equals (Object other) {
	return this.nodeValue().getName().equals(((Node)other).nodeValue().getName());

    }

    public int compareTo(Node n1, Node n2) {
	return n1.nodeValue().toString().compareTo(n2.nodeValue().toString());
    }


    public String toString() {
	return nodeValue().toString();
    }

}