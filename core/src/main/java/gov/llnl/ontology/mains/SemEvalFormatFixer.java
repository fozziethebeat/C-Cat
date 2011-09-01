package gov.llnl.ontology.mains;

/*
import edu.ucla.sspace.text.CorpusReader;
import edu.ucla.sspace.text.corpora.SemEvalCorpusReader;

import java.io.File;
import java.io.PrintWriter;


/**
 * This main reformats the SemEval 2010 corpora into the same format as the
 * SenseEval2007 format, which is much simpler and easier to work with. 
 *
 * @author Keith Stevens
public class SemEvalFormatFixer {
    public static void main(String[] args) throws Exception {
        File baseDir = new File(args[0]);
        for (int i = 1; i < args.length; ++i) {
            File input = new File(args[i]);
            PrintWriter writer = new PrintWriter(new File(
                        baseDir, input.getName()));

            writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            writer.println(" <corpus lang=\"en\">");
            writer.printf(" <lexelt item=\"$s\">\n",
                          input.getName().replace(".xml", ""));
            CorpusReader reader = new SemEvalCorpusReader();
            reader.initialize(args[i]);
            while (reader.hasNext()) {
                String line = reader.next().reader().readLine();
                String[] headerLine = line.split("\\s+", 2);
                headerLine[1] = headerLine[1].replaceFirst(
                        "\\|\\|\\|\\| (\\w+) ", "<head> $1 </head> ");
                headerLine[1] = headerLine[1].replace("|||| ", "");
                writer.printf("<instance id=\"%s\" corpus=\"unk\">", headerLine[0]);
                writer.println(headerLine[1]);
                writer.println("</instance>");
            }
            writer.println("</lexelt>");
            writer.println("</corpus>");
            writer.close();
        }
    }
}

*/
