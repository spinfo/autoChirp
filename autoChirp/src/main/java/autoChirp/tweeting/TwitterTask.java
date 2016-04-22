package autoChirp.tweeting;

import java.util.TimerTask;

/**
 * A subclass of java.util.TimerTask related to a single tweet and twitter-user.
 * Scheduled by a TweetScheduler timed by the tweets tweetDate. the overwritten
 * run()- Method creates a new TwitterConnection which executes the twitter
 * status-update.
 * 
 * @author Alena Geduldig
 * 
 */
public class TwitterTask extends TimerTask {

	private int tweetID;
	private int userID;

	/**
	 * @param userID
	 *            userID
	 * @param tweetID
	 *            tweetID
	 */
	public TwitterTask(int userID, int tweetID) {
		this.userID = userID;
		this.tweetID = tweetID;
	}

	/**
	 * creates a new TwitterConnection
	 * 
	 */
	@Override
	public void run() {
		TwitterConnection tc = new TwitterConnection();
		tc.run(userID, tweetID);
	}

}
