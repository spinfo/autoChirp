package workflowTests;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import autoChirp.DBConnector;
import autoChirp.preProcessing.parser.WikipediaParser;
import autoChirp.tweetCreation.MalformedTSVFileException;
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
		TweetFactory factory = new TweetFactory("src/main/resources/dateTimeFormats/dateTimeFormats.txt");
		List<String> urls = new ArrayList<String>();
		String url = "https://de.wikipedia.org/wiki/Universität_zu_Köln";
		urls.add(url);
		url = "https://en.wikipedia.org/wiki/Harvard_University";
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
	 * test for tsv-imports
	 * @throws MalformedTSVFileException 
	 */
	@Test
	public void generateTweetsFromTSVFile() throws MalformedTSVFileException{
		TweetFactory factory = new TweetFactory("src/main/resources/dateTimeFormats/dateTimeFormats.txt");
		File testFile = new File("src/test/resources/testTSVFile_Image_Locations.txt");
		
		//without delay
		TweetGroup group = factory.getTweetsFromTSVFile(testFile, "testTitle", "testDescription", 0);
		Assert.assertEquals(group.tweets.size(), 3);
		System.out.println("Title: " + group.title);
		System.out.println("Description: " + group.description);
		System.out.println("numberOfTweets: " + group.tweets.size());
		System.out.println("Tweets: ");
		for (Tweet tweet : group.tweets) {
			Assert.assertTrue(tweet.tweetDate.startsWith("2016"));
			System.out.println(tweet.tweetDate+": "+ tweet.content);
		}
		//with delay
		group = factory.getTweetsFromTSVFile(testFile, "testTitle", "testDescription", 3);
		Assert.assertEquals(group.tweets.size(), 3);
		System.out.println("Title: " + group.title);
		System.out.println("Description: " + group.description);
		System.out.println("numberOfTweets: " + group.tweets.size());
		System.out.println("Tweets: ");
		for (Tweet tweet : group.tweets) {
			Assert.assertTrue(tweet.tweetDate.startsWith("2019"));
			System.out.println(tweet.tweetDate+": "+ tweet.content+" "+tweet.imageUrl+" "+tweet.longitude+" "+tweet.latitude);
		}
	}
	
	/**
	 * test all supported dateTime- and dateFormats specified in src/main/resources/dateTmeFormats
	 * @throws IOException 
	 * @throws MalformedTSVFileException 
	 */
	@Test
	public void dateTimeFomatsTest() throws IOException, MalformedTSVFileException{
		File file = new File("src/test/resources/testTSVFile_DateFormats.txt");
		TweetFactory factory = new TweetFactory("src/main/resources/dateTimeFormats/dateTimeFormats.txt");
		TweetGroup group = factory.getTweetsFromTSVFile(file, "dateFormatTest", "test all supported formats", 3);
		Assert.assertEquals(group.tweets.size(), 18);
		for (Tweet tweet : group.tweets) {
			System.out.println(tweet.tweetDate);
		}
	}
}