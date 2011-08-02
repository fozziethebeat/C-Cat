package gov.llnl.ontology.text.corpora;

import gov.llnl.ontology.text.DocumentReader;
import gov.llnl.ontology.text.SimpleDocument;

import org.apache.commons.lang.StringEscapeUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.HashSet;


/**
 * @author Keith Stevens
 */
public class UkWacDocumentReader implements DocumentReader {

    public static final String CORPUS_NAME = "ukwac";

    public String corpusName() {
        return CORPUS_NAME;
    }

    public gov.llnl.ontology.text.Document readDocument(String doc) {
        String[] lines = doc.split("\\n");

        // Find the title.
        int titleStart = lines[0].indexOf("id=\"")+4;
        int titleEnd = lines[0].lastIndexOf("\">");
        String key = lines[0].substring(titleStart, titleEnd);
        long id = key.hashCode();
        
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i < lines.length-1; ++i) {
            // Skip empty lines and xml tags.
            if (lines[i].length() == 0 ||
                lines[i].endsWith("s>"))
                continue;

            lines[i] = StringEscapeUtils.unescapeHtml(lines[i]);
            String[] toks = lines[i].split("\\s+");
            builder.append(toks[0]).append(" ");
        }

        return new SimpleDocument(corpusName(), builder.toString(), doc, 
                                  key, id, key, new HashSet<String>());
    }
}
