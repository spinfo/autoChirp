//package autoChirp;
//
//import java.sql.SQLException;
//import java.util.ArrayList;
//import java.util.List;
//
//import org.junit.Assert;
//import org.junit.BeforeClass;
//import org.junit.Test;
//
//import autoChirp.tweetCreation.Tweet;
//import autoChirp.tweetCreation.TweetGroup;
//
///**
// * 
// * @author Alena
// * 
// * Junit test-class for the DBConnector functionality
// *
// */
//
//public class DBConnectorTest {
//
//	private static String dbPath = "src/test/resources/";
//	private static String dbFileName = "autoChirp.db";
//	private static String dbCreationFileName = "src/main/resources/database/createDatabaseFile.sql";
//
//	@BeforeClass
//	//connect and reset database
//	public static void dbConnection() {
//		DBConnector.connect(dbPath + dbFileName);
//		DBConnector.createOutputTables(dbCreationFileName);
//	}
//
//	
//
//	@Test
//	public void insertAndGetTweetsTest() throws SQLException {
//		List<Tweet> tweets = new ArrayList<Tweet>();
//		Tweet tweet = new Tweet("1999-11-12 13:23:12", "tweet1");
//		tweets.add(tweet);
//		tweet = new Tweet("2013-11-12 23:23:12", "tweet2");
//		tweets.add(tweet);
//		TweetGroup group = new TweetGroup("test_title1", "description");
//		group.setTweets(tweets);
//		DBConnector.insertTweetGroup(group, 1);
//		DBConnector.insertTweetGroup(group, 2);
//		//TODO  read tweets
//	}
//
//	@Test
//	public void insertAndGetOAuthTokenTest() {
//		int user_id = DBConnector.insertNewUser(9, "oauthToken", "oauthTokenSecret");
//		Assert.assertEquals(1, user_id);
//		user_id = DBConnector.insertNewUser(10, "oauthToken2", "oauthTokenSecret2");
//		Assert.assertEquals(2, user_id);
//		String[] userInfo = DBConnector.getUserConfig(1);
//		Assert.assertEquals("9", userInfo[0]);
//		Assert.assertEquals("oauthToken", userInfo[1]);
//		Assert.assertEquals("oauthTokenSecret", userInfo[2]);
//		userInfo = DBConnector.getUserConfig(2);
//		Assert.assertEquals("10", userInfo[0]);
//		Assert.assertEquals("oauthToken2", userInfo[1]);
//		Assert.assertEquals("oauthTokenSecret2", userInfo[2]);
//	}
//
//	@Test
//	public void insertAndGetTwitterConfigurationTest() {
//		DBConnector.insertTwitterConfiguration("test_callback_url", "test_consumer_key", "test_consumer_secret");
//		String[] twitterConfig = DBConnector.getAppConfig();
//		Assert.assertEquals("test_callback_url", twitterConfig[0]);
//		Assert.assertEquals("test_consumer_key", twitterConfig[1]);
//		Assert.assertEquals("test_consumer_secret", twitterConfig[2]);
//	}
//	
//	@Test 
//	public void insertAndGetGroups(){
//		List<Tweet> tweets = new ArrayList<Tweet>();
//		Tweet tweet = new Tweet("1999-11-12 13:23:12", "tweet1");
//		tweets.add(tweet);
//		tweet = new Tweet("2013-11-12 23:23:12", "tweet2");
//		tweets.add(tweet);
//		TweetGroup group = new TweetGroup("title", "description");
//		group.setTweets(tweets);
//		DBConnector.insertTweetGroup(group, 10);
//		group = new TweetGroup("title", "description");
//		tweets.add(new Tweet("2015-11-12 14:23:12", "tweet3"));
//		//group.setTweets(tweets);
//		DBConnector.insertTweetGroup(group,10 );
//		List<Integer> groupIDs = DBConnector.getGroupIDsForUser(10);
//		for (Integer groupID : groupIDs) {
//			group = DBConnector.getTweetGroupForUser(10, groupID);
//			Assert.assertTrue(group.description.equals("description"));
//			Assert.assertTrue(group.title.equals("title"));
//			Assert.assertTrue(group.enabled == false);
//			DBConnector.deleteTweet(1);
//			DBConnector.deleteGroup(groupID)
//			;
//		}
//	}
//	
//
//	
//}
