package autoChirp.preProcessing.parser;

import autoChirp.preProcessing.Document;

/**
 * @author Alena Geduldig
 * 
 *         A simple interface for a parser (e.g. WikipediaParser) Ensures that
 *         each parser has to return a Document-object with text, title, url and
 *         language.
 *
 */

public interface Parser {

	/**
	 * creates a Document from an url
	 * 
	 * @param url
	 * @return an object of class Document
	 */
	public Document parse(String url);

}
