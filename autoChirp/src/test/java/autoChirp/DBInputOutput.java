package autoChirp;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import autoChirp.tweetCreation.Tweet;
import autoChirp.tweetCreation.TweetGroup;

/**
 * 
 * @author Alena Geduldig
 * 
 * Junit test-class for the DBConnector
 *
 */

public class DBInputOutput {

	private static String dbPath = "src/test/resources/";
	private static String dbFileName = "autoChirp.db";
	private static String dbCreationFileName = "src/main/resources/database/createDatabaseFile.sql";
	
	private static List<Tweet> testTweets;
	private static TweetGroup testGroup;

	/**
	 * connect to database and  create output-tables database
	 */
	@BeforeClass
	public static void dbConnection() {
		DBConnector.connect(dbPath + dbFileName);
		DBConnector.createOutputTables(dbCreationFileName);
	}

	/**
	 * create test data
	 */
	@BeforeClass
	public static void createTestData(){
		testTweets = new ArrayList<Tweet>();
		testGroup = new TweetGroup("testTitle", "testDescription");
		Tweet tweet = new Tweet("1478-12-12 00:12", "testTweet1");
		testTweets.add(tweet);
		tweet = new Tweet("1999-10-08 12:00", "testTweet2");
		testTweets.add(tweet);
		tweet = new Tweet("1999-10-08 13:58", "testTweet3");
		testTweets.add(tweet);
		tweet = new Tweet("2017-11-01 13:00", "testTweet4");
		testTweets.add(tweet);
		testGroup.setTweets(testTweets);
	}
	

	/**
	 * insert and read tweetGroup
	 * 
	 */
	@Test
	public void insertAndGetTweetsTest(){
		int userID = 1;
		int groupID = DBConnector.insertTweetGroup(testGroup, userID);
		TweetGroup read = DBConnector.getTweetGroupForUser(userID, groupID);
		Assert.assertTrue(read.tweets.size() == testGroup.tweets.size());
		for (int i = 0; i < read.tweets.size();i++) {
			Assert.assertTrue(read.tweets.get(i).content.equals(testTweets.get(i).content));
		}
		Assert.assertEquals("testTweet4", read.tweets.get(3).content);
		Assert.assertEquals("1478-12-12 00:12", read.tweets.get(0).tweetDate);
		Assert.assertEquals("testTitle", read.title);
		Assert.assertEquals("testDescription", read.description);
	}
	
	/**
	 * check if a user exists and insert a new User
	 */
	@Test
	public void insertNewUser(){
		long twitterID = 123;
		String oAuthToken = "testToken";
		String tokenSecret = "testSecret";
		int userID = DBConnector.insertNewUser(twitterID, oAuthToken, tokenSecret);
		int localID = DBConnector.checkForUser(twitterID);
		Assert.assertEquals(userID, localID);
		localID = DBConnector.checkForUser(222);
		Assert.assertEquals(localID, -1);
	}
	/**
	 * read userConfig from DB
	 */
	@Test
	public void readUserConfig(){
		long twitterID = 123;
		String oAuthToken = "testToken";
		String tokenSecret = "testSecret";
		int userID = DBConnector.insertNewUser(twitterID, oAuthToken, tokenSecret);
		String[] config = DBConnector.getUserConfig(userID);
		Assert.assertEquals(twitterID, Long.parseLong(config[0]));
		Assert.assertEquals(oAuthToken, config[1]);
		Assert.assertEquals(tokenSecret, config[2]);
	}
	
	/**
	 * update and statuses and get tweets/groupTweets by status
	 */
	@Test
	public void updateTweetOrTweetGroupStatus(){
		int userID = 12;
		int groupID = DBConnector.insertTweetGroup(testGroup, userID);
		TweetGroup read = DBConnector.getTweetGroupForUser(userID, groupID);
		Assert.assertFalse(DBConnector.isEnabledGroup(groupID, userID));
		DBConnector.updateGroupStatus(groupID, true, userID);
		read = DBConnector.getTweetGroupForUser(userID, groupID);
		Assert.assertTrue(DBConnector.isEnabledGroup(groupID, userID));
		DBConnector.updateGroupStatus(groupID, false, userID);
		read = DBConnector.getTweetGroupForUser(userID, groupID);
		Assert.assertFalse(read.enabled);
		for (Tweet tweet : read.tweets) {
			Assert.assertFalse(tweet.scheduled);
			Assert.assertFalse(tweet.tweeted);
			int tweetID = tweet.tweetID;
			DBConnector.flagAsScheduled(tweetID, userID);
			DBConnector.flagAsTweeted(tweetID, userID);
			tweet = DBConnector.getTweetByID(tweetID, userID);
			Assert.assertTrue(tweet.scheduled);
			Assert.assertTrue(tweet.tweeted);
		}
		Map<Integer,List<TweetGroup>> enabledGroups = DBConnector.getAllEnabledGroups();
		Assert.assertEquals(enabledGroups.size(), 0);
		DBConnector.updateGroupStatus(groupID, true, userID);
		enabledGroups = DBConnector.getAllEnabledGroups();
		Assert.assertEquals(enabledGroups.size(), 1);
		Assert.assertEquals(enabledGroups.keySet().iterator().next().intValue(), userID);
		Assert.assertEquals(1, enabledGroups.get(userID).size());
		
	}
	
	/**
	 * delete Account and check if all user-related data is deleted
	 */
	@Test
	public void deleteteAccount(){
		int userID = DBConnector.insertNewUser(15, null, null);
		int groupID = DBConnector.insertTweetGroup(testGroup, userID);
		DBConnector.deleteUser(userID);
		String[] config = DBConnector.getUserConfig(userID);
		Assert.assertTrue(config == null);
		TweetGroup read = DBConnector.getTweetGroupForUser(userID, groupID);
		Assert.assertTrue(read == null);
		List<Tweet> tweets = DBConnector.getTweetsForUser(userID);
		Assert.assertTrue(tweets.size() == 0);	
	}
	
	/**
	 * test Method getGroupIdsForUser()
	 */
	@Test
	public void getGroupIDsForUser(){
		int userID = DBConnector.insertNewUser(44, null, null);
		int groupID1 = DBConnector.insertTweetGroup(testGroup, userID);
		int groupID2 = DBConnector.insertTweetGroup(testGroup, userID);
		List<Integer> groupIDs = DBConnector.getGroupIDsForUser(userID);
		Assert.assertEquals(groupID1, groupIDs.get(0).intValue());
		Assert.assertEquals(groupID2, groupIDs.get(1).intValue());
	}
	
	/**
	 * test all different getTweet-Methods
	 */
	@Test
	public void getTweets(){
		int userID = DBConnector.insertNewUser(44, null, null);
		int groupID = DBConnector.insertTweetGroup(testGroup, userID);
		DBConnector.updateGroupStatus(groupID, true, userID);
		TweetGroup read = DBConnector.getTweetGroupForUser(userID, groupID);
		DBConnector.flagAsScheduled(read.tweets.get(0).tweetID, userID);
		DBConnector.flagAsTweeted(read.tweets.get(0).tweetID, userID);
		List<Tweet> tweets = DBConnector.getTweetsForUser(userID, true, true, groupID);
		Assert.assertEquals(tweets.get(0).content, testTweets.get(0).content);
		Assert.assertEquals(1, tweets.size());
		tweets = DBConnector.getTweetsForUser(userID, false, false);
		Assert.assertEquals(3, tweets.size());
	}
	
	/**
	 * test editing Tweets and TweetGroups
	 */
	@Test
	public void editTweetAndTweetGroup(){
		int userID = DBConnector.insertNewUser(44, null, null);
		int groupID = DBConnector.insertTweetGroup(testGroup, userID);
		DBConnector.editGroup(groupID, "newTitle", "newDescription", userID);
		TweetGroup read = DBConnector.getTweetGroupForUser(userID, groupID);
		String title = DBConnector.getGroupTitle(groupID, userID);
		Assert.assertEquals("newTitle", title);
		Assert.assertEquals("newDescription", read.description);
		DBConnector.editTweet(read.tweets.get(3).tweetID, "newContent", userID);
		read = DBConnector.getTweetGroupForUser(userID, groupID);
		Assert.assertEquals("newContent", read.tweets.get(3).content);
	}
	
	/**
	 * test addTweetToGrop-Method
	 */
	@Test
	public void addTweetToGroup(){
		int userID = DBConnector.insertNewUser(44, null, null);
		int groupID = DBConnector.insertTweetGroup(testGroup, userID);
		Tweet toAdd = new Tweet("2018-01-03", "content");
		DBConnector.addTweetToGroup(userID, toAdd, groupID);
		TweetGroup read = DBConnector.getTweetGroupForUser(userID, groupID);
		Assert.assertEquals(read.tweets.size(), 5);
		Assert.assertEquals(read.tweets.get(4).content, "content");
	}
}
