package autoChirp.preProcessing.parser;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import autoChirp.preProcessing.Document;
import de.unihd.dbs.uima.annotator.heideltime.resources.Language;

/**
 * A Parser for Wikipedia-Urls using the org.cyberneko.html.parsers.DOMParser;
 * Creates an object of class Document, which consists of text, title, url and
 * language. This Parser appends the content of each p-element to the documents
 * text and selects the first h1-element as the documents title. The documents
 * language is read directly from the Wikipedia-Url (e.g.
 * https://de.wikipedia.org/wiki/Köln)
 * 
 * @author Alena Geduldig
 *
 */
public class WikipediaParser implements Parser {

	private StringBuilder builder;
	private boolean hasTitle;
	private String title;
	private DOMParser domParser = new DOMParser();
	// regex for footnotes in wikipedia
	private String regex = "((\\[[0-9]+\\])+(:[0-9]+)?)";

	/**
	 * extracts the plain text (p-elements) and title (first h1-element) of the
	 * given wikipedia-url and returns an object of class Document
	 * 
	 * @param url
	 *            the url to parse
	 */
	@Override
	public Document parse(String url) {
		try {
			domParser.setProperty("http://cyberneko.org/html/properties/default-encoding", "UTF-8");
			domParser.parse(url);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		builder = new StringBuilder();
		hasTitle = false;
		process(domParser.getDocument().getFirstChild());

		return new Document(builder.toString().trim(), url, title, getLanguage(url));
	}

	/**
	 * appends the content of each p-element to the documents text and selects
	 * the first h1-element as title
	 * 
	 * @param node
	 */
	private void process(Node node) {
		String elementName = node.getNodeName().toLowerCase().trim();
		// takes the content of the first h1-element as title
		if (hasTitle == false && elementName.equals("h1")) {
			title = node.getTextContent().trim();
			hasTitle = true;
		}
		// takes the content of each p-element as text
		if (elementName.equals("p")) {
			String elementContent = node.getTextContent().trim();
			// remove footnotes
			Pattern pattern = Pattern.compile(regex);
			Matcher matcher = pattern.matcher(elementContent);
			while (matcher.find()) {
				elementContent = elementContent.replace(matcher.group(1), "");
			}
			if (elementContent.length() > 0) {
				builder.append(elementContent).append("\n\n");
			}
		}
		Node sibling = node.getNextSibling();
		if (sibling != null) {
			process(sibling);
		}
		Node child = node.getFirstChild();
		if (child != null) {
			process(child);
		}
		if (hasTitle == false) {
			title = "ohne Titel";
		}
	}

	/**
	 * returns the language of a wikipedia-article taken from it's url (e.g.
	 * https://[de].wikipedia.org/wiki/köln") supports English and German
	 * 
	 * @param url
	 *            url
	 * @return language
	 * 
	 */
	private Language getLanguage(String url) {
		String lang = url.split("://")[1].substring(0, 2);
		if (lang.equals("en")) {
			return Language.ENGLISH;
		}
		if (lang.equals("de")) {
			return Language.GERMAN;
		}
		System.out.println("WARNING: unknown Language!  ");
		return Language.ENGLISH;
	}
}
