package gov.llnl.ontology.mains;

import gov.llnl.ontology.text.parse.Parser;
import gov.llnl.ontology.text.parse.MaltLinearParser;

import gov.llnl.text.util.FileUtils;

import java.io.PrintWriter;


/**
 * @author Keith Stevens
 */
public class ParseUkWac {

    public static void main(String[] args) throws Exception {
        Parser parser = new MaltLinearParser();
        PrintWriter writer = new PrintWriter(args[0]);
        StringBuilder sentence = new StringBuilder();
        for (int i = 1; i < args.length; ++i) {
            for (String line : FileUtils.iterateFileLines(args[i])) {
                if (line.startsWith("<text") ||
                    line.startsWith("<s") ||
                    line.startsWith("</text") ||
                    line.length() == 0 )
                    writer.println(line);
                else if (line.startsWith("</s")) {
                    writer.println(parser.parseText("", sentence.toString()));
                    sentence = new StringBuilder();
                } else
                    sentence.append(line.split("\\s+")[0]).append(" ");
            }
        }
        writer.close();
    }
}

