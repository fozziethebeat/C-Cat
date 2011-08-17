package gov.llnl.ontology.text.hbase;

import gov.llnl.ontology.text.Document;

import gov.llnl.ontology.mapreduce.table.SchemaUtil;

import com.google.gson.reflect.TypeToken;

import org.apache.hadoop.hbase.client.Result;

import java.util.Set;


/**
 * @author Keith Stevens
 */
public class DynamicDocument implements Document {
    
    private final Result row;

    private final String corpusNameColumn;
    private final String rawTextColumn;
    private final String originalTextColumn;
    private final String keyColumn;
    private final String idColumn;
    private final String titleColumn;
    private final String categoriesColumn;

    private String corpusName;
    private String rawText;
    private String originalText;
    private String key;
    private long id;
    private String title;
    private Set<String> categories;

    public DynamicDocument(Result row, String corpusNameColumn, String rawTextColumn, 
                           String originalTextColumn, String keyColumn, String idColumn, 
                           String titleColumn, String categoriesColumn) {
        this.row = row;
        this.corpusNameColumn = corpusNameColumn;
        this.rawTextColumn = rawTextColumn;
        this.originalTextColumn = originalTextColumn;
        this.keyColumn = keyColumn;
        this.idColumn = idColumn;
        this.titleColumn = titleColumn;
        this.categoriesColumn = categoriesColumn;

        corpusName = null;
        rawText = null;
        originalText = null;
        key = null;
        id = -1;
        title = null;
        categories = null;
    }

    /**
     * {@inheritDoc}
     */
    public String sourceCorpus() {
        if (corpusName == null)
            corpusName = SchemaUtil.getColumn(row, corpusNameColumn);
        return corpusName;
    }

    /**
     * {@inheritDoc}
     */
    public String rawText() {
        if (rawText == null)
            rawText = SchemaUtil.getColumn(row, rawTextColumn);
        return rawText;
    }

    /**
     * {@inheritDoc}
     */
    public String originalText() {
        if (originalText == null)
            originalText = SchemaUtil.getColumn(row, originalTextColumn);
        return originalText;
    }

    /**
     * {@inheritDoc}
     */
    public String key() {
        if (key == null)
            key = SchemaUtil.getColumn(row, keyColumn);
        return key;
    }

    /**
     * {@inheritDoc}
     */
    public long id() {
        if (id < 0)
            id = Long.parseLong(SchemaUtil.getColumn(row, idColumn));
        return id;
    }

    /**
     * {@inheritDoc}
     */
    public String title() {
        if (title == null)
            title = SchemaUtil.getColumn(row, titleColumn); 
        return title;
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> categories() {
        if (categories == null)
            categories = SchemaUtil.getObjectColumn(
                    row, categoriesColumn, new TypeToken<Set<String>>(){}.getType());
        return categories;
    }
}
