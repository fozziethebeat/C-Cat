package gov.llnl.ontology.wordnet.castanet;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.util.List;
import java.util.*;


import gov.llnl.ontology.wordnet.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class CastanetServlet extends HttpServlet {
    
    private ServletConfig SERVLET_CONFIG = null;
    
    public void init(ServletConfig config) throws ServletException {
	
	super.init(config);

	// Get the wordnet and stop file locations
	ServletContext sc = config.getServletContext();
	String wordnetDirectory = sc.getInitParameter("wordnet");
	String stopfile = sc.getInitParameter("stopfile");

	Castanet cnet = new Castanet(wordnetDirectory, stopfile);
	
        sc.setAttribute("castanet", cnet);

	
	SERVLET_CONFIG = config;
	
    }


    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
	
	ServletContext sc = SERVLET_CONFIG.getServletContext();
	
	
	Castanet cnet = (Castanet) sc.getAttribute("castanet");
	PrintWriter out = response.getWriter();

	String directory = request.getParameter("directory");


	// Get all the files in the directory and extract all the top keywords
	try{
	    
	    File directoryFile = new File(directory);

	    Node castanetGraph = cnet.runCastanet(directoryFile.getCanonicalPath());
	    
	    
	    out.println("Castanet SUCCESSFULLY initialized at directory: " + directory);


	    cnet.printGraph(castanetGraph, out);
	    


	    sc.setAttribute("castanetGraph", castanetGraph);

	}catch(IOException e){

	}
    
	


    }



    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
	
	ServletContext sc = SERVLET_CONFIG.getServletContext();
	
	Castanet cnet = (Castanet) sc.getAttribute("castanet");
	Node castanetGraph = (Node) sc.getAttribute("castanetGraph");

	PrintWriter out = response.getWriter();
	
	
	
	// Get the list of Synset ID's to follow
	String pathway = request.getParameter("pathway");

	if(pathway == null) {
	    out.println("Pathway was invalid!");
	    return;
	}

	String[] path = pathway.split(",");
	String[] empty = {};

	if(path.length == 1 && path[0].equals(""))
	    {
		path = empty;
	    }

	// Follow the nodes 
	Node resultNode = cnet.followTheRabbit(castanetGraph, path);
	
	Attribute files = resultNode.nodeValue().getAttribute("files");
	
	List jsonFiles = null;

	if(files == null) {
	    jsonFiles = new LinkedList();
	    

	    // DEBUG
	    System.out.println("-------- NO FILES for "+ pathway+"!!! -----------");

	}else{
	    FileListAttribute lof = (FileListAttribute)files;
	    jsonFiles = new LinkedList();

	    Iterator iter = lof.object().iterator();
	    while(iter.hasNext()){
		File f = (File)iter.next();
		jsonFiles.add(f.toString());
	    }
	    System.out.println("--------  FILES:  "+ jsonFiles+"!!! -----------");

	    
	}

	Map jsonOutput = new TreeMap(); 
	jsonOutput.put("children", getSynsetNames(resultNode.getChildren()));
	jsonOutput.put("files", jsonFiles);
	
	out.print(new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().toJson(jsonOutput));
    }


    private List<String> getSynsetNames(List input){
	List<String> result = new LinkedList();
	
	Iterator inputIter = input.iterator();
	
	while(inputIter.hasNext()){

	    Node current = (Node)inputIter.next();
	    result.add(current.nodeValue().getName());

	}

	return result;
    }
	
}

