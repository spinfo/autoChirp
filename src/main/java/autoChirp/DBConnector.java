package autoChirp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import autoChirp.tweetCreation.Tweet;
import autoChirp.tweetCreation.TweetGroup;

/**
 * A class for database input/output. Includes methods to write in and read from
 * the database
 *
 * @author Alena Geduldig
 * @editor Philip Schildkamp
 *
 */
public class DBConnector {

	private static Connection connection;

	/**
	 * connects to a database
	 *
	 * @param dbFilePath
	 *            file to database
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
		}
		try {
			connection = DriverManager.getConnection("jdbc:sqlite:" + dbFilePath);
		} catch (SQLException e) {
			System.out.print("DBConnector.connect: ");
			e.printStackTrace();
		}
		System.out.println("Database '" + dbFilePath + "' successfully opened");
		return connection;
	}

	/**
	 * creates (or overrides) output-tables defined in dbCreationFileName
	 *
	 * @param dbCreationFileName
	 *            file to the db-specification
	 *
	 */
	public static void createOutputTables(String dbCreationFileName) {
		// read creationFile
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
	 * checks if the user with the given twitterID is already registered.
	 *
	 * @param twitter_id
	 *            the global TwitterID
	 * @return returns the local userID if user already exists in the database,
	 *         or -1 if not.
	 */
	public static int checkForUser(long twitter_id) {
		int toReturn;
		try {
			connection.setAutoCommit(false);
			String sql = "SELECT twitter_id, user_id FROM users WHERE (twitter_id = '" + twitter_id + "')";
			Statement stmt = connection.createStatement();
			ResultSet result = stmt.executeQuery(sql);
			if (!result.next()) {
				toReturn = -1;
			} else {
				toReturn = result.getInt(2);
			}
			stmt.close();
			connection.commit();
			return toReturn;
		} catch (SQLException e) {
			System.out.print("DBConnector: checkForUser: ");
			e.printStackTrace();
			return -1;
		}
	}

	/**
	 * creates a new user in the table 'users' and returns its local userID
	 *
	 * @param twitterID
	 *            the users global twitterID
	 * @param oauthToken
	 *            the users twitter oauthToken
	 * @param oauthTokenSecret
	 *            the users twitter oauthTokenSecret
	 * @return the local userID of the new user or -1 if insertion was not
	 *         successful
	 */
	public static int insertNewUser(long twitterID, String oauthToken, String oauthTokenSecret) {
		int toReturn;
		try {
			// insert user
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "INSERT INTO users (twitter_id, oauth_token, oauth_token_secret) VALUES ('" + twitterID + "', "
					+ "'" + oauthToken + "', " + "'" + oauthTokenSecret + "' )";
			stmt.executeUpdate(sql);
			stmt.close();
			connection.commit();
			// get userID
			stmt = connection.createStatement();
			sql = "SELECT last_insert_rowid();";
			ResultSet result = stmt.executeQuery(sql);
			toReturn = result.getInt(1);
			stmt.close();
			connection.commit();
		} catch (SQLException e) {
			System.out.println("DBConnector.insertNewUser: couldnt insert the new user " + twitterID);
			e.printStackTrace();
			toReturn = -1;
		}
		return toReturn;
	}

	/**
	 * returns the config.-attributes for the given userID as a string-array of
	 * lenth 3. config[0]: the users global twitterID config[1]: the users
	 * oauthToken config[2]: the users oauthTokenSecret
	 *
	 * @param userID
	 *            userID
	 * @return string-array with twitterID (0), oauthToken (1) and
	 *         oauthTokenSecret (2)
	 */
	public static String[] getUserConfig(int userID) {
		String[] toReturn = null;
		try {
			connection.setAutoCommit(false);
			String sql = "SELECT twitter_id, oauth_token, oauth_token_secret FROM users WHERE user_id = '" + userID
					+ "';";
			Statement stmt = connection.createStatement();
			ResultSet result = stmt.executeQuery(sql);
			if (!result.next()) {
				return toReturn;
			}
			toReturn = new String[3];
			toReturn[0] = Integer.toString(result.getInt(1));
			toReturn[1] = result.getString(2);
			toReturn[2] = result.getString(3);
			stmt.close();
			connection.commit();
		} catch (Exception e) {
			System.out.println("DBConnector: couldnt read config for user_id " + userID);
			e.printStackTrace();
		}
		return toReturn;
	}

	/**
	 * returns the number of registered users as int.
	 *
	 * @return an int-count for registered users
	 */
	public static int getRegisteredUsers() {
		int toReturn = 0;

		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			ResultSet result = stmt.executeQuery("SELECT Count(*) FROM users;");
			toReturn = result.getInt(1);
			stmt.close();
			connection.commit();
		} catch (SQLException e) {
			System.out.println("DBConnector.countTweets: ");
			e.printStackTrace();
			return 0;
		}

		return toReturn;
	}

	/**
	 * writes a TweetGroup into the database and returns its new groupID.
	 * updates the tables 'groups' and 'tweets'.
	 *
	 * @param tweetGroup
	 *            a TweetGroup-Object consisting of title, description and a
	 *            list of tweets
	 * @param userID
	 *            the users local userID
	 * @return the groupID of the inserted tweetGroup, or -1 if insertion failed
	 *
	 */
	public static int insertTweetGroup(TweetGroup tweetGroup, int userID) {
		int toReturn;
		try {
			connection.setAutoCommit(false);
			PreparedStatement prepGroups = connection
					.prepareStatement("INSERT INTO groups(user_id, group_name, description, enabled, threaded) VALUES(?,?,?,?,?)");
			PreparedStatement prepTweets = connection.prepareStatement(
					"INSERT INTO tweets(user_id, group_id, scheduled_date, tweet, scheduled, tweeted, img_url, longitude, latitude) VALUES(?,?,?,?,?,?,?,?,?)");
			// update table groups
			prepGroups.setInt(1, userID);
			prepGroups.setString(2, tweetGroup.title);
			prepGroups.setString(3, tweetGroup.description);
			prepGroups.setBoolean(4, false);
			prepGroups.setBoolean(5, tweetGroup.threaded);
			prepGroups.executeUpdate();
			// get groupID
			Statement stmt = connection.createStatement();
			String sql = "SELECT last_insert_rowid();";
			ResultSet result = stmt.executeQuery(sql);
			int group_id = result.getInt(1);
			toReturn = group_id;
			// update table 'tweets'
			for (Tweet tweet : tweetGroup.tweets) {
				prepTweets.setInt(1, userID);
				prepTweets.setInt(2, group_id);
				prepTweets.setString(3, tweet.tweetDate);
				prepTweets.setString(4, tweet.content);
				prepTweets.setBoolean(5, false);
				prepTweets.setBoolean(6, false);
				prepTweets.setString(7, tweet.imageUrl);
				prepTweets.setFloat(8, tweet.longitude);
				prepTweets.setFloat(9, tweet.latitude);
				prepTweets.executeUpdate();
			}
			prepGroups.close();
			prepTweets.close();
			stmt.close();
			connection.commit();
		} catch (Exception e) {
			System.out.print("DBConnector.insertTweets: Couldnt insert tweets ");
			e.printStackTrace();
			toReturn = -1;
		}
		return toReturn;
	}

	/**
	 * enables/disables (activates/deactivates) the given TweetGroup for
	 * tweeting (if userID fits to groupID) and updates the field 'enabled' in
	 * table 'groups'
	 *
	 * @param groupID
	 *            groupID
	 * @param enabled
	 *            to update
	 * @param userID
	 *            userID
	 * @return returns true if update was successful
	 */
	public static boolean updateGroupStatus(int groupID, boolean enabled, int userID) {
		try {
			int boolint = (enabled) ? 1 : 0;
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "UPDATE groups SET enabled = '" + boolint + "' WHERE (group_id = '" + groupID
					+ "' AND user_id = '" + userID + "')";
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

	/**
	 * flags the given tweet as scheduled (if userID fits to tweetID )
	 *
	 * @param tweetID
	 *            tweetID
	 * @param userID
	 *            userID
	 * @return returns true if update was successful
	 */
	public static boolean flagAsScheduled(int tweetID, int userID) {
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "UPDATE tweets SET scheduled = '1' WHERE (tweet_id = '" + tweetID + "' AND user_id = '"
					+ userID + "')";
			stmt.executeUpdate(sql);
			stmt.close();
			connection.commit();
		} catch (SQLException e) {
			System.out.print("DBConnector.flagAsScheduled: failed");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * flags the given tweet as tweeted (if userID fits to tweetID )
	 *
	 * @param tweetID
	 *            tweetID
	 * @param userID
	 *            userID
	 * @return returns true if update was successful
	 */
	public static boolean flagAsTweeted(int tweetID, int userID) {
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "UPDATE tweets SET tweeted = '1' WHERE (tweet_id = '" + tweetID + "' AND user_id = '" + userID
					+ "')";
			stmt.executeUpdate(sql);
			stmt.close();
			connection.commit();
		} catch (SQLException e) {
			System.out.print("DBConnector.flagAsTweeted: failed");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * deletes the TweetGroup with the given groupID in table 'groups' and all
	 * tweets in table 'tweets' related to this group
	 *
	 * @param groupID
	 *            to delete
	 * @param userID
	 *            userID
	 */
	public static void deleteGroup(int groupID, int userID) {
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "DELETE FROM groups WHERE group_id = '" + groupID + "' AND user_id = '" + userID + "'";
			stmt.executeUpdate(sql);
			stmt.close();
			connection.commit();
			sql = "DELETE FROM tweets WHERE group_id='" + groupID + "'";
			stmt = connection.createStatement();
			stmt.executeUpdate(sql);
			stmt.close();
			connection.commit();
		} catch (SQLException e) {
			System.out.println("DBConnector.deleteGroup:");
			e.printStackTrace();
		}
	}

	/**
	 * deletes a single tweet in table 'tweets'
	 *
	 * @param tweetID
	 *            to delete
	 * @param userID
	 *            userID
	 */
	public static void deleteTweet(int tweetID, int userID) {
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "DELETE FROM tweets WHERE tweet_id = '" + tweetID + "' AND user_id = '" + userID + "'";
			stmt.executeUpdate(sql);
			stmt.close();
			connection.commit();
		} catch (SQLException e) {
			System.out.println("DBConnector.deleteTweet:");
			e.printStackTrace();
		}
	}

	/**
	 * returns a list of all tweets from the specified tweetGroup with the given
	 * scheduled- and tweeted-status
	 *
	 * @param userID
	 *            userID
	 * @param scheduled
	 *            selected scheduled status
	 * @param tweeted
	 *            selected tweeted status
	 * @param groupID
	 *            goupID
	 * @return a list of all tweets which satisfy the given status-combination
	 */
	public static List<Tweet> getTweetsForUser(int userID, boolean scheduled, boolean tweeted, int groupID) {
		int scheduledInt = (scheduled) ? 1 : 0;
		int tweetedInt = (tweeted) ? 1 : 0;
		String query = "SELECT * FROM tweets WHERE(user_id = '" + userID + "' AND group_id = '" + groupID
				+ "' AND scheduled = '" + scheduledInt + "' AND tweeted = '" + tweetedInt
				+ "') ORDER BY scheduled_date ASC";
		return getTweets(query, userID);
	}

	/**
	 * @param userID
	 * @param groupID
	 * @return all tweets with the given groupID
	 */
	private static List<Tweet> getTweetsForUser(int userID, int groupID) {
		String query = "SELECT * FROM tweets WHERE(user_id = '" + userID + "' AND group_id = '" + groupID
				+ "') ORDER BY scheduled_date ASC";
		return getTweets(query, userID);
	}
	


	/**
	 * returns a list of all tweets of a user with the given scheduled- and
	 * tweeted-status
	 *
	 * @param userID
	 *            iserID
	 * @param scheduled
	 *            selected scheduled status
	 * @param tweeted
	 *            selected tweeted status
	 * @return all tweets which satisfy the given status-combination
	 */
	public static List<Tweet> getTweetsForUser(int userID, boolean scheduled, boolean tweeted) {
		int scheduledInt = (scheduled) ? 1 : 0;
		int tweetedInt = (tweeted) ? 1 : 0;
		String query = "SELECT * FROM tweets WHERE(user_id = '" + userID + "' AND scheduled = '" + scheduledInt
				+ "' AND tweeted = '" + tweetedInt + "') ORDER BY scheduled_date ASC";
		return getTweets(query, userID);
	}

	/**
	 * returns a list of all tweets from a user
	 *
	 * @param userID
	 *            userID
	 * @return all tweets from the user
	 */
	public static List<Tweet> getTweetsForUser(int userID) {
		String query = "SELECT * FROM tweets WHERE(user_id = '" + userID + "') ORDER BY scheduled_date ASC";
		return getTweets(query, userID);
	}

	/**
	 * returns a list of 5 upcoming tweets
	 *
	 * @return a list of 5 upcoming tweets
	 */
	public static List<Tweet> getUpcomingTweets() {
		String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
		String query = "SELECT * FROM tweets WHERE(scheduled = 1 AND tweeted = 0 AND scheduled_date > '" + now
				+ "') ORDER BY scheduled_date ASC LIMIT 5";
		return getTweets(query, 0);
	}

	/**
	 * returns a list of 5 recent tweets
	 *
	 * @return a list of 5 recent tweets
	 */
	public static List<Tweet> getLatestTweets() {
		String query = "SELECT * FROM tweets WHERE(tweeted = 1) ORDER BY scheduled_date DESC LIMIT 5";
		return getTweets(query, 0);
	}

	/**
	 *
	 * @param query
	 * @param userID
	 * @return list of tweets selected with the query
	 */
	private static List<Tweet> getTweets(String query, int userID) {
		List<Tweet> toReturn = new ArrayList<Tweet>();
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			ResultSet result = stmt.executeQuery(query);
			while (result.next()) {
				Tweet tweet = new Tweet(result.getString(4), result.getString(5), result.getInt(1), result.getInt(3),
						result.getBoolean(6), result.getBoolean(7), userID, result.getString(8), result.getFloat(9),
						result.getFloat(10), result.getLong(11));
				toReturn.add(tweet);
			}
			stmt.close();
			connection.commit();
		} catch (SQLException e) {
			System.out.print("DBConnector.getTweets: ");
			e.printStackTrace();
		}
		return toReturn;
	}

	/**
	 * returns the number of scheduled Tweets
	 *
	 * @return number of scheduled Tweets
	 */
	public static int getScheduledTweets() {
		String query = "SELECT Count(*) FROM tweets WHERE(scheduled = 1 AND tweeted = 0)";
		return countTweets(query);
	}

	/**
	 * returns the number of published Tweets
	 *
	 * @return number of published Tweets
	 */
	public static int getPublishedTweets() {
		String query = "SELECT Count(*) FROM tweets WHERE(tweeted = 1)";
		return countTweets(query);
	}

	/**
	 * returns the number of published Tweets
	 *
	 * @return number of published Tweets
	 */
	public static int getAllTweets() {
		String query = "SELECT Count(*) FROM tweets";
		return countTweets(query);
	}

	/**
	 * returns the number of Tweets for the query
	 *
	 * @param query
	 *            the SQL query
	 * @return number of tweets
	 */
	public static int countTweets(String query) {
		int toReturn;

		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			ResultSet result = stmt.executeQuery(query);
			toReturn = result.getInt(1);
			stmt.close();
			connection.commit();
		} catch (SQLException e) {
			System.out.println("DBConnector.countTweets: ");
			e.printStackTrace();
			return 0;
		}

		return toReturn;
	}

	/**
	 * returns the tweetGroup with the given groupID (if userID fits to groupID)
	 *
	 * @param userID
	 *            userID
	 * @param groupID
	 *            groupID
	 * @return tweetGroup with groupID
	 */
	public static TweetGroup getTweetGroupForUser(int userID, int groupID) {
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "SELECT group_name, description, enabled, group_id, threaded FROM groups WHERE (user_id = '" + userID
					+ "' AND group_id = '" + groupID + "')";
			ResultSet result = stmt.executeQuery(sql);
			if (!result.next())
				return null;
			TweetGroup group = new TweetGroup(result.getInt(4), result.getString(1), result.getString(2),
					result.getBoolean(3), result.getBoolean(5));
			stmt.close();
			connection.commit();
			List<Tweet> tweets = getTweetsForUser(userID, groupID);
			group.setTweets(tweets);
			return group;
		} catch (SQLException e) {
			System.out.print("DBConnector.getTweetGroupForUser: ");
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * returns a list with all groupIDs for the given user
	 *
	 * @param userID
	 *            userID
	 * @return groupIDs of the users group
	 */
	public static List<Integer> getGroupIDsForUser(int userID) {
		List<Integer> toReturn = new ArrayList<Integer>();
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "SELECT group_id FROM groups WHERE (user_id = '" + userID + "')";
			ResultSet result = stmt.executeQuery(sql);
			while (result.next()) {
				toReturn.add(result.getInt(1));
			}
		} catch (SQLException e) {
			System.out.print("DBConnector.getGroupIDsForUser: ");
			e.printStackTrace();
		}
		return toReturn;
	}

	/**
	 * reads a single tweet from the database specified by tweetID (if userID
	 * fits to tweetID)
	 *
	 * @param tweetID
	 *            tweetID
	 * @param userID
	 *            userID
	 * @return tweet with tweetID
	 */
	public static Tweet getTweetByID(int tweetID, int userID) {
		Tweet toReturn = null;
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "SELECT * FROM tweets WHERE (tweet_id = '" + tweetID + "' AND user_id = '" + userID + "')";
			ResultSet result = stmt.executeQuery(sql);
			if (!result.next()) {
				return null;
			}
			toReturn = new Tweet(result.getString(4), result.getString(5), result.getInt(1), result.getInt(3),
					result.getBoolean(6), result.getBoolean(7), userID, result.getString(8), result.getFloat(9),
					result.getFloat(10), result.getLong(11));
		} catch (SQLException e) {
			System.out.print("DBConnector.getGroupIDsForUser: ");
			e.printStackTrace();
		}
		return toReturn;
	}
	
	/**
	 * reads a single tweet from the database specified by tweetID (if userID
	 * fits to tweetID)
	 *
	 * @param tweetID
	 *            tweetID
	 * @return tweet with tweetID
	 */
	public static Tweet getTweetByID(int tweetID) {
		Tweet toReturn = null;
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "SELECT * FROM tweets WHERE (tweet_id = '" + tweetID + "')";
			ResultSet result = stmt.executeQuery(sql);
			if (!result.next()) {
				return null;
			}
			toReturn = new Tweet(result.getString(4), result.getString(5), result.getInt(1), result.getInt(3),
					result.getBoolean(6), result.getBoolean(7), 0, result.getString(8), result.getFloat(9),
					result.getFloat(10), result.getLong(11));
		} catch (SQLException e) {
			System.out.print("DBConnector.getGroupIDsForUser: ");
			e.printStackTrace();
		}
		return toReturn;
	}
	

	/**
	 * returns the groupTitle of the given group (if userID fits to groupID)
	 *
	 * @param groupID
	 *            groupID
	 * @return groupTitle of the given group
	 */
	public static String getGroupTitle(int groupID) {
		String toReturn = null;
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "SELECT group_name FROM groups WHERE (group_id = '" + groupID + "')";
			ResultSet result = stmt.executeQuery(sql);
			if (!result.next()) {
				return null;
			}
			toReturn = result.getString(1);
		} catch (SQLException e) {
			System.out.print("DBConnector.getGroupIDsForUser: ");
			e.printStackTrace();
		}
		return toReturn;
	}	

	/**
	 * returns the groupTitle of the given group (if userID fits to groupID)
	 *
	 * @param groupID
	 *            groupID
	 * @param userID
	 *            userID
	 * @return groupTitle of the given group
	 */
	public static String getGroupTitle(int groupID, int userID) {
		String toReturn = null;
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "SELECT group_name FROM groups WHERE (group_id = '" + groupID + "' AND user_id = '" + userID
					+ "')";
			ResultSet result = stmt.executeQuery(sql);
			if (!result.next()) {
				return null;
			}
			toReturn = result.getString(1);
		} catch (SQLException e) {
			System.out.print("DBConnector.getGroupIDsForUser: ");
			e.printStackTrace();
		}
		return toReturn;
	}

	/**
	 * updates a groups description or title in the database (if userID fits to
	 * groupID)
	 *
	 * @param groupID
	 *            groupID
	 * @param title
	 *            new title
	 * @param description
	 *            new description
	 * @param userID
	 *            userID
	 */
	public static void editGroup(int groupID, String title, String description, int userID) {
		try {
			connection.setAutoCommit(false);
			PreparedStatement stmt = connection.prepareStatement(
					"UPDATE groups SET group_name = ?, description = ?  WHERE (group_id = ?  AND user_id = ?)");
			// String sql = "UPDATE groups SET group_name = '" + title + "',
			// description = '" + description
			// + "' WHERE (group_id = '" + groupID + "' AND user_id = '" +
			// userID + "')";
			stmt.setString(1, title);
			stmt.setString(2, description);
			stmt.setInt(3, groupID);
			stmt.setInt(4, userID);
			stmt.executeUpdate();
			stmt.close();
			connection.commit();
		} catch (SQLException e) {
			System.out.print("DBConnector.editGroup: ");
			e.printStackTrace();
		}
	}

	/**
	 * updates the content, imageUrl and/or geo-location of a single tweet (if
	 * userID fits to tweetID)
	 *
	 * @param tweetID
	 *            tweetID
	 * @param content
	 *            new content
	 * @param userID
	 *            userID
	 * @param imageUrl
	 *            new imageUrl
	 * @param longitude
	 *            new longitude
	 * @param latitude
	 *            new latitude
	 */
	public static void editTweet(int tweetID, String content, int userID, String imageUrl, float longitude,
			float latitude, String tweetDate) {
		try {
			connection.setAutoCommit(false);
			PreparedStatement stmt = connection.prepareStatement(
					"UPDATE tweets SET tweet = ?, img_url = ?, longitude = ?, latitude = ?, scheduled_date = ? WHERE (tweet_id = ? AND user_id = ?)");
			// String sql = "UPDATE tweets SET tweet = '" + content + "',
			// img_url = '" + imageUrl + "', longitude = '"
			// + longitude + "', latitude = '" + latitude + "' WHERE (tweet_id =
			// '" + tweetID
			// + "' AND user_id = '" + userID + "')";
			stmt.setString(1, content);
			stmt.setString(2, imageUrl);
			stmt.setFloat(3, longitude);
			stmt.setFloat(4, latitude);
			stmt.setString(5, tweetDate);
			stmt.setInt(6, tweetID);
			stmt.setInt(7, userID);
			stmt.executeUpdate();
			stmt.close();
			connection.commit();
		} catch (SQLException e) {
			System.out.print("DBConnector.editTweet: ");
			e.printStackTrace();
		}
	}
	
	/**
	 * sets the statusID of a published tweet 
	 * @param tweet
	 * @param statusId
	 */
	public static void addStatusID(int tweetID, long statusId){
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "UPDATE tweets SET status_id = '" + statusId + "' WHERE (tweet_id = '" + tweetID+"')";
			stmt.executeUpdate(sql);
			stmt.close();
		} catch (SQLException e) {
			System.out.println("DBConnector.addStatusID: ");
			e.printStackTrace();
		}
		
		
	}

	/**
	 * adds a single tweet to an existing group and returns its tweetID
	 *
	 * @param userID
	 *            userID
	 * @param tweet
	 *            to add
	 * @param groupID
	 *            groupID
	 * @return tweetID of the new tweet
	 */
	public static int addTweetToGroup(int userID, Tweet tweet, int groupID) {
		int toReturn = -1;
		TweetGroup group = getTweetGroupForUser(userID, groupID);
		group.addTweet(tweet);
		for(int i =  group.tweets.size()-1; i >=0; i--){
			deleteTweet(group.tweets.get(i).tweetID, userID);
		}
		try{
			connection.setAutoCommit(false);
			PreparedStatement prepStmt = connection.prepareStatement(
				"INSERT INTO tweets (user_id, group_id, scheduled_date, tweet, scheduled, tweeted, img_url, longitude, latitude) VALUES(?,?,?,?,?,?,?,?,?)");
			for (Tweet t : group.tweets) {
				prepStmt.setInt(1, userID);
				prepStmt.setInt(2, groupID);
				prepStmt.setString(3, t.tweetDate);
				prepStmt.setString(4, t.content);
				prepStmt.setBoolean(5, t.scheduled);
				prepStmt.setBoolean(6, t.tweeted);
				prepStmt.setString(7, t.imageUrl);
				prepStmt.setFloat(8, t.longitude);
				prepStmt.setFloat(9, t.latitude);
				prepStmt.executeUpdate();
				if(t.tweetID == tweet.tweetID){
					Statement stmt = connection.createStatement();
					String sql = "SELECT last_insert_rowid();";
					ResultSet result = stmt.executeQuery(sql);
					toReturn = result.getInt(1);
					stmt.close();
				}
			}
			prepStmt.close();
			connection.commit();
			DBConnector.updateGroupStatus(groupID, false, userID);
			return toReturn;
		}
		catch(SQLException e){
			System.out.print("DBConnector.addTweetToGroup: ");
			e.printStackTrace();
			return -1;
		}
	}


	/**
	 * returns the status (enabled/disabled) of the given group
	 *
	 * @param groupID
	 *            groupID
	 * @param userID
	 *            userID
	 * @return enabled enabled status
	 */
	public static boolean isEnabledGroup(int groupID, int userID) {
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "SELECT enabled FROM groups WHERE (group_id = '" + groupID + "' AND user_id = '" + userID
					+ "')";
			ResultSet result = stmt.executeQuery(sql);
			boolean enabled = result.getBoolean(1);
			stmt.close();
			connection.commit();
			return enabled;
		} catch (SQLException e) {
			System.out.print("DBConnector.isEnabledGroup: ");
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * @param groupID
	 * @param userID
	 * @return returns true if group is a threaded group
	 */
	public static boolean isThreadedGroup(int groupID, int userID){
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "SELECT threaded FROM groups WHERE (group_id = '" + groupID + "' AND user_id = '" + userID
					+ "')";
			ResultSet result = stmt.executeQuery(sql);
			boolean threaded = result.getBoolean(1);
			stmt.close();
			connection.commit();
			return threaded;
		} catch (SQLException e) {
			System.out.print("DBConnector.isThreadedGroup: ");
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * updates the threaded-attribute of the given tweetgroup
	 * table 'groups'
	 * @param groupID
	 * @param userID
	 * @param threaded
	 * @return returns true is update was successful
	 */
	public static boolean setThreaded(int groupID, int userID, boolean threaded){
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			int boolint = (threaded) ? 1 : 0;
			String sql = "UPDATE groups SET threaded = '" + boolint + "' WHERE (group_id = '" + groupID
					+ "' AND user_id = '" + userID + "')";
			stmt.executeUpdate(sql);
			stmt.close();
			connection.commit();
		} catch (SQLException e) {
			System.out.println("DBConnector.setThreaded: ");
			e.printStackTrace();
			return false;
		}
		return true;
		
	}
	
	

	/**
	 *
	 * returns a map of all enabled (active) groups from the database, sorted by
	 * its usersIDs. This method is called once at the start of the application,
	 * to schedule all active tweets.
	 *
	 * @return a map of all active TweetGroups sorted by its users
	 */
	public static Map<Integer, List<TweetGroup>> getAllEnabledGroups() {
		Map<Integer, List<TweetGroup>> toReturn = new HashMap<Integer, List<TweetGroup>>();
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "SELECT user_id, group_id FROM groups WHERE (enabled = '1')";
			ResultSet result = stmt.executeQuery(sql);
			TweetGroup group;
			while (result.next()) {
				int userID = result.getInt(1);
				int groupID = result.getInt(2);
				group = DBConnector.getTweetGroupForUser(userID, groupID);
				List<TweetGroup> groupList = toReturn.get(userID);
				if (groupList == null) {
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

	/**
	 * deletes a user from the database - deletes user config. from table
	 * 'users' - deletes all tweetGroups in table 'groups' - deletes all tweets
	 * in table 'tweets'
	 *
	 * @param userID
	 *            userID
	 */
	public static void deleteUser(int userID) {
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "DELETE FROM users WHERE user_id = '" + userID + "'";
			stmt.executeUpdate(sql);
			stmt.close();
			stmt = connection.createStatement();
			sql = "DELETE FROM groups WHERE user_id = '" + userID + "'";
			stmt.executeUpdate(sql);
			stmt.close();
			stmt = connection.createStatement();
			sql = "DELETE FROM tweets WHERE user_id = '" + userID + "'";
			stmt.executeUpdate(sql);
			stmt.close();
			connection.commit();
		} catch (SQLException e) {
			System.out.print("DBConnector.deleteUser: ");
			e.printStackTrace();
		}
	}

	/**
	 * @param group group to repeat
	 * @param userID  user of group
	 * @param delayInSeconds delay in seconds
	 * @return  a copy of the given group with updated tweetdates (old date plus delayInSeconds seconds)
	 */
	public static TweetGroup createRepeatGroupInSeconds(TweetGroup group, int userID, int delayInSeconds, String newTitle){
		TweetGroup repeatGroup = new TweetGroup(newTitle, group.description); 
		List<Tweet> repeatTweets = new ArrayList<Tweet>();
		Tweet repeatTweet;
		for (Tweet tweet : group.tweets) {
			LocalDateTime time = LocalDateTime.parse(tweet.tweetDate,
					DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
			time = time.plusSeconds(delayInSeconds);
			String timeString = time.format( DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
			repeatTweet = new Tweet(timeString, tweet.content, tweet.imageUrl, tweet.longitude, tweet.latitude);
			repeatTweets.add(repeatTweet);
		}
		repeatGroup.setTweets(repeatTweets);
		return repeatGroup;
	}


	/**
	 * @param group group to repeat
	 * @param userID  user of group
	 * @param delayInYears delay in years
	 * @return  a copy of the given group with updated tweetdates (old date plus delayInYears years)
	 */
	public static TweetGroup createRepeatGroupInYears(TweetGroup group, int userID, int delayInYears) {
		TweetGroup updatedGroup = new TweetGroup(group.title, group.description);
		List<Tweet> updatedTweets = new ArrayList<Tweet>();
		Tweet updatedTweet;
		String timeString = null;
		for (Tweet tweet : group.tweets) {
			LocalDateTime time = LocalDateTime.parse(tweet.tweetDate,
					DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
			time = time.plusYears(delayInYears);
			timeString = time.format( DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
			updatedTweet = new Tweet(timeString, tweet.content, tweet.imageUrl, tweet.longitude, tweet.latitude);
			updatedTweets.add(updatedTweet);
		}
		String newYear = timeString.substring(0, 4);
		updatedGroup.setTweets(updatedTweets);
		updatedGroup.title = group.title+"_"+newYear;
		return updatedGroup;
	}

	/**
	 * @param tweetID
	 * @param groupID
	 * @param userID
	 * @return returns the status_id of the last published tweet in this tweetgroup
	 */
	public static long getReplyID(int tweetID, int groupID, int userID) {
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "SELECT status_id FROM tweets WHERE (group_id = '" + groupID + "' AND user_id = '" + userID
					+ "' AND tweet_id = '"+(tweetID-1)+"')";
			ResultSet result = stmt.executeQuery(sql);
			long replyID = result.getLong(1);
			stmt.close();
			connection.commit();
			return replyID;
		} catch (SQLException e) {
			System.out.print("DBConnector.getReplyID: ");
			e.printStackTrace();
			return -1;
		}
	}
	
}
