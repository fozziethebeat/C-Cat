package gov.llnl.ontology.wordnet.castanet;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;

import java.net.URLEncoder;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Set;
import java.util.TreeSet;


import edu.ucla.sspace.util.Duple;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class AutosummaryServlet extends HttpServlet {
    
    private ServletConfig SERVLET_CONFIG = null;
    
    public void init(ServletConfig config) throws ServletException {
	
	super.init(config);

	// Get the wordnet and stop file locations
	ServletContext sc = config.getServletContext();
	String stopfile = sc.getInitParameter("stopfile");
	
	SERVLET_CONFIG = config;
	
    }

    private String filterString(String s) {
	return s.replaceAll("\\?","");

    }
    


    /** 
     * Takes a comma deliminated string and then converts it into a set.
     * Each token of the string becomes a member of the set.
     */
    private Set<String> stringToSet(String s) {
	
	String[] commaSeperatedStrings = s.split(",");
	Set<String> result = new TreeSet();

	for(int i = 0; i < commaSeperatedStrings.length; i++) {

	    System.out.println("AutosummaryServlet.stringToSet: "+ commaSeperatedStrings[i]);

	    result.add(commaSeperatedStrings[i]);
	}

	return result;
    }
    

    /***
     * Handles the HTTP GET request for the Autosummary Servlet. Expects an input that specifies the file path
     * and returns the automatic summary of this file.
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
	
	ServletContext sc = SERVLET_CONFIG.getServletContext();
	PrintWriter out = response.getWriter();

	String file = request.getParameter("file");
	String selectedKeywords = request.getParameter("selectedKeywords");
	Set<String> selectedKeywordSet = stringToSet(selectedKeywords);
		
	System.out.println(selectedKeywordSet);


	// Get the keyword scores
	Map<String, Double> keywordScores = (Map)sc.getAttribute("keywordScores");

	// Recalculate the keywords, letting the selected keyword scores "float" higher
	Map<String, Double> recalculatedScores = Autosummary.recalculateSelectedScores(keywordScores, selectedKeywordSet, selectedKeywordSet.size());
	
	File summarizeMe = new File(file);
	List<Duple<String, Double>> generatedSummary = Autosummary.autosummarize(summarizeMe, keywordScores);

	String summaryToPrint = "";

	// Print out the sentences
	int printed = 1;
	for( Duple<String, Double> sentenceDuple : generatedSummary ) {
	    
	    if(printed > 3)
		break;
	    
	    String sentence = sentenceDuple.x;
	    summaryToPrint += "<br>" + this.filterString(sentence) + "</br>";
	    printed++;
	    
	}


	
	Map jsonOutput = new TreeMap(); 
	jsonOutput.put("filepath", file);
	jsonOutput.put("summary", summaryToPrint);
	jsonOutput.put("selectedKeywords", selectedKeywordSet);

	out.println(new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().toJson(jsonOutput));

	out.flush();
	out.close();

    }



    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
	// Deal with post.

    }



}

