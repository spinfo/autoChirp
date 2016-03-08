package autoChirp.tweeting;

import java.util.TimerTask;

import org.springframework.social.twitter.api.Twitter;
import org.springframework.social.twitter.api.impl.TwitterTemplate;

import autoChirp.DBConnector;
import autoChirp.tweetCreation.Tweet;

/**
 * @author Alena Geduldig
 * 
 *         A subclass of java.util.TimerTask related to a single tweet and
 *         twitter-user. Its overwritten run()-method updates the users
 *         twitter-status to the tweets content and update its status in the database to tweeted = true
 *         Uses org.springframework.social.twitter
 *         
 */
public class TwitterTask extends TimerTask {

	private int tweetID;
	private int userID;
	private String appID = "eX5ADra0ejYndEarZWfwGDf3L";
	private String appSecret = "iVnE0TBMaPFjXik4AGdxuZ5kkrW0QfXFvA7Nrp3R8oDtEGUsmA";

	/**
	 * @param user_id
	 * @param tweetID
	 */
	public TwitterTask(int user_id, int tweetID) {
		this.userID = user_id;
		this.tweetID = tweetID;
	}

	/* (non-Javadoc)
	 * @see java.util.TimerTask#run()
	 */
	@Override
	public void run() {
		Tweet toTweet = DBConnector.getTweetByID(tweetID, userID);
		if (toTweet == null) {
			return;
		}
		if (toTweet.tweeted) {
			// already tweeted
			return;
		}
		// check if tweetGroup is still flagged enabled
		if (!DBConnector.isEnabledGroup(toTweet.groupID, userID)) {
			return;
		}
		// read users token and secret from database
		String[] userConfig = DBConnector.getUserConfig(userID);
		String token = userConfig[1];
		String tokenSecret = userConfig[2];

		// tweeting with org.springframework.social.twitter
		Twitter twitter = new TwitterTemplate(appID, appSecret, token, tokenSecret);
		twitter.timelineOperations().updateStatus(toTweet.content);
		// update tweet-status to tweeted = true
		DBConnector.flagAsTweeted(tweetID, userID);

		// tweetig with twitter4j
		// Twitter twitter = new TwitterFactory().getInstance();
		// twitter.setOAuthConsumer(consumerKey, consumerSecret);
		// twitter.setOAuthAccessToken(new AccessToken(token, tokenSecret));
		//
		// Status status;
		// try {
		// status = twitter.updateStatus(toTweet);
		// System.out.println("Successfully updated the status to [" +
		// status.getText() + "].");
		// } catch (TwitterException e) {
		// e.printStackTrace();
		// }
	}

}
