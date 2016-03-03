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
			sql = "SELECT last_insert_rowid();";
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

	/**
	 * Writes a TweetGroup  into the database.
	 * Updates the tables groups and tweets.
	 *
	 * @param tweetGroup
	 *            - a TweetGroup-Object consisting of title, description and a list of tweets
	 * @param userID
	 *            - the users local userID
	 * @return returns the groupId of the inserted tweetGroup, or -1 if insertion failed
	 *
	 *
	 */
	public static int insertTweetGroup(TweetGroup tweetGroup, int userID) {
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
			return group_id;
		} catch (Exception e) {
			System.out.print("DBConnector.insertTweets: Couldnt insert tweets ");
			e.printStackTrace();
			return -1;
		}

	}

	/**
	 * updates the field 'enabled' in groups-table
	 *
	 * @param group_id
	 * @param enabled
	 * @return returns true if update was successful
	 */
	public static boolean updateGroupStatus(int group_id, boolean enabled, int userID) {
		try {
      int boolint = (enabled) ? 1 : 0;
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "UPDATE groups SET enabled = '" + boolint + "' WHERE (group_id = '" + group_id + "' AND user_id = '"+userID+"')";
			stmt.executeUpdate(sql);
			stmt.close();
			connection.commit();
		} catch (SQLException e) {
			System.out.println("DBConnector.updateGroupStatus: couldnt update group-status");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public static void flagAsScheduled(int tweetID, int userID){
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "UPDATE tweets SET scheduled = 'true' WHERE (tweet_id = '"+tweetID+"' AND user_id = '"+userID+"')";
			stmt.executeUpdate(sql);
			stmt.close();
			connection.commit();
		} catch (SQLException e) {
			System.out.print("DBConnector.flagAsScheduled: failed");
			e.printStackTrace();
		}
	}

	public static void flagAsTweeted(int tweetID, int userID){
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "UPDATE tweets SET tweeted = 'true' WHERE (tweet_id = '"+tweetID+"' AND user_id = '"+userID+"')";
			stmt.executeUpdate(sql);
			stmt.close();
			connection.commit();
		} catch (SQLException e) {
			System.out.print("DBConnector.flagAsTweeted: failed");
			e.printStackTrace();
		}
	}


	public static void deleteGroup(int groupID, int userID){
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "DELETE FROM groups WHERE group_id = '"+groupID+"' AND user_id = '"+userID+"'";
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

	public static void deleteTweet(int tweetID, int userID){
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "DELETE FROM tweets WHERE tweet_id = '"+tweetID+"' AND user_id = '"+userID+"'";
			stmt.executeUpdate(sql);
			stmt.close();
			connection.commit();
		} catch (SQLException e) {
			System.out.println("DBConnector.deleteTweet:");
			e.printStackTrace();
		}
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

	public static List<Tweet> getTweetsForUser(int userID, boolean scheduled, boolean tweeted, int groupID){
		String query = "SELECT * FROM tweets WHERE(user_id = '"+userID+"' AND group_id = '"+groupID+"' AND scheduled = '"+scheduled+"' AND tweeted = '"+tweeted+"') ORDER BY scheduled_date ASC";
		return getTweets(query, userID);
	}

	private static List<Tweet> getTweetsForUser(int userID, int groupID) {
		String query = "SELECT * FROM tweets WHERE(user_id = '"+userID+"' AND group_id = '"+groupID+"') ORDER BY scheduled_date ASC";
		return getTweets(query, userID);
	}

	public static List<Tweet> getTweetsForUser(int userID, boolean scheduled, boolean tweeted){
		String query = "SELECT * FROM tweets WHERE(user_id = '"+userID+"' AND scheduled = '"+scheduled+"' AND tweeted = '"+tweeted+"') ORDER BY scheduled_date ASC";
		return getTweets(query, userID);
	}

	public static List<Tweet> getTweetsForUser(int userID){
		String query = "SELECT * FROM tweets WHERE(user_id = '"+userID+"') ORDER BY scheduled_date ASC";
		return getTweets(query, userID);
	}

	private static List<Tweet> getTweets(String query, int userID){
		List<Tweet> tweets = new ArrayList<Tweet>();
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			ResultSet result = stmt.executeQuery(query);
			while(result.next()){
				Tweet tweet = new Tweet(result.getString(4), result.getString(5),result.getInt(1), result.getInt(3),result.getBoolean(6), result.getBoolean(7), userID);
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


	public static Tweet getTweetByID(int tweetID, int userID){
		System.out.println("getTweetNxID: TweetID: "+ tweetID);
		Tweet toReturn = null;
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "SELECT * FROM tweets WHERE (tweet_id = '"+tweetID+"' AND user_id = '"+userID+"')";
			ResultSet result = stmt.executeQuery(sql);
			toReturn = new Tweet(result.getString(4), result.getString(5), result.getInt(1), result.getInt(3), result.getBoolean(6), result.getBoolean(7), userID);
		} catch (SQLException e) {
			System.out.print("DBConnector.getGroupIDsForUser: ");
			e.printStackTrace();
		}
		return toReturn;
	}

	public static String getGroupTitle(int groupID, int userID){
		String toReturn = null;
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "SELECT group_name FROM groups WHERE (group_id = '"+groupID+"' AND user_id = '"+userID+"')";
			ResultSet result = stmt.executeQuery(sql);
			toReturn = result.getString(1);
		} catch (SQLException e) {
			System.out.print("DBConnector.getGroupIDsForUser: ");
			e.printStackTrace();
		}
		return toReturn;
	}

	public static void editGroup(int groupID,String title, String description, int userID){
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "UPDATE groups SET group_name = '"+title+"', description = '"+description+"'  WHERE (group_id = '"+groupID+"' AND user_id = '"+userID+"')";
			stmt.executeUpdate(sql);
			stmt.close();
			connection.commit();
		} catch (SQLException e) {
			System.out.print("DBConnector.editGroup: ");
			e.printStackTrace();
		}
	}

	public static void editTweet(int tweetID, String content, int userID){
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "UPDATE tweets SET tweet = '"+content+"'  WHERE (tweet_id = '"+tweetID+"' AND user_id = '"+userID+"')";
			stmt.executeUpdate(sql);
			stmt.close();
			connection.commit();
		} catch (SQLException e) {
			System.out.print("DBConnector.editTweet: ");
			e.printStackTrace();
		}
	}

	public static int addTweetToGroup(int userID, Tweet tweet, int groupID){
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "INSERT INTO tweets (user_id, group_id, scheduled_date, tweet, scheduled, tweeted) VALUES ('"
					+ userID + "', " + "'" + groupID + "', " + "'" + tweet.tweetDate + "', " + "'" + tweet.content+"', "+"'false', 'false')";
			stmt.executeUpdate(sql);
			stmt.close();
			connection.commit();
			stmt = connection.createStatement();
			sql = "SELECT last_insert_rowid();";
			ResultSet result = stmt.executeQuery(sql);
			int toReturn = result.getInt(1);
			stmt.close();
			connection.commit();
			DBConnector.updateGroupStatus(groupID, false, userID);
			System.out.println("TweetID: " + toReturn);
			return toReturn;

		} catch (SQLException e) {
			System.out.print("DBConnector.editTweet: ");
			e.printStackTrace();
			return -1;
		}
	}

	public static boolean isEnabledGroup(int groupID, int userID){
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "SELECT enabled FROM groups WHERE (group_id = '"+groupID+"' AND user_id = '"+userID+"')";
			ResultSet result = stmt.executeQuery(sql);
			boolean enabled = result.getBoolean(1);
			stmt.close();
			connection.commit();
			return enabled;
		} catch (SQLException e) {
			System.out.print("DBConnector.editTweet: ");
			e.printStackTrace();
			return false;
		}
	}

	public static Map<Integer,List<TweetGroup>> getAllEnabledGroups(){
		Map<Integer,List<TweetGroup>> toReturn = new HashMap<Integer,List<TweetGroup>>();
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "SELECT user_id, group_id FROM groups WHERE (enabled = 'true')";
			ResultSet result = stmt.executeQuery(sql);
			TweetGroup group;
			while(result.next()){
				int userID = result.getInt(1);
				int groupID = result.getInt(2);
				group = DBConnector.getTweetGroupForUser(userID, groupID);
				List<TweetGroup> groupList = toReturn.get(userID);
				if(groupList == null){
					groupList = new ArrayList<TweetGroup>();
				}
				groupList.add(group);
				toReturn.put(userID, groupList);
			}
			stmt.close();
			connection.commit();
		} catch (SQLException e) {
			System.out.print("DBConnector.getAllEnabledGroupsByUser: ");
			e.printStackTrace();
		}
		return toReturn;
	}

	public static void deleteUser(int userID){
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "DELETE FROM users WHERE user_id = '"+userID+"'";
			stmt.executeUpdate(sql);
			stmt.close();
			stmt = connection.createStatement();
			sql = "DELETE FROM groups WHERE user_id = '"+userID+"'";
			stmt.executeUpdate(sql);
			stmt.close();
			stmt = connection.createStatement();
			sql = "DELETE FROM tweets WHERE user_id = '"+userID+"'";
			stmt.executeUpdate(sql);
			stmt.close();
			connection.commit();
		} catch (SQLException e) {
			System.out.print("DBConnector.deleteUser: ");
			e.printStackTrace();
		}
	}
}
