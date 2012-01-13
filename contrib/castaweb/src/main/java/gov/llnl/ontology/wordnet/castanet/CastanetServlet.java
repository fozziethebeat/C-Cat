package gov.llnl.ontology.wordnet.castanet;

import gov.llnl.ontology.text.CorpusReader;
import gov.llnl.ontology.text.Document;
import gov.llnl.ontology.text.NewLineSplitCorpusReader;
import gov.llnl.ontology.text.corpora.TasaDocumentReader;
import gov.llnl.ontology.wordnet.Attribute;
import gov.llnl.ontology.wordnet.OntologyReader;
import gov.llnl.ontology.wordnet.WordNetCorpusReader;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.ucla.sspace.matrix.TfIdfTransform;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class CastanetServlet extends HttpServlet {
    
    private ServletConfig SERVLET_CONFIG = null;
    
    private Gson gson;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

        // Get the wordnet and stop file locations
        ServletContext sc = config.getServletContext();
        String wordnetDir = sc.getInitParameter("wordnet");
        OntologyReader wordnet = WordNetCorpusReader.initialize(wordnetDir);
        KeywordExtractor extractor = new TransformKeywordExtractor(
                new TfIdfTransform());
        Castanet cnet = new Castanet(wordnet, extractor);
        sc.setAttribute("castanet", cnet);
        SERVLET_CONFIG = config;
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ServletContext sc = SERVLET_CONFIG.getServletContext();
        Castanet cnet = (Castanet) sc.getAttribute("castanet");
        PrintWriter out = response.getWriter();

        String directory = request.getParameter("directory");
        CorpusReader corpReader = new NewLineSplitCorpusReader(
                directory, new TasaDocumentReader());
        String stopFile = sc.getInitParameter("stopfile");
        Node castanetGraph = cnet.runCastanet(corpReader, stopFile);

        Set<String> keywords = Sets.newHashSet();
        extractKeyWords(castanetGraph, keywords);
        sc.setAttribute("castanetGraph", castanetGraph);
        sc.setAttribute("keywords", keywords);

        out.println("Castanet SUCCESSFULLY initialized at directory: " + directory);
        cnet.printGraph(castanetGraph, out);
    }

    private static void extractKeyWords(Node root, Set<String> keywords) {
        keywords.add(root.getValue().getName());
        for (Node child : root.getChildren())
            keywords.add(child.getValue().getName());
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ServletContext sc = SERVLET_CONFIG.getServletContext();
        PrintWriter out = response.getWriter();

        // Get the list of Synset ID's to follow
        String pathway = request.getParameter("pathway");
        if(pathway == null) {
            out.println("Pathway was invalid!");
            return;
        }

        // Extract the object data.
        Castanet cnet = (Castanet) sc.getAttribute("castanet");
        Set<String> keywords = (Set<String>) sc.getAttribute("keywords");
        Node castanetGraph = (Node) sc.getAttribute("castanetGraph");

        // Follow the path and determine the current level in the corpus
        // hierarchy.
        String[] path = pathway.trim().split(",");
        Node resultNode = cnet.followTheRabbit(castanetGraph, path);

        // Extract all the available documents classified at this level in the
        // hierarchy.
        Attribute<List<Document>> files = (Attribute<List<Document>>) resultNode.getValue().getAttribute("files");
        List<String> jsonFiles = Lists.newArrayList();
        for (Document doc : files.object())
            jsonFiles.add(doc.key());

        // Extract the set of keywords for refining the documents at this level.
        Set<String> currentKeyWords = Sets.newHashSet();
        for (Node child : resultNode.getChildren())
            currentKeyWords.add(child.getValue().getName());

        // Store the data in a json map.
        Map<String, Object> jsonOutput = Maps.newHashMap();
        jsonOutput.put("children", currentKeyWords);
        jsonOutput.put("files", jsonFiles);
        jsonOutput.put("keywords", keywords);
        out.print(gson.toJson(jsonOutput));
    }
}
