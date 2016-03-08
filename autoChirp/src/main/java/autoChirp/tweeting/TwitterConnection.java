package autoChirp.tweeting;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.social.twitter.api.Twitter;
import org.springframework.social.twitter.api.impl.TwitterTemplate;
import org.springframework.stereotype.Component;

import autoChirp.DBConnector;
import autoChirp.tweetCreation.Tweet;

/**
 * @author Alena Geduldig
 * 
 *         This class executes the actual twitter status-update using
 *         org.springframework.social.twitter
 *
 */
@Component
public class TwitterConnection {

	@Value("${spring.social.twitter.appId}")
	private String appIDProp;

	@Value("${spring.social.twitter.appSecret}")
	private String appSecretProp;

	private static String appID;
	private static String appSecret;

	/**
	 * get appToken and appSecret
	 */
	@PostConstruct
	public void initializeConnection() {
		this.appID = appIDProp;
		this.appSecret = appSecretProp;
	}

	/**
	 * updates the users twitter-status to the tweets content - reads the tweet
	 * with the given tweetID from the database - checks if the related
	 * tweetGroup is still enabled and tweet wasn't tweeted already - reads the
	 * users oAuthToken and oAuthTokenSecret from the database - updates the
	 * users twitter status to the tweets tweetContent
	 * 
	 * @param user_id
	 * @param tweetID
	 */
	public void run(int user_id, int tweetID) {
		Tweet toTweet = DBConnector.getTweetByID(tweetID, user_id);
		if (toTweet == null) {
			return;
		}
		//check if tweet was not tweeted already
		if (toTweet.tweeted) {
			return;
		}
		// check if tweetGroup is still enabled
		if (!DBConnector.isEnabledGroup(toTweet.groupID, user_id)) {
			return;
		}
		//read userConfig
		String[] userConfig = DBConnector.getUserConfig(user_id);
		String token = userConfig[1];
		String tokenSecret = userConfig[2];
		//tweeting with org.springframework.social.twitter
		Twitter twitter = new TwitterTemplate(appID, appSecret, token, tokenSecret);
		twitter.timelineOperations().updateStatus(toTweet.content);
		DBConnector.flagAsTweeted(tweetID, user_id);
	}

}
