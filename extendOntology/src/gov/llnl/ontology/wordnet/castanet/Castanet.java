package gov.llnl.ontology.wordnet.castanet;


import gov.llnl.ontology.wordnet.Synset.PartsOfSpeech;
import gov.llnl.ontology.wordnet.Synset;
import gov.llnl.ontology.wordnet.castanet.Node;
import gov.llnl.ontology.wordnet.WordNetCorpusReader;
import gov.llnl.ontology.wordnet.Attribute;
import gov.llnl.ontology.wordnet.FileListAttribute;

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



import java.util.logging.Logger;

import java.io.*;




/**
 *  A Data structure used to figure out what the keyword of a document is.
 * 
 */
class Keyword implements Comparable<Keyword> {

    private double value;
    private String word;

    
    Keyword(String theWord, double tfidf) {
	value = tfidf;
	word = theWord;
    }


    public int compareTo(Keyword other){
	return Double.compare(getValue(), other.getValue());
    }

    public double getValue() { return value; }
    public String getWord() { return word; }



    public String toString() { return word + " - "+value; } 

}



/**
 *
 * This class implements the Castanet Algorithim described by 
 *
 *   <li style="font-family:Garamond, Georgia, serif"> E. Stoica, M. A. Hearst, and M. Richardson. Automating
 *  creation of hierarchical faceted metadata structures. In Proc.
 *  NAACL-HLT 2007, pages 244–251, 2007.
 *
 * @author Terry Huang
 */
public class Castanet {
    
    private WordNetCorpusReader reader;
    private String stopWordsFile;
    private String wordnetFilePath;
    /**
     * The logger used to record all output
     */
    private static final Logger LOGGER = 
        Logger.getLogger(Castanet.class.getName());


    public Castanet (String wordnet_file_path) {
	this(wordnet_file_path, "");
    }


    public Castanet (String wordnet_file_path, String stopWordFile) {
	reader = WordNetCorpusReader.initialize(wordnet_file_path);
	wordnetFilePath = wordnet_file_path;
	stopWordsFile = stopWordFile;
    }


    /*** 
     *  Extracts keywords for all documents in {@code folderLocation}. It returns a Map with two
     *  key/value pairs.
     * @return - The key {@code files} returns a list of File objects of all the documents in the folder.
     *  - The key {@code keywords} returns a list of lists that each contain extracted keywords for each document (in ranked order based off TFIDF and other transforms).
     *    The order of the list of lists is in the same order as the list of documents.
     *   - Returns null if failed.
     * @param folderLocation The directory that contains all the documents
     * @param topN The top N keywords after TFIDF transformation..
     * 
     */
    public static Map extractKeywordsFromDocument(String folderLocation, int topN, String stopWordsFile) {
	
	
	if(folderLocation.equals("")) return null;


	File dir = new File(folderLocation);
	
	// Make sure folderLocation is a working directory
	if(!dir.isDirectory() || !dir.exists()) {
	    System.err.println("extractKeywordsFromDocument: Invalid Folder Directory = "+folderLocation);
	    return null;
	}
	
	Map<String, List> results = new TreeMap<String, List>();
	
	// Put the list of documents into the map.
	results.put("files", Arrays.asList(dir.listFiles()) );

	
	try{

	    
	    SemanticSpace vsm = new VectorSpaceModel();

	    // We want to keep track of all the keywords extracted from each document.
	    // This contains a list of list. All the elements are lists of top keywords
	    // for a document.
	    // The list (not the element list) is in the same order as dir.listFiles().
	    // This is eventually used to rank what are the top keywords.
	    List<List> listOfDocs = new ArrayList<List>(dir.listFiles().length);
	    

	    /*
	    // If we have a stop word list, then use it.
	    if(!stopWordsFile.equals("")){

		LOGGER.info("setting the stop word file = "+stopWordsFile);
		System.setProperty(IteratorFactory.TOKEN_FILTER_PROPERTY, "exclude="+stopWordsFile);
		IteratorFactory.setProperties(System.getProperties());
	    }
	    */



	    // TODO: use Sakai et al. method. to extract keywords.
	    Map<String, Double> wordScores = Autosummary.calculateSakaiEtAlScore(folderLocation,stopWordsFile);
	    

	    // Get the list of files and process them
	    for(File doc : dir.listFiles()) {
		
		FileDocument fileDocument = new FileDocument(doc.getCanonicalPath());
		vsm.processDocument(fileDocument.reader());
	       
		listOfDocs.add(new ArrayList<Keyword>());
		
	    }
	    

	    

	    // Try out some different matrix transform properties
	    
	    /*

	    String[] matrixTransforms = {"edu.ucla.sspace.matrix.TfIdfTransform"};

	    for (int transform = 0; transform < matrixTransforms.length; transform++){
		
		LOGGER.info("Applying transform = " + matrixTransforms[transform]);

		System.setProperty(VectorSpaceModel.MATRIX_TRANSFORM_PROPERTY, matrixTransforms[transform]);
		vsm.processSpace(System.getProperties());
	    
	    }
	    
	     LOGGER.info("Done with TF-IDF Transform...");

	    */
	    
	    
	    // Finalize the internal matrix
	    vsm.processSpace(System.getProperties());
		
		
	    for (String term : vsm.getWords()) {

		// DEBUG
		//		System.out.println(term);


		// Get all the terms found in the document
		edu.ucla.sspace.vector.Vector termVector = vsm.getVector(term);
		
		for(int i = 0; i < termVector.length(); i++) {
		    
		    // If the term existed in the document then add it to the document's list of keywords
		    // with the term's score.

		    if(termVector.getValue(i).doubleValue() != 0.0 && 
		       termVector.getValue(i).doubleValue() != Double.POSITIVE_INFINITY &&
		       termVector.getValue(i).doubleValue() != Double.NEGATIVE_INFINITY){
			Double keywordScore = wordScores.get(term);			

			if(keywordScore.doubleValue() != Double.POSITIVE_INFINITY && 
			   keywordScore.doubleValue() != Double.NEGATIVE_INFINITY){

			    Keyword keyword = new Keyword(term, keywordScore.doubleValue());
			
			    // DEBUG
			    System.err.println(keyword +"\t"+keywordScore);
			    
			    ((List)listOfDocs.get(i)).add(keyword);
			}



		    }
		}
		

	
	    }
	    
	    List documentKeywords = new ArrayList(dir.listFiles().length);
	    
	    LOGGER.info("Sorting the keywords by their TFIDF score...");


	    // Sort all the keywords by the TFIDF Ranking
	    for(List docTerms : listOfDocs) {
		Collections.sort(docTerms);
		Collections.reverse(docTerms);
		
		List top_N_terms = new ArrayList(topN);
		
		// Print out the top N terms
		for(int i = 0; i < topN && i  < docTerms.size(); i++){
		    
		    top_N_terms.add(docTerms.get(i));
		    System.out.println(i+". "+docTerms.get(i));
		    
		}
		
		documentKeywords.add(top_N_terms);

		//		System.out.println("------");
	    }

	    results.put("keywords", documentKeywords);

	}catch(SecurityException se){
	    System.err.println("extractKeywordsFromDocument: Could not read the file because, we have no access!");
	    
	}catch(IOException e){
	    //DEBUG 
	    System.err.println("extractKeywordsFromDocuments: IO ERROR!");
	}

	
	return results;


    }


    private Node eliminateSingleParents(Node root) {
	
	if(root.getChildren().size() == 0) return root;
	
	if(root.getChildren().size() == 1 && root.getParent() != null && root.nodeValue().getAttribute("files") == null ) {
	    
	    
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


    /** Merges the Synset attributes of both nodes. The receiver Node, will get all the attributes
     * the giver Node has */
    private void mergeAttributes(Node receiver, Node giver) {
	
	java.util.Set receiverLabel = receiver.nodeValue().attributeLabels();
	java.util.Set giverLabel = giver.nodeValue().attributeLabels();

	Iterator giverIter = giverLabel.iterator();
	
	while(giverIter.hasNext()) {
	    
	    String attLabel = (String)giverIter.next();
	    Attribute giveAttr = giver.nodeValue().getAttribute(attLabel);
    
	    // If the reciever has the same attribute, then merge them.
	    if(receiverLabel.contains(attLabel)){
		
		Attribute recAttr = receiver.nodeValue().getAttribute(attLabel);
		
		
		recAttr.merge(giveAttr);


	    }else {
		
		receiver.nodeValue().setAttribute(attLabel, giveAttr);

	    }
	}
	
	    // Otherwise add teh attribute
	    


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
		

		// This should have the node in the 'first' Node.
		Node commonNode = (Node)both_iter.next();
		
		// Find the node in the second's children list. (The same node in the 'second' Node).
		Node nodeInSecond = (Node) second.getChildren().get(second.getChildren().indexOf(commonNode));
		
		
		// Merge the attributes of the two common children Nodes.
		//	mergeAttributes(commonNode, nodeInSecond);
		
		new_children.add(mergeOntologyGraphs(commonNode, nodeInSecond));
		

	    }

	    
	    
	    // Merge the attributes of the 'first' and 'second' Node
	    //    mergeAttributes(first, second);
	    

	    first.setChildren(new_children);

	    return first;
	    
	    
	}else {
	    return null;

	}

    }


    /** Creates a node tree structure all the way down to the leaf */
    private Node createOntologyGraph (List<Synset> pathToRoot, Node parent) {

	// This is the leaf
	if (pathToRoot.size() == 0)  return null;

	Node this_node = new Node (pathToRoot.get(0), parent);
	Node child = createOntologyGraph(pathToRoot.subList(1, pathToRoot.size()), this_node);

	// This is a non leaf node. 
	if (child != null) 
	    this_node.addChild(child);
	 



	return this_node;
    }



    /** Used for debugging purposes... */
    private void printGraph(Node root) {
	
	System.out.print("[");
	System.out.print(root.nodeValue().getName());

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

    public void printGraph(Node root, PrintWriter out) {
	
	out.print("[");
	out.print(root.nodeValue().getName());

	Iterator child_iter = root.getChildren().iterator();


	while (child_iter.hasNext()) {
	    Node next_child = (Node) child_iter.next();

	    // Use tabs to make it easier to read.
	    out.print("<");

	    printGraph(next_child, out);

	    out.print(">");

	}
       out.print("]");
    }

    

    /** 
     *  Takes a word, part of speech and word sense, then creates a Tree based off of its ontology starting from
     *  the root. */
    public Node getOntologyGraph(String word, PartsOfSpeech pos, int senseNum) {

	Synset word_synset = reader.getSynset(word, pos, senseNum);	
	List<List<Synset>> path_to_root = word_synset.getParentPaths();
	
	
	Node graph = null;
	
	// For all the paths to the parent, we must merge them together into one tree.
	for(List<Synset> this_path_to_root : path_to_root) {
	   
	    Node this_synset_graph = createOntologyGraph(this_path_to_root, null);
	    

	    if(graph == null) {
		graph = this_synset_graph;
	    }else {
		graph = mergeOntologyGraphs(graph, this_synset_graph);
		
	    }


	}

	return graph;

    }

   
 

    /** 
     *  Takes a {@link Synset}, then creates a Tree based off of its ontology starting from
     *  the root. */
    public Node getOntologyGraph(Synset s) {

	Synset word_synset = s;	
	List<List<Synset>> path_to_root = word_synset.getParentPaths();
	
	
	Node graph = null;
	
	// For all the paths to the parent, we must merge them together into one tree.
	for(List<Synset> this_path_to_root : path_to_root) {
	   
	    Node this_synset_graph = createOntologyGraph(this_path_to_root, null);
	    

	    if(graph == null) {
		graph = this_synset_graph;
	    }else {
		graph = mergeOntologyGraphs(graph, this_synset_graph);
		
	    }


	}


	
	


	return graph;
    }


    /*
    public void createDirectoryStructure(String targetDirectoryPath, Node graph){
	
	if(targetDirectoryPath.equals("")) return;
	
	File directory = new File(targetDirectoryPath);

	// If there is a Node, then create the directory.
	if(graph == null) return;
	
	// Move all the files associated with this Node and move them to the current directory.
	Synset currentSynset = graph.nodeValue();
	


    }
    */

    

    public Node runCastanet(String directory) {
	
	// Get all the files in the directory and extract all the top keywords
	Map keywordsAndFiles = extractKeywordsFromDocument(directory, 10, stopWordsFile);

	List<List> keywords = (List)keywordsAndFiles.get("keywords");
	List<File> files = (List)keywordsAndFiles.get("files");
	
	Node masterGraph = null;
	
	
	for(int fileNo = 0; fileNo < keywords.size(); fileNo++){
	    
	    // Get the keywords that were extracted from the file
	    List<Keyword> topKeywords = keywords.get(fileNo);
	    File currentFile = files.get(fileNo);
	    
	    for(Keyword k : topKeywords) {
		
		String word = k.getWord();
		LOGGER.info("Processing: " + word);

		
		Synset wordSynset = reader.getSynset(word, PartsOfSpeech.NOUN, 1);
		
		// if the synset does not exist then skip it
		if(wordSynset == null) continue;
		
		// For every keyword, create an ontology tree, attach a File object to it.
		
		// Check if there is a FileListAttribute, if yes add to it, if not create one.
		Attribute fileAttribute = wordSynset.getAttribute("files");
		
		if(fileAttribute == null) {
		    List synsetFiles = new ArrayList();
		    synsetFiles.add(currentFile);
		    
		    FileListAttribute fileList = new FileListAttribute(synsetFiles);
		    wordSynset.setAttribute("files", fileList);
		    
		    

		}else{
		    
		    FileListAttribute fileList = (FileListAttribute) fileAttribute;

		    // Add the file to the list
		    fileList.object().add(currentFile);

		}

		
		Node wordGraph = getOntologyGraph(word, PartsOfSpeech.NOUN, 1);
		
		

		if(masterGraph == null) masterGraph = wordGraph;
		else {
		    masterGraph = mergeOntologyGraphs(masterGraph, wordGraph);

		}
		

	    }
	    


	}


	// Remove the single parents
	eliminateSingleParents(masterGraph);



	return masterGraph;

	

	
    }


    
    public Node followTheRabbit(Node rootNode, String[] path){
	

	Node currentNode = rootNode;
	
	for (int i = 0; i < path.length; i++) {
	    
	    String nextPath = path[i];
	    
	    // Find the child with the unique id (the name).
	    List children = currentNode.getChildren();
	    Iterator child_iter = children.iterator();
	    

	    
	    boolean foundChild = false;
	    
	    while(child_iter.hasNext()) {
		
		Node childNode = (Node)child_iter.next();
		String childID = childNode.nodeValue().getName();
		
		if(nextPath.equals(childID)) {
		    // Follow the rabbit...
		    currentNode = childNode;
		    foundChild = true;

		    // DEBUG 
		    System.out.println("Going into: "+childID);

		    break;
		}
		
		// DEBUG
		else {
		    System.out.println("Looking for  " + nextPath +", but got "+childID+" =  FAILED!");

		}

	    }
	    

	    if(!foundChild) {
		 throw new IllegalStateException("Could not find node: " + nextPath + " in "+rootNode.nodeValue().getName()+".  Available children are: " + children);
		 
	    }

	}


	return currentNode;
    }
    


    // DEBUG print out the unique ids.
     public void printUniqueID(Node rootNode){
	


	

	    
	// Find the child with the unique id (the name).
	List children = rootNode.getChildren();
	Iterator child_iter = children.iterator();
	
	while(child_iter.hasNext()) {
	    
	    Node childNode = (Node)child_iter.next();
	    
	    System.out.println(childNode.nodeValue().getName());
	    

	    printUniqueID(childNode);
	    break;
	}
	    
	
    }



    public static void main (String[] args) {
		
	Castanet cnet;

	File testFile = new File("test-docs/");
	
	if(args.length == 2){
	    System.out.println("DEBUG: Using STOP FILE = "+args[1]);
	    
	    cnet = new Castanet(args[0], args[1]);
	}
	else
	    cnet = new Castanet(args[0]);


	
	// Get all the files in the directory and extract all the top keywords
		
	try{

	    
	    Node castanetGraph = cnet.runCastanet(testFile.getCanonicalPath());
	    
	    String[] pathwayToHeaven = {};

	    Node test = cnet.followTheRabbit(castanetGraph, pathwayToHeaven);
	    cnet.printGraph(test);
	    
	    /*
	    Map keywordsAndFiles = cnet.extractKeywordsFromDocument(testFile.getCanonicalPath(), 5);
	
	    
	    List<List> keywords = (List)keywordsAndFiles.get("keywords");
	    List<File> files = (List)keywordsAndFiles.get("files");
	    
	    System.out.println(keywords);
	    System.out.println(files);
	    
	    */
	}catch(IOException e){

	}
    
	
	/*
	
	Node pig_graph = cnet.getOntologyGraph("pig", PartsOfSpeech.NOUN, 1);
	Node computer = cnet.getOntologyGraph("computer", PartsOfSpeech.NOUN, 1);
      
	Node cat = cnet.getOntologyGraph("cat", PartsOfSpeech.NOUN, 1);
	
	String[] path = {"physical_entity.NOUN.1","object.NOUN.1","whole.NOUN.2","living_thing.NOUN.1","organism.NOUN.1","animal.NOUN.2","chordate.NOUN.1",
			 "vertebrate.NOUN.1","mammal.NOUN.1","placental.NOUN.1","ungulate.NOUN.1","even-toed_ungulate.NOUN.1"};
	*/
	    
	//cnet.printUniqueID(pig_graph);

	/*


	// DEBUG: test out merging and attributes.
	
	try{
	    File wordnet = new File(args[0]);
	    ArrayList fileList = new ArrayList();
	    fileList.add(wordnet);
	    FileListAttribute test = new FileListAttribute(fileList);
	    
	    
	    System.out.println("FileList = "+ test);
	    
	    pig_graph.nodeValue().setAttribute("file", test);
	    
	}catch(Exception e) {
	    

	}
	
	


	
	Node merged = cnet.mergeOntologyGraphs(pig_graph, computer);
	//	merged = cnet.mergeOntologyGraphs(merged, cat);
	
	merged = cnet.eliminateSingleParents(merged);
	
	cnet.printGraph(merged);

	//	Node resultNode = cnet.followTheRabbit(merged, path);
	// System.out.println(resultNode.nodeValue().getName());


	
	 //System.out.println(merged.nodeValue().getAttribute("file"));
	

	 */

	
	/*

	File testFile = new File("/Users/thuang513/Projects/research/C-Cat/extendOntology/test-docs/");
	
	try{

	    if(testFile.exists()){
		LOGGER.info("Reading all documents inside: "+ testFile);

		Map<String, List> keywordResults = cnet.extractKeywordsFromDocument(testFile.getCanonicalPath(), 10);
		
		List docs = keywordResults.get("files");
		List docKeywords = keywordResults.get("keywords");

		// Print out the top keywords for each document
		for(int i = 0; i < docs.size(); i++) {
		    
		    System.out.println("File : " + docs.get(i) +" | top keywords = "+docKeywords.get(i));

		    
		}

	    }else{
		throw new IOException();

	    }
	}catch(IOException ioe){
	    
	    System.err.println("Could not find test file!!!");

	}

	*/
    }

}

