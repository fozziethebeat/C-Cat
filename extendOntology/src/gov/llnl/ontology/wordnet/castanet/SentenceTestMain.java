package gov.llnl.ontology.wordnet.castanet;


import java.io.File;
import java.io.IOException;

import edu.ucla.sspace.text.FileDocument;


public class SentenceTestMain {

    public static void main(String[] args) {



	String PATH = "test-docs";
	    
	String PATH2 = "test-docs/JapanNuclear.txt";
	String PATH3 = "test-docs/TaxGas.txt";
	// Test document location 
	File doc = new File(PATH3);
	File doc2 = new File(PATH2);


	try{

	    // //Process Document
	    // FileDocument fileDoc = new FileDocument(doc.getCanonicalPath());
	    // FileDocument fileDoc2 = new FileDocument(doc2.getCanonicalPath());
	    
	    // SentenceSpace sp = new SentenceSpace();
	    // sp.processDocument(fileDoc.reader());
	    // sp.processDocument(fileDoc2.reader());

	    // sp.setupMatrix();
	    
	    // sp.calculateSentenceScore();
	    
	    
	    
	    // for(String term: sp.getWords()) {
	    // 	System.out.print("Word = "+ term);

	    // 	for(int i = 0; i < sp.getVector(term).length(); i++) {
	    // 	    System.out.print("\t" + sp.getVector(term).getValue(i).doubleValue());
		    
	    // 	}
		
	    // 	System.out.println();
		
	    // }
	    

	  Autosummary.calculateSakaiEtAlScore(PATH);


	}catch(IOException ioe) {
	    System.err.println("Error reading the file!");
	    System.exit(-1);
	}

	System.out.println("Test run complete!");
	System.exit(0);

    }


}