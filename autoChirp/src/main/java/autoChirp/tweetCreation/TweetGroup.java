package autoChirp.tweetCreation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TweetGroup {
	public List<Tweet> tweets = new ArrayList<Tweet>();
	public String title;
	public String description;
	public boolean enabled;
	public int groupID;


	public TweetGroup(String title, String description) {
		this.title = title;
		this.description = description;
	}

	public TweetGroup(int groupID, String title, String description, boolean enabled){
    this.groupID = groupID;
		this.title = title;
		this.description = description;
		this.enabled = enabled;
	}

	public void setTweets(List<Tweet> tweets){
		this.tweets.addAll(tweets);
		Collections.sort(this.tweets);
	}

	public void addTweet(Tweet tweet){
		tweets.add(tweet);
		Collections.sort(tweets);
	}

}
