
package gov.llnl.ontology.wordnet.castanet;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import java.util.Iterator;

import gov.llnl.ontology.wordnet.Synset;

/**
 *
 * This class is a tree node used in the internal data structure of a Castanet tree.
 *
 * @author Terry Huang
 */
public class Node {

    private transient Synset value;
    private transient List<Node> children;
    private Node parent;
    private Set<String> keywordSet;
    private boolean keywordShouldRegenerate;
    
    public Node(Synset value, Node parent, List children){
	this.value = value;
	this.parent = parent;

	if(children == null) this.children = new ArrayList<Node>();
	else {
	    this.children = children;
	}
	
	keywordSet = new TreeSet();
	keywordShouldRegenerate = true;
	
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


    /** 
     * Generates a set of all the actual keywords that are part of this node and all of its descendents.
     * @return A set of all the actual extracted keywords that can be found in the descendents of this node (including itself).
     */
    public Set<String> generateKeywordSet() {


	// If this node is a leaf node (i.e. it has no children)
	// then it should just add its keyword and return the set with a single member.
	if (children.size() == 0) {

	    String currentKeyword = toString();
	    keywordSet.add(currentKeyword);
	}else{

	    // If this node is a non leaf node, then it will compile
	    // a Set of keywords that its descendants have.
	    if(keywordShouldRegenerate) {
		for(Node child : children) {
		    Set<String> childKeywords = child.generateKeywordSet();
		    Iterator childIter = childKeywords.iterator();
		    
		    while(childIter.hasNext()) {
			String childKeyword = (String) childIter.next();
			keywordSet.add(childKeyword);
		    }
		}

		keywordShouldRegenerate = false;
	    


		// Also if the current node has a file attached, also throw in its keyword too.
		if(value.getAttribute("files") != null){
		    String currentKeyword = toString();
		    keywordSet.add(currentKeyword);
		    
		}
	    }
	}
	
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

    
    /** Setter functions */





}