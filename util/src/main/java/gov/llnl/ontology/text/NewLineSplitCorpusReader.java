package gov.llnl.ontology.text;

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;
import java.io.FileReader;


/**
 * @author Keith Stevens
 */
public class NewLineSplitCorpusReader implements CorpusReader {

    private final DocumentReader reader;

    private final String corpusPath;

    private BufferedReader br;

    private Document currDoc;

    public NewLineSplitCorpusReader(String corpusPath, DocumentReader reader) {
        this.reader = reader;
        this.corpusPath = corpusPath;
    }

    public void initialize() {
        try {
            br = new BufferedReader(new FileReader(corpusPath));
            currDoc = advance();
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    public boolean hasNext() {
        return currDoc != null;
    }

    public Document next() {
        Document doc = currDoc;
        try {
            currDoc = advance();
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
        return doc;
    }

    public void remove() {
        throw new UnsupportedOperationException(
                "Cannot remove documents from a corpus reader");
    }

    private Document advance() throws IOException {
        String line = null;
        while ((line = br.readLine()) != null && line.length() == 0)
            ;

        // If there were no lines left return null.
        if (line == null)
            return null;

        // Keep reading until a blank line was seen or the reader has no
        // further lines
        StringBuilder sb = new StringBuilder();
        sb.append(line).append("\n");
        while ((line = br.readLine()) != null && line.length() != 0)
            sb.append(line).append("\n");
        return reader.readDocument(sb.toString());
    }
}
