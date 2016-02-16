package autoChirp.tweeting;

import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import org.springframework.social.twitter.api.Twitter;
import org.springframework.social.twitter.api.impl.TwitterTemplate;

import autoChirp.DBConnector;
import autoChirp.tweetCreation.Tweet;



public class TwitterTask extends TimerTask {

	private int tweetID;
	private int user_id;

	public TwitterTask(int user_id, int tweetID) {
		this.user_id = user_id;
		this.tweetID = tweetID;
	}

	@Override
	public void run() {
		TwitterConnection tc = new TwitterConnection();
		System.out.println("tc.run...");
		tc.run(user_id, tweetID);
	}

}
