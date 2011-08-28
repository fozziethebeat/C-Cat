package gov.llnl.ontology.text.corpora;

import gov.llnl.ontology.text.corpora.Article;
/**
 *  An interface for an Article factory following the factory pattern. All factories that 
 *  create articles should implement this interface.
 *  @author Terry Huang (thuang513@gmail.com)
 */
public interface ArticleFactory {

	/**
	 * The factory method for creating articles.
	 */
	public Article createArticle(String articleUrl);
}