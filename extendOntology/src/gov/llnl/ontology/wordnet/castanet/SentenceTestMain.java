package gov.llnl.ontology.wordnet.castanet;


import java.io.File;
import java.io.IOException;

import edu.ucla.sspace.text.FileDocument;


public class SentenceTestMain {

    public static void main(String[] args) {



	String PATH = "test-docs/LibyaNews2.txt";
	    
	// Test document location 
	File doc = new File(PATH);



	try{

	    // Process Document
	    FileDocument fileDoc = new FileDocument(doc.getCanonicalPath());

	    
	    SentenceSpace sp = new SentenceSpace();
	    sp.processDocument(fileDoc.reader());
	}catch(IOException ioe) {
	    System.err.println("Error reading the file!");
	    System.exit(-1);
	}

	System.out.println("Test run complete!");
	System.exit(0);

    }


}