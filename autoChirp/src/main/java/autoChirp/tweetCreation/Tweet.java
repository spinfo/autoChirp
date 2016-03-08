package autoChirp.tweetCreation;

import autoChirp.DBConnector;

/**
 * 
 * @author Alena Geduldig
 * 
 *         represents a single tweet consisting of content, tweetDate, tweetID,
 *         groupID, groupName and the status-attributes scheduled and tweeted
 */
public class Tweet implements Comparable<Tweet> {
	public String tweetDate;
	public String content;
	public int tweetID;
	public int groupID;
	public boolean scheduled;
	public boolean tweeted;
	public String groupName;

	/**
	 * Constructor for tweets read from the database
	 * 
	 * @param tweetDate
	 * @param content
	 * @param tweetID
	 * @param scheduled
	 * @param tweeted
	 */
	public Tweet(String tweetDate, String content, int tweetID, int groupID, boolean scheduled, boolean tweeted,
			int userID) {
		this.tweetDate = tweetDate;
		this.content = content;
		this.tweetID = tweetID;
		this.scheduled = scheduled;
		this.tweeted = tweeted;
		this.groupID = groupID;
		this.groupName = DBConnector.getGroupTitle(groupID, userID);
	}

	/**
	 * Constructor for new tweets, which are not stored in the database yet.
	 * 
	 * @param tweetDate
	 * @param content
	 */
	public Tweet(String tweetDate, String content) {
		this.tweetDate = tweetDate;
		this.content = content;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Tweet t) {
		return this.tweetDate.compareTo(t.tweetDate);
	}
}
