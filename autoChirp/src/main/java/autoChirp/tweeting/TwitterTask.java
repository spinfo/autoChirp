package autoChirp.tweeting;

import java.util.TimerTask;

/**
 * @author Alena Geduldig
 * 
 *         A subclass of java.util.TimerTask related to a single tweet and
 *         twitter-user. 
 *         - scheduled by a TweetScheduler timed by the tweets tweetDate
 *         - the overwritten run()- Method creates a new TwitterConnection which executes the 
 *         	twitter status-update. 
 * 
 */
public class TwitterTask extends TimerTask {

	private int tweetID;
	private int user_id;

	/**
	 * @param user_id
	 * @param tweetID
	 */
	public TwitterTask(int user_id, int tweetID) {
		this.user_id = user_id;
		this.tweetID = tweetID;
	}

	/* (non-Javadoc)
	 * @see java.util.TimerTask#run()
	 */
	@Override
	public void run() {
		TwitterConnection tc = new TwitterConnection();
		tc.run(user_id, tweetID);
	}

}
