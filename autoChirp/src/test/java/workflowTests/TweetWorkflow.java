//
package workflowTests;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import autoChirp.DBConnector;
import autoChirp.tweetCreation.Tweet;
import autoChirp.tweetCreation.TweetGroup;
import autoChirp.tweeting.TweetScheduler;


/**
 * @author Alena Geduldig
 * 
 *         A test class for scheduling and twitter tweets - requires the file
 *         src/test/resources/twitter_secrets.txt with a valid TwitterHandle, oAuthToken and oAuthTokenSecret
 *
 *         Twitter-Handle: ...
 *         Access Token: ...
 *         Access Token Secret: ...

 *
 */
public class TweetWorkflow {

	private static String dbPath = "src/test/resources/";
	private static String dbFileName = "autoChirp.db";
	private static String dbCreationFileName = "src/main/resources/database/createDatabaseFile.sql";

	private static String[] twitterSecrets;

	/**
	 * connect to database and create output-tables database
	 */
	@BeforeClass
	public static void dbConnection() {
		DBConnector.connect(dbPath + dbFileName);
		DBConnector.createOutputTables(dbCreationFileName);
	}

	@BeforeClass
	public static void readTwitterConfigFromFile() {
		twitterSecrets = new String[5];
		BufferedReader in;
		try {
			in = new BufferedReader(new FileReader("src/test/resources/twitter_secrets.txt"));
			String line = in.readLine();
			int i = 0;
			while (line != null) {
				twitterSecrets[i] = line.split(":")[1].trim();
				i++;
				line = in.readLine();
			}
			in.close();
		} catch (IOException e) {
			System.out.println("twitter_secretes.txt is missing");
			System.exit(0);
		}
	}

	/**
	 * schedule a TweetGroup
	 */
	@Test
	public  void scheduleTweetsForUser() {
		// insert new user
		int userID = DBConnector.insertNewUser(12, twitterSecrets[1], twitterSecrets[2]);
		
		// generate test-tweets
		LocalDateTime ldt = LocalDateTime.now();
		ldt = ldt.plusSeconds(65);
		String tweetDate = ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		List<Tweet> tweets = new ArrayList<Tweet>();
		tweets.add(new Tweet(tweetDate, "das ist ein test #autoChirp"));
		tweets.add(new Tweet(tweetDate, "das ist auch ein test #autoChirp"));
		TweetGroup group = new TweetGroup("test_title", "description");
		group.setTweets(tweets);

		// insert test-tweets
		int groupID = DBConnector.insertTweetGroup(group, userID);

		// update group-status to enabled = true
		DBConnector.updateGroupStatus(groupID, true, userID);
		TweetGroup read = DBConnector.getTweetGroupForUser(userID, groupID);

		// schedule tweets
		TweetScheduler.scheduleTweetsForUser(read.tweets, userID);

		// program has to run until all tweets are tweeted
		LocalDateTime date = LocalDateTime.parse(tweetDate, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		date = date.plusSeconds(10);
		boolean stop = false;
		while (!stop) {
			LocalDateTime now = LocalDateTime.now();
			if (now.isAfter(date)) {
				stop = true;
			}
		}
	}
}

