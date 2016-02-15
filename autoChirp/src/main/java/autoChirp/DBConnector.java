package autoChirp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import autoChirp.tweetCreation.Tweet;
import autoChirp.tweetCreation.TweetGroup;

/**
 * 
 * @author Alena
 * 
 *         A class for database input/output. Includes methods to write and read
 *         the database
 * 
 */
public class DBConnector {

	private static Connection connection;

	/**
	 * connects to a database
	 * 
	 * @param dbFilePath
	 * @return connection
	 * 
	 */
	public static Connection connect(String dbFilePath) {
		// register the driver
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			System.out.print("DBConnector.connect: ");
			e.printStackTrace();
			System.exit(0);
		}
		try {
			connection = DriverManager.getConnection("jdbc:sqlite:" + dbFilePath);
		} catch (SQLException e) {
			System.out.print("DBConnector.connect: ");
			e.printStackTrace();
			System.exit(0);
		}
		System.out.println("Database '" + dbFilePath + "' successfully opened");
		return connection;
	}

	/**
	 * creates (or overrides) output-tables defined in dbCreationFileName
	 * 
	 * @param dbCreationFileName
	 *
	 */
	public static void createOutputTables(String dbCreationFileName) {
		//read creationFile
		StringBuffer sql = new StringBuffer();
		BufferedReader in;
		try {
			in = new BufferedReader(new FileReader(dbCreationFileName));
			String line = in.readLine();
			while (line != null) {
				sql.append(line + "\n");
				line = in.readLine();
			}
			in.close();
		} catch (IOException e) {
			System.out.print("DBConnector.createOututTables: couldnt create outputtables");
			e.printStackTrace();
			System.exit(0);
		}
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			stmt.executeUpdate(sql.toString());
			stmt.close();
			connection.commit();
			System.out.println("Initialized new output-database.");
		} catch (SQLException e) {
			System.out.print("DBConnector.createOututTables: couldnt create outputtable");
			e.printStackTrace();
		}	
	}

	/**
	 * 
	 * (only for testing)
	 * inserts an url with user_id into wikipedia-table
	 * 
	 * @param url
	 * @param user_id
	 * @return returns true if insertion was successful
	 * 
	 */
	public static boolean isertUrl(String url, int user_id) {
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "INSERT INTO wikipedia (url, user_id) VALUES ('" + url + "', " + "'" + user_id + "'" + ")";
			stmt.executeUpdate(sql);
			stmt.close();
			connection.commit();
		} catch (SQLException e) {
			if (e.getMessage().equals("columns user_id, url are not unique")) {
				System.out.println("Already in DB: " + url + " with user UserID " + user_id);
				return true;
			} else {
				System.out.println("DBConnector.insertUrl:  couldnt insert urls");
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}

	/**
	 * (only for testing)
	 * searches for new urls to parse in wikipedia-table
	 * 
	 * @return map of urls (key) and associated user_ids (value) from
	 *         wikipedia-table
	 */
	public static Map<String, List<Integer>> getUrls() {
		Map<String, List<Integer>> toReturn = new HashMap<String, List<Integer>>();
		ResultSet result = null;
		Statement stmt = null;
		try {
			connection.setAutoCommit(false);
			stmt = connection.createStatement();
			String sql = "SELECT url, user_id FROM wikipedia";
			result = stmt.executeQuery(sql);
		} catch (SQLException e) {
			System.out.print("DBconnector.getUrls: Couldnt read from wikipedia-table");
			e.printStackTrace();
		}
		try {
			while (result.next()) {
				String url = result.getString(1);
				List<Integer> ids = toReturn.get(url);
				if (ids == null)
					ids = new ArrayList<Integer>();
				ids.add(result.getInt(2));
				toReturn.put(url, ids);
				stmt.close();
				connection.commit();
			}
		} catch (SQLException e) {
			System.out.print("DBConnector.getUrls: ");
			e.printStackTrace();
		}
		return toReturn;
		//TODO  delete parsed urls
	}

	/**
	 * Writes a TweetGroup (e.g. created from a wikipedia document )  into the database.
	 * Updates the tables groups and tweets.
	 * 
	 * 
	 * @param description
	 *            - the description for this group
	 * @param tweetGroup
	 *            - a TweetGroup-Object consisting of title, description and a list of tweets
	 * @param userID
	 *            - the users local userID
	 * @return returns true if insertion was successful
	 * 
	 * 
	 */
	public static boolean insertTweetGroup(TweetGroup tweetGroup, int userID) {
		try {
			connection.setAutoCommit(false);
			PreparedStatement prepUsers = connection
					.prepareStatement("INSERT INTO groups(user_id, group_name, description, enabled) VALUES(?,?,?,?)");
			PreparedStatement prepTweets = connection
					.prepareStatement("INSERT INTO tweets(user_id, group_id, scheduled_date, tweet, scheduled, tweeted) VALUES(?,?,?,?,?,?)");
				prepUsers.setInt(1, userID);
				prepUsers.setString(2, tweetGroup.title);
				prepUsers.setString(3, tweetGroup.description);
				prepUsers.setBoolean(4, false);
				prepUsers.executeUpdate();
				Statement stmt = connection.createStatement();
				String sql = "SELECT last_insert_rowid();";
				ResultSet result = stmt.executeQuery(sql);
				int group_id = result.getInt(1);
				for (Tweet tweet : tweetGroup.tweets) {
					prepTweets.setInt(1, userID);
					prepTweets.setInt(2, group_id);
					prepTweets.setString(3, tweet.tweetDate);
					prepTweets.setString(4, tweet.content);
					prepTweets.setBoolean(5, false);
					prepTweets.setBoolean(6, false);
					prepTweets.executeUpdate();
				}		
			prepUsers.close();
			prepTweets.close();
			connection.commit();
		} catch (Exception e) {
			System.out.print("DBConnector.insertTweets: Couldnt insert tweets ");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	
	public static void deleteGroup(int groupID){
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "DELETE FROM groups WHERE group_id = '"+groupID+"'";
			stmt.executeUpdate(sql);
			stmt.close();
			connection.commit();
			sql = "DELETE FROM tweets WHERE group_id='"+groupID+"'";
			stmt.executeUpdate(sql);
			stmt.close();
			connection.commit();
		} catch (SQLException e) {
			System.out.println("DBConnector.deleteGroup:");
			e.printStackTrace();
		}
	}
	
	public static void deleteTweet(int tweetID){
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "DELETE FROM tweets WHERE tweet_id = '"+tweetID+"'";
			stmt.executeUpdate(sql);
			stmt.close();
			connection.commit();
		} catch (SQLException e) {
			System.out.println("DBConnector.deleteTweet:");
			e.printStackTrace();
		}
	}
	/**
	 * updates the field 'enabled' in groups-table
	 * 
	 * @param group_id
	 * @param enabled
	 * @return returns true if update was successful
	 */
	public static boolean updateGroupStatus(int group_id, boolean enabled) {
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "UPDATE groups SET enabled = '" + enabled + "' WHERE (group_id = '" + group_id + "')";
			stmt.executeUpdate(sql);
			stmt.close();
			connection.commit();
			// TODO schedule tweets
		} catch (SQLException e) {
			System.out.println("DBConnector.updateGroupStatus: couldnt update group-status");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * 
	 * (only for testing)
	 * inserts the twitter-configuration parameters for this application
	 * 
	 * @param twitter_callback_url
	 * @param twitter_consumer_key
	 * @param twitter_consumer_secret
	 * @return returns true if insertion was successful
	 */
	public static boolean insertTwitterConfiguration(String twitter_callback_url, String twitter_consumer_key,
			String twitter_consumer_secret) {
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "INSERT INTO configuration (twitter_callback_url, twitter_consumer_key, twitter_consumer_secret) VALUES('"
					+ twitter_callback_url + "','" + twitter_consumer_key + "','" + twitter_consumer_secret + "')";
			stmt.execute(sql);
			stmt.close();
			connection.commit();
		} catch (SQLException e) {
			System.out.println("DBConnector.insertTweitterConfiguration: couldnt insert tweitter-config");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * (only for testing)
	 * reads the twitter-configuration of this app
	 * 
	 * @return String-Array with twitter_callback_url (0), twitter_consumer_key
	 *         (1) twitter_consumer_secret (2)
	 */
	public static String[] getAppConfig() {
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "SELECT twitter_callback_url, twitter_consumer_key, twitter_consumer_secret FROM configuration";
			ResultSet result = stmt.executeQuery(sql);
			String[] toReturn = new String[3];
			toReturn[0] = result.getString(1);
			toReturn[1] = result.getString(2);
			toReturn[2] = result.getString(3);
			stmt.close();
			connection.commit();
			return toReturn;
		} catch (SQLException e) {
			System.out.print("DBConnctor.getTwitterConfiguration: ");
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * checks if the user with the given global twitterID is already registered.
	 * @param twitter_id
	 * 			- the global TwitterID
	 * @return returns the local userID if user already exists, or -1 if not.
	 */
	public static int checkForUser(long twitter_id){
		try {
			connection.setAutoCommit(false);
			String sql = "SELECT twitter_id, user_id FROM users WHERE (twitter_id = '"+twitter_id+"')";
			Statement stmt = connection.createStatement();
			ResultSet result = stmt.executeQuery(sql);
			if(!result.next()){
				stmt.close();
				return -1;
			}
			else {
				int user_id = result.getInt(2);
				stmt.close();
				return user_id;
			}
			
		} catch (SQLException e) {
			System.out.print("DBConnector: checkForUser: ");
			e.printStackTrace();
			return -2;
		}
	}

	/**
	 * creates a new user in the users-table
	 * 
	 * @param twitterID
	 * 			- the global twitterID
	 * @param oauthToken
	 * @param oauthTokenSecret
	 * @return the local userID of the new user or -1 if insertion was not successful
	 */
	public static int insertNewUser(long twitterID, String oauthToken, String oauthTokenSecret) {
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "INSERT INTO users (twitter_id, oauth_token, oauth_token_secret) VALUES ('"
					+ twitterID + "', " + "'" + oauthToken + "', " + "'" + oauthTokenSecret + "' )";
			stmt.executeUpdate(sql);
			stmt.close();
			connection.commit();
			stmt = connection.createStatement();
			sql = "SELECT user_id FROM users WHERE (twitter_id = '" + twitterID + "' AND oauth_token = '"
					+ oauthToken + "' AND oauth_token_secret ='" + oauthTokenSecret + "')";
			ResultSet result = stmt.executeQuery(sql);
			int toReturn = result.getInt(1);
			stmt.close();
			connection.commit();
			return toReturn;
		} catch (SQLException e) {
			System.out.println("DBConnector.insertNewUser: couldnt insert the new user " + twitterID);
			e.printStackTrace();
			return -1;
		}

	}

	/**
	 * reads the user-config of a specific user
	 * 
	 * @param userID
	 * @return String-Array with twitterID (0), oauthToken (1) and
	 *         oauthTokenSecret (2)
	 */
	public static String[] getUserConfig(int userID) {
		try {
			connection.setAutoCommit(false);
			String sql = "SELECT twitter_id, oauth_token, oauth_token_secret FROM users WHERE user_id = '" + userID
					+ "';";
			Statement stmt = connection.createStatement();
			ResultSet result = stmt.executeQuery(sql);
			String[] toReturn = new String[3];
			toReturn[0] = Integer.toString(result.getInt(1));
			toReturn[1] = result.getString(2);
			toReturn[2] = result.getString(3);
			stmt.close();
			connection.commit();
			return toReturn;
		} catch (Exception e) {
			System.out.println("DBConnector: couldnt read config for user_id " + userID);
			e.printStackTrace();
			return null;
		}
	}

	
	public static List<Tweet> getTweetsForUser(int userID, boolean scheduled, boolean tweeted, int groupID, int offset, int limit){
		String query = "SELECT * FROM tweets WHERE(user_id = '"+userID+"' AND group_id = '"+groupID+"' AND scheduled = '"+scheduled+"' AND tweeted = '"+tweeted+"') LIMIT "+limit+" OFFSET "+offset;
		return getTweets(query); 
	}
	
	public static List<Tweet> getTweetsForUser(int userID, boolean scheduled, boolean tweeted, int groupID){
		String query = "SELECT * FROM tweets WHERE(user_id = '"+userID+"' AND group_id = '"+groupID+"' AND scheduled = '"+scheduled+"' AND tweeted = '"+tweeted+"')";
		return getTweets(query);
	}
	
	private static List<Tweet> getTweetsForUser(int userID, int groupID) {
		String query = "SELECT * FROM tweets WHERE(user_id = '"+userID+"' AND group_id = '"+groupID+"')";
		return getTweets(query);
	}

	
	public static List<Tweet> getTweetsForUser(int userID, boolean scheduled, boolean tweeted, int offset, int limit){
		String query = "SELECT * FROM tweets WHERE(user_id = '"+userID+"' AND scheduled = '"+scheduled+"' AND tweeted = '"+tweeted+"') LIMIT "+limit+" OFFSET "+offset;
		return getTweets(query);
	}
	
	public static List<Tweet> getTweetsForUser(int userID, boolean scheduled, boolean tweeted){
		String query = "SELECT * FROM tweets WHERE(user_id = '"+userID+"' AND scheduled = '"+scheduled+"' AND tweeted = '"+tweeted+"')";
		return getTweets(query);
	}
	
	public static List<Tweet> getTweetsForUser(int userID, int offset, int limit){
		String query = "SELECT * FROM tweets WHERE(user_id = '"+userID+"') LIMIT "+limit+" OFFSET "+offset;
		return getTweets(query);
	}
	
	public static List<Tweet> getTweetsForUser(int userID){
		String query = "SELECT * FROM tweets WHERE(user_id = '"+userID+"')";
		return getTweets(query);
	}
	
	private static List<Tweet> getTweets(String query){
		List<Tweet> tweets = new ArrayList<Tweet>();	
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			ResultSet result = stmt.executeQuery(query);
			while(result.next()){
				Tweet tweet = new Tweet(result.getString(4), result.getString(5),result.getInt(1), result.getInt(3),result.getBoolean(6), result.getBoolean(7));
				tweets.add(tweet);
			}
			stmt.close();
			connection.commit();
		} catch (SQLException e) {
			System.out.print("DBConnector.getTweets: ");
			e.printStackTrace();
		}
		return tweets;
	}
	
	
	public static void flagAsTweeted(Tweet tweet, int userID){
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "UPDATE tweets SET tweeted = 'true' WHERE (tweet_id = '"+tweet.tweetID+"')";
			stmt.executeUpdate(sql);
			stmt.close();
			connection.commit();
		} catch (SQLException e) {
			System.out.print("DBConnector.flagAsTweeted: failed");
			e.printStackTrace();
		}	
	}
	
	public static void flagAsScheduled(Tweet tweet, int userID){
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "UPDATE tweets SET scheduled = 'true' WHERE (tweet_id = '"+tweet.tweetID+"')";
			stmt.executeUpdate(sql);
			stmt.close();
			connection.commit();
		} catch (SQLException e) {
			System.out.print("DBConnector.flagAsScheduled: failed");
			e.printStackTrace();
		}	
	}
	
	public static TweetGroup getTweetGroupForUser(int userID, int groupID){
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "SELECT group_name, description, enabled, group_id FROM groups WHERE (user_id = '"+userID+"' AND group_id = '"+groupID+"')";
			ResultSet result = stmt.executeQuery(sql);
			if(!result.next()) return null;
			TweetGroup group = new TweetGroup(result.getInt(4), result.getString(1), result.getString(2), result.getBoolean(3));
			stmt.close();
			connection.commit();
			List<Tweet> tweets = getTweetsForUser(userID, groupID);
			group.setTweets(tweets);
			return group;
		}
		catch(SQLException e){
			System.out.print("DBConnector.getTweetGroupForUser: ");
			e.printStackTrace();
			return null;
		}
	}

	
	public static List<Integer> getGroupIDsForUser(int userID){
		List<Integer> toReturn = new ArrayList<Integer>();
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "SELECT group_id FROM groups WHERE (user_id = '"+userID+"')";
			ResultSet result = stmt.executeQuery(sql);
			while(result.next()){
				toReturn.add(result.getInt(1));
			}
		} catch (SQLException e) {
			System.out.print("DBConnector.getGroupIDsForUser: ");
			e.printStackTrace();
		}
		return toReturn;
	}
	
	
	public static Tweet getTweetByID(int tweetID){
		Tweet toReturn = null;
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "SELECT * FROM tweets WHERE (tweet_id = '"+tweetID+"')";
			ResultSet result = stmt.executeQuery(sql);
			toReturn = new Tweet(result.getString(4), result.getString(5), result.getInt(1), result.getInt(3), result.getBoolean(6), result.getBoolean(7));
		} catch (SQLException e) {
			System.out.print("DBConnector.getGroupIDsForUser: ");
			e.printStackTrace();
		}
		return toReturn;
	}
	
	public static String getGroupTitle(int groupID){
		String toReturn = null;
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "SELECT group_name FROM groups WHERE (group_id = '"+groupID+"')";
			ResultSet result = stmt.executeQuery(sql);
			toReturn = result.getString(1);
		} catch (SQLException e) {
			System.out.print("DBConnector.getGroupIDsForUser: ");
			e.printStackTrace();
		}
		return toReturn;
	}
//	
//	/**
//	 * selects all new (= not yet scheduled) tweets for a specific user and
//	 * group
//	 * 
//	 * @param user_id
//	 * @param group_id
//	 * @return A map of dates (key) and the associated list of tweets (value)
//	 */
//	public static List<Tweet> getTweetsForUser(int user_id, int group_id) {
//		List<Tweet> toReturn = new ArrayList<Tweet>();
//		try {
//			connection.setAutoCommit(false);
//			Statement stmt = connection.createStatement();
//			String sql = "SELECT * FROM tweets WHERE (group_id = '" + group_id + "' AND user_id='" + user_id + "')";
//			ResultSet tweets = stmt.executeQuery(sql);
//			try {
//				while (tweets.next()) {
//					String date = tweets.getString(4);
//					String tweet = tweets.getString(5);
//					int tweetID = tweets.getInt(1);
//					toReturn.add(new Tweet(date,tweet, tweetID));
//				}
//			} catch (SQLException e) {
//				System.out.println("DBConnector.getAllNewTweets: no new tweets to schedule");
//			}
//			
//			stmt.close();
//			connection.commit();
//		} catch (SQLException e) {
//			System.out.println("DBConnector.getAllNewTweets: ");
//			e.printStackTrace();
//		}
//		return toReturn;
//	}
//
//	/**
//	 * selects all new (not yet scheduled) tweets. This method should be
//	 * executed once after application-start
//	 * 
//	 * @return A Map of user_ids (key) and a map of tweets sorted by their
//	 *         tweetdate (value)
//	 */
//
//	public static Map<Integer, List<Tweet>> getAllTweets() {
//		Map<Integer, List<Tweet>> toReturn = new HashMap<Integer, List<Tweet>>();
//		ResultSet group_ids = null;
//		try {
//			connection.setAutoCommit(true);
//			String sql = "SELECT group_id, user_id FROM groups WHERE (enabled = 'true')";
//			Statement stmt = connection.createStatement();
//			group_ids = stmt.executeQuery(sql);
//		} catch (SQLException e) {
//			System.out.println("DBConntor.getAllNewTweets: couldnt read from table groups");
//			e.printStackTrace();
//			return toReturn;
//		}
//		// Select all enabled group_ids with user
//		try {
//			while (group_ids.next()) {
//				int group_id = group_ids.getInt(1);
//				int user_id = group_ids.getInt(2);
//				ResultSet tweets = null;
//				try {
//					Statement stmt2 = connection.createStatement();
//					// Select all tweets for the current group_id
//					String sql2 = "SELECT * FROM tweets WHERE (group_id = '" + group_id + "' AND user_id='" + user_id + "')";
//					tweets = stmt2.executeQuery(sql2);
//				} catch (Exception e) {
//					System.out.println("DBConntor.getAllNewTweets: couldnt read from table tweets");
//					e.printStackTrace();
//					return toReturn;
//				}
//				while (tweets.next()) {
//					String date = tweets.getString(4);
//					String content = tweets.getString(5);
//					int tweetID = tweets.getInt(1);
//					List<Tweet> tweetsForUser = toReturn.get(user_id);
//					if (tweetsForUser == null) {
//						tweetsForUser = new ArrayList<Tweet>();
//					}
//					tweetsForUser.add(new Tweet(date,content, tweetID));
//					toReturn.put(user_id, tweetsForUser);
//				}
//			}
//		} catch (SQLException e) {
//			System.out.println("DBConnector.readAllNewTweets: no new Tweets to schedule");
//			e.printStackTrace();
//			return toReturn;
//		}
//		return toReturn;
//	}
//	
//	public static List<TweetGroup> getActiveGroupsForUser(int user_id){
//		List<TweetGroup> toReturn = new ArrayList<TweetGroup>();
//		try {
//			connection.setAutoCommit(false);
//			Statement stmt = connection.createStatement();
//			String sql = "SELECT group_id, group_name, description FROM groups WHERE (user_id = '"+user_id+"' AND enabled ='true')";
//			ResultSet result = stmt.executeQuery(sql);
//			while(result.next()){
//				TweetGroup group = new TweetGroup(result.getString(2), result.getString(3));
//				int group_id = result.getInt(1);
//				Statement stmt2 = connection.createStatement();
//				String sql2 = "SELECT tweet_id, tweet, scheduled_date FROM tweets WHERE(group_id = '"+group_id+"' AND user_id ='"+user_id+"')";
//				ResultSet result2 = stmt2.executeQuery(sql2);
//				Tweet tweet;
//				while(result2.next()){
//					String content = result.getString(2);
//					String tweetDate = result.getString(3);
//					int tweetID = result.getInt(1);
//					tweet = new Tweet(tweetDate, content, tweetID);
//					group.addTweet(tweet);
//				}
//				
//				toReturn.add(group);
//				stmt2.close();
//			}
//			stmt.close();
//		} catch (SQLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}	
//		return toReturn;
//	}
}
