package autoChirp.tweetCreation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Alena Geduldig
 * 
 * represents a group of related tweets (e.g. generated from one wikipedia-articel or an excel-file).
 * consists of a list of tweets, title, description, groupID and the status-attribute enabled , which indicates if the 
 * group is active (= its tweets are scheduled and will be tweeted)
 *
 */
public class TweetGroup {
	public List<Tweet> tweets = new ArrayList<Tweet>();
	public String title;
	public String description;
	public boolean enabled;
	public int groupID;


	/**
	 * Constructor for new TweetGroup-objects (not read from the database)
	 * @param title
	 * @param description
	 */
	public TweetGroup(String title, String description) {
		this.title = title;
		this.description = description;
	}

	/**
	 * Constructor for TweetGroups read from the database
	 * @param groupID
	 * @param title
	 * @param description
	 * @param enabled
	 */
	public TweetGroup(int groupID, String title, String description, boolean enabled){
    this.groupID = groupID;
		this.title = title;
		this.description = description;
		this.enabled = enabled;
	}

	/**
	 * adds a list of tweets to the group
	 * @param tweets
	 */
	public void setTweets(List<Tweet> tweets){
		this.tweets.addAll(tweets);
		Collections.sort(this.tweets);
	}

	/**
	 * adds a single tweet to the group
	 * @param tweet
	 */
	public void addTweet(Tweet tweet){
		tweets.add(tweet);
		Collections.sort(tweets);
	}

}
