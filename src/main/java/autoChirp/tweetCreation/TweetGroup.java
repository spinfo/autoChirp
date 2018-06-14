package autoChirp.tweetCreation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * represents a group of related tweets (e.g. generated from one
 * wikipedia-articel or a tsv-file). consists of a list of tweets, a title and 
 * description. If read from DB also consisting of groupID and the status-attribute enabled , which indicates if
 * the group is active (= it's tweets are scheduled and prepared for tweeting) or
 * not.
 * 
 * @author Alena Geduldig
 *
 */
public class TweetGroup {
	public List<Tweet> tweets = new ArrayList<Tweet>();
	public String title;
	public String description;
	public boolean enabled;
	public int groupID;
	public boolean threaded;
	public String flashcard = "default.jpg";

	/**
	 * Constructor for new TweetGroup-objects (not read from the database)
	 * 
	 * @param title
	 *            title of this group
	 * @param description
	 *            description of this group
	 */
	public TweetGroup(String title, String description) {
		this.title = title;
		this.description = description;
	}

	/**
	 * Constructor for TweetGroups read from the database. In contrast to new
	 * TweetGoups, TweetGroups read from DB already have a groupID and  status
	 * attributes.
	 * 
	 * @param groupID
	 *            db-key
	 * @param title
	 *            title of this group
	 * @param description
	 *            description of this group
	 * @param enabled
	 *            group is active (= tweets are prepared for tweeting)
	 */
	public TweetGroup(int groupID, String title, String description, boolean enabled, boolean threaded) {
		this.groupID = groupID;
		this.title = title;
		this.description = description;
		this.enabled = enabled;
		this.threaded = threaded;
	}

	/**
	 * adds a list of tweets to the group
	 * 
	 * @param tweets
	 *            list of tweets
	 */
	public void setTweets(List<Tweet> tweets) {
		this.tweets.addAll(tweets);
		Collections.sort(this.tweets);
	}
	
	/**
	 * @param threaded
	 */
	public void setThreaded(boolean threaded){
		this.threaded = threaded;
	}

	/**
	 * adds a single tweet to the group
	 * 
	 * @param tweet
	 *            tweet to add
	 */
	public void addTweet(Tweet tweet) {
		tweets.add(tweet);
		Collections.sort(tweets);
	}
	
	public void setFlashCard(String flashcard){
		this.flashcard = flashcard;
	}

}
