
package autoChirp;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import autoChirp.preProcessing.parser.Parser;
import autoChirp.preProcessing.parser.WikipediaParser;
import autoChirp.tweetCreation.Tweet;
import autoChirp.tweetCreation.TweetFactory;
import autoChirp.tweetCreation.TweetGroup;

/**
 * @author geduldia
 *
 */
/**
 * @author geduldia
 *
 */
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
	private static String dbCreationFileName = "src/main/resources/database/createDatabaseFile.sql";
	private static Parser wikiParser = new WikipediaParser();
	private static TweetFactory tweetFactory = new TweetFactory();

	
	@BeforeClass
	public static void dbConnection() {
		DBConnector.connect(dbPath + dbFileName);
		DBConnector.createOutputTables(dbCreationFileName);
	}

	/**
	 * @throws SQLException
	 * @throws IOException
	 */
	@Test
	public void buildTweetsFromUrlTest() throws SQLException, IOException {
		int userID = DBConnector.insertNewUser(123, "token", "secret");
		TweetGroup group = tweetFactory.getTweetsFromUrl("https://de.wikipedia.org/wiki/Star_Wars", wikiParser);
		int sizeBefore = group.tweets.size();
		Assert.assertTrue(sizeBefore > 0);
		int groupID = DBConnector.insertTweetGroup(group, userID);
		group = DBConnector.getTweetGroupForUser(userID, groupID);
		Assert.assertEquals(sizeBefore, group.tweets.size());
		for (Tweet tweet : group.tweets) {
			System.out.println("TweetDate: " + tweet.tweetDate);
			System.out.println(tweet.content);
			System.out.println();
		}
	}
	
	
	@Test
	public void buildTweetsFromTableTest(){
		int userID = DBConnector.insertNewUser(111, "token", "secret");
		File csvFile = new File("src/test/resources/testFiles/testCSV.csv");
		TweetGroup group = tweetFactory.getTweetsFromCSV(csvFile, "csv-test", "description");
		int groupID = DBConnector.insertTweetGroup(group, userID);
		group = DBConnector.getTweetGroupForUser(userID, groupID);
		for (Tweet tweet : group.tweets) {
			System.out.println("date: " + tweet.tweetDate);
			System.out.println(tweet.content);
		}
	}
}
