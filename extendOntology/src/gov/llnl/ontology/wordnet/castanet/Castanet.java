package gov.llnl.ontology.wordnet.castanet;


import gov.llnl.ontology.wordnet.Synset.PartsOfSpeech;
import gov.llnl.ontology.wordnet.Synset;
import gov.llnl.ontology.wordnet.castanet.Node;
import gov.llnl.ontology.wordnet.WordNetCorpusReader;


import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Map;
import java.util.*;


import edu.ucla.sspace.vsm.VectorSpaceModel;
import edu.ucla.sspace.text.FileDocument;
import edu.ucla.sspace.common.*;
import edu.ucla.sspace.vector.VectorIO;


import edu.ucla.sspace.text.IteratorFactory;

import java.io.*;

/**
 *
 * This class implements the Castanet Algorithim described by 
 *
 * @author Terry Huang
 */
public class Castanet {
    
    WordNetCorpusReader reader;


    public Castanet (String wordnet_file_path) {
	reader = WordNetCorpusReader.initialize(wordnet_file_path);

    }
    
    public void extractKeywordsFromDocument(String folderLocation) {
	

	File dir = new File(folderLocation);
	

	// Make sure folderLocation is a working directory
	if(!dir.isDirectory() || !dir.exists()) {
	    System.err.println("extractKeywordsFromDocument: Invalid Directory!");
	    return;
	}

	try{

	    
	    SemanticSpace vsm = new VectorSpaceModel();
	    
	    // Get the list of files and process them
	    for(File doc : dir.listFiles()) {
		
		FileDocument fileDocument = new FileDocument(doc.getCanonicalPath());
		vsm.processDocument(fileDocument.reader());
		
		
	    }

	    System.setProperty(VectorSpaceModel.MATRIX_TRANSFORM_PROPERTY, "edu.ucla.sspace.matrix.TfIdfTransform");
	    vsm.processSpace(System.getProperties());


	    //vsm.processSpace(System.getProperties());
	    // for (String term : vsm.getWords())
	    //	 System.out.println(VectorIO.toString(vsm.getVector(term)));
				    

	    for (String term : vsm.getWords()) {
		edu.ucla.sspace.vector.Vector termVector = vsm.getVector(term);
		
		// Find out if we should print out the values

		boolean toPrint = true;
		String whatPrint = "";
		for(int i = 0; i < termVector.length(); i++) {

		    if(termVector.getValue(i).doubleValue() >= 0.0){
			
		    }
		    
		    whatPrint = whatPrint + termVector.getValue(i)+"\t";
		}
		
		
		System.out.println(term+"\t"+whatPrint);


	
	    }


	}catch(SecurityException se){
	    System.err.println("extractKeywordsFromDocument: Could not read the file because, we have no access!");
	    
	}catch(IOException e){
	    //DEBUG 
	    System.err.println("extractKeywordsFromDocuments: IO ERROR!");
	}
	
    }


    private Node eliminateSingleParents(Node root) {
	
	if(root.getChildren().size() == 0) return root;

	if(root.getChildren().size() == 1 && root.getParent() != null ) {
	    
	    
	    Node child = (Node) root.getChildren().get(0);
	    Node parent = root.getParent();

	    return eliminateSingleParents(child);
	}else {
	    
	    Iterator child_iter = root.getChildren().iterator();
	    List new_children = new ArrayList();
	    
	    while(child_iter.hasNext()) {


		Node child = (Node) child_iter.next();
		new_children.add(eliminateSingleParents(child));
		
	    }

	    
	    root.setChildren(new_children);
	    return root;
	}


    }


    /** Returns a Map that has three lists. 
     *  first - Child nodes only found in first.
     *  second - Child nodes only found in second.
     *  both - Chld nodes found in both node childs.
     */
    private Map getVennMap(Node first, Node second) {
	List first_list = new ArrayList();
	List second_list = new ArrayList();
	List both_list = new ArrayList();

	List<Node> second_node_list = second.getChildren();
	
	Iterator first_iter = first.getChildren().iterator();

	while(first_iter.hasNext()) {

	    Node node = (Node) first_iter.next(); 
	    if (second_node_list.contains(node)) {
		
		// both = first U second
		both_list.add(node);
	    }else{

		// found only in first
		first_list.add(node);

	    }

	}

	// Go through second and remove anything that is shared.
	// Thus we will have a list containing exclusive elemnts of the second node
	
	Iterator second_iter = second_node_list.iterator();

	while(second_iter.hasNext()) {
	    Node second_node = (Node)second_iter.next();
	    
	    if(!both_list.contains(second_node)){
		second_list.add(second_node);
	    }
	}



	TreeMap result = new TreeMap();

	result.put("first",first_list);
	result.put("second", second_list);
	result.put("both",both_list);

	return result;

    }




    /** This merges to graphs together, meaning it takes two graphs, keeps traversing until there is a split, and then attaches the difference to
	a children */
    private Node mergeOntologyGraphs (Node first, Node second) {

	if (first == null && second != null) return second;
	if (second == null && first != null) return first;
	if (first == null && second == null) return null;

	


	if (first.nodeValue().equals(second.nodeValue())) {

	    Map vennMap = getVennMap(first,second);
	    
	    List first_only = (List)vennMap.get("first");
	    List second_only = (List)vennMap.get("second");
	    List both = (List)vennMap.get("both");
	    
	    // new_children is the new list that will replace the first's current children list.
	    // Add children that the first does not already have yet.
	    List new_children = new ArrayList(first_only);
	    new_children.addAll(second_only);
	    
	    Iterator both_iter = both.iterator();
	    
	    while(both_iter.hasNext()){
		
		Node commonNode = (Node)both_iter.next();
		
		// Find the node in the second's children list.
		Node nodeInSecond = (Node) second.getChildren().get(second.getChildren().indexOf(commonNode));

		new_children.add(mergeOntologyGraphs(commonNode, nodeInSecond));
		

	    }

	    
	    first.setChildren(new_children);

	    return first;
	    
	    
	}else {
	    return null;

	}

    }

    private Node createOntologyGraph (List<Synset> pathToRoot, Node parent) {

	// This is the leaf
	if (pathToRoot.size() == 0)  return null;

	Node this_node = new Node (pathToRoot.get(0), parent);
	Node child = createOntologyGraph(pathToRoot.subList(1, pathToRoot.size()), this_node);

	if (child != null)
	    this_node.addChild(child);

	return this_node;
    }

    private void printGraph(Node root) {
	
	System.out.print("[");
	System.out.print(root.nodeValue());

	Iterator child_iter = root.getChildren().iterator();


	while (child_iter.hasNext()) {
	    Node next_child = (Node) child_iter.next();

	    // Use tabs to make it easier to read.
	    System.out.print("<");

	    printGraph(next_child);

	    System.out.print(">");

	}
       System.out.print("]");
    }

    /** 
     *  Takes a word, part of speech and word sense, then creates a Tree based off of its ontology starting from
     *  the root. */
    public Node getOntologyGraph(String word, PartsOfSpeech pos, int senseNum) {

	Synset word_synset = reader.getSynset(word, pos, senseNum);	
	List<List<Synset>> path_to_root = word_synset.getParentPaths();


	// DEBUG: try creating the first node    
	List<Synset> first_path = path_to_root.get(0);


	Node graph = createOntologyGraph(first_path, null);

	return graph;
    }


    public static void main (String[] args) {

	Castanet cnet = new Castanet(args[0]);

	Node pig_graph = cnet.getOntologyGraph("pig", PartsOfSpeech.NOUN, 1);
	Node computer = cnet.getOntologyGraph("computer", PartsOfSpeech.NOUN, 1);

	Node merged = cnet.mergeOntologyGraphs(pig_graph, computer);
	
	File testFile = new File("/Users/thuang513/Projects/research/C-Cat/extendOntology/test-docs/");
	
	try{

	    if(testFile.exists()){
		System.out.println(testFile);

		cnet.extractKeywordsFromDocument(testFile.getCanonicalPath());
	    }else{
		throw new IOException();

	    }
	}catch(IOException ioe){
	    
	    System.err.println("Could not find test file!!!");

	}
    }

}


// A Data structure used to figure out what the keyword of a document is
class Keyword implements Comparable<Keyword> {

    private double value;
    private String word;
    
    Keyword(double tfidf, String theWord) {
	value = tfidf;
	word = theWord;
    }

    int compareTo(Keyword other){
	return Double.compare(getValue(), other.getValue());
    }

    double getValue() { return value; }
    String getWord() { return word; }


}