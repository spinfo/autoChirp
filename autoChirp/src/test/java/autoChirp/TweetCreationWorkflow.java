
package autoChirp;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import autoChirp.tweetCreation.Document;
import autoChirp.tweetCreation.SentenceSplitter;
import autoChirp.tweetCreation.Tweet;
import autoChirp.tweetCreation.TweetFactory;
import autoChirp.tweetCreation.Parser.WikipediaParser;

public class TweetCreationWorkflow {
	
	/**
	 * @author Alena
	 * 
	 * junit test-class for the data-mining workflow
	 * - insert and read urls to parse
	 * - parse urls and extract dates and tweets
	 * - store tweets and tweet-date
	 */

	private static String dbPath = "src/test/resources/";
	private static String dbFileName = "autoChirp.db";
	private static String dbCreationFileName = "src/test/resources/createDatabaseFile.sql";
	private static WikipediaParser parser = new WikipediaParser();
	private static TweetFactory tweetFactory = new TweetFactory();

	@BeforeClass
	public static void dbConnection() {
		DBConnector.connect(dbPath + dbFileName);
		DBConnector.createOutputTables(dbCreationFileName);
	}

	@Test
	public void dataMiningTest() throws SQLException, IOException {

		// DBConnector.insertURL("https://de.wikipedia.org/wiki/Zweiter_Weltkrieg",
		// 5);
		DBConnector.isertUrl("https://en.wikipedia.org/wiki/History_of_New_York", 5);
		// DBConnector.insertURL("https://en.wikipedia.org/wiki/Woody_Allen",
		// 2);
		Map<String, List<Integer>> urlsAndUserIDs = DBConnector.getUrls();
		if (urlsAndUserIDs.isEmpty())
			return;
		for (String url : urlsAndUserIDs.keySet()) {
			Document doc = parser.parse(url);
			SentenceSplitter st = new SentenceSplitter(doc.getLanguage());
			doc.setSentences(st.splitIntoSentences(doc.getText(), doc.getLanguage()));
			List<Tweet> tweets = tweetFactory.getTweets(doc);
			DBConnector.insertTweets(url, tweets, urlsAndUserIDs.get(url), doc.getTitle());

			System.out.println("Title: " + doc.getTitle());
			System.out.println("URL: " + doc.getUrl());
			System.out.println("Language: " + doc.getLanguage());
			System.out.println();
			for (Tweet tweet : tweets) {
				System.out.print(tweet.getTweetDate()+": ");
				System.out.println(tweet.getContent());
			}
			System.out.println();
		}
	}
}