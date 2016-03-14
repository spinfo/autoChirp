package workflowTests;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import autoChirp.DBConnector;
import autoChirp.preProcessing.parser.WikipediaParser;
import autoChirp.tweetCreation.Tweet;
import autoChirp.tweetCreation.TweetFactory;
import autoChirp.tweetCreation.TweetGroup;



/**
 * @author Alena Geduldig
 * 
 * JUnit test-class for the Data-Minint- and TweetCreationWorkflow
 *
 */
public class TweetCreationWorkflow {
	
	private static String dbPath = "src/test/resources/";
	private static String dbFileName = "autoChirp.db";
	private static String dbCreationFileName = "src/main/resources/database/createDatabaseFile.sql";
	

	/**
	 * connect to database and  create output-tables database
	 */
	@BeforeClass
	public static void dbConnection() {
		DBConnector.connect(dbPath + dbFileName);
		DBConnector.createOutputTables(dbCreationFileName);
	}


	
	/**
	 * test for the tweet creation from wiki-urls
	 */
	@Test
	public void generateTweetsFromWikipediaUrl(){
		TweetFactory factory = new TweetFactory();
		List<String> urls = new ArrayList<String>();
		String url = "https://de.wikipedia.org/wiki/Star_Wars";
		urls.add(url);
		url = "https://en.wikipedia.org/wiki/Woody_Allen";
		urls.add(url);
		for (String currentUrl : urls) {
			TweetGroup group = factory.getTweetsFromUrl(currentUrl, new WikipediaParser(),"testDescription", "prefix");
			System.out.println("Title: " + group.title);
			System.out.println("Description: " + group.description);
			System.out.println("numberOfTweets: " + group.tweets.size());
			System.out.println("Tweets: ");
			for (Tweet tweet : group.tweets) {
				Assert.assertTrue(tweet.content.startsWith("prefix: "));
				System.out.println(tweet.tweetDate+": "+ tweet.content);
			}
		}
	}
	
	/**
	 * test for csv-imports
	 */
	@Test
	public void generateTweetsFromCSVFile(){
		TweetFactory factory = new TweetFactory();
		File testFile = new File("src/test/resources/testCSVFile.csv");
		
		//without delay
		TweetGroup group = factory.getTweetsFromExcelFile(testFile, "testTitle", "testDescription", 0);
		Assert.assertEquals(group.tweets.size(), 5);
		System.out.println("Title: " + group.title);
		System.out.println("Description: " + group.description);
		System.out.println("numberOfTweets: " + group.tweets.size());
		System.out.println("Tweets: ");
		for (Tweet tweet : group.tweets) {
			Assert.assertTrue(tweet.tweetDate.startsWith("2016"));
			System.out.println(tweet.tweetDate+": "+ tweet.content);
		}
		//with delay
		group = factory.getTweetsFromExcelFile(testFile, "testTitle", "testDescription", 3);
		Assert.assertEquals(group.tweets.size(), 5);
		System.out.println("Title: " + group.title);
		System.out.println("Description: " + group.description);
		System.out.println("numberOfTweets: " + group.tweets.size());
		System.out.println("Tweets: ");
		for (Tweet tweet : group.tweets) {
			Assert.assertTrue(tweet.tweetDate.startsWith("2019"));
			System.out.println(tweet.tweetDate+": "+ tweet.content);
		}
	}
}