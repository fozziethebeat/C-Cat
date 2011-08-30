package gov.llnl.ontology.text.corpora;

import gov.llnl.ontology.text.Document;

import java.io.File;
import java.io.IOException;

public interface Article extends Document {
	
	/* 
	 * Get the URL of the article.
	 */
	public String getArticleUrl();
	
	/* 
	 * Fetch the contents of the article and parse the article.
	 */
	public boolean fetchAndParseContent();

	/*
	 *  Return the date of the article.
	 */
	//	public Date getDate();

	/*
	 * Write the contents of the article to a file.
	 */
	void writeToFile(File outFile) throws IOException;
	
	public String getDescription();

	public String getTitle();

	void setArticleUrl(String articleUrl);
		
	void setDescription(String content);

	void setTitle(String articleTitle);

}