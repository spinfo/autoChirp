package autoChirp.tweeting;

import java.net.MalformedURLException;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.social.twitter.api.StatusDetails;
import org.springframework.social.twitter.api.TweetData;
import org.springframework.social.twitter.api.Twitter;
import org.springframework.social.twitter.api.impl.TwitterTemplate;
import org.springframework.stereotype.Component;

import autoChirp.DBConnector;
import autoChirp.tweetCreation.Tweet;
import autoChirp.tweetCreation.TweetFactory;

/**
 * This class executes the actual twitter status-update using Spring Social
 * Twitter API
 *
 * @author Alena Geduldig
 *
 */
@Component
public class TwitterConnection {

	@Value("${spring.social.twitter.appId}")
	private String appIDProp;

	@Value("${spring.social.twitter.appSecret}")
	private String appSecretProp;

	@Value("${autochirp.domain}")
	private String appDomainProp;

	@Value("${autochirp.parser.dateformats}")
	private String dateformatsProp;

	private static String appID;
	private static String appSecret;
	private static String appDomain;
	private static String dateformats;

	/**
	 * get appToken and appSecret
	 */
	@PostConstruct
	public void initializeConnection() {
		this.appID = appIDProp;
		this.appSecret = appSecretProp;
		this.appDomain = appDomainProp;
		this.dateformats = dateformatsProp;
	}

	/**
	 * updates the users twitter-status to the tweets content. 1. reads the
	 * tweet with the given tweetID from the database 2. checks if the related
	 * tweetGroup is still enabled and tweet wasn't tweeted already 3. reads the
	 * users oAuthToken and oAuthTokenSecret from the database 4. updates the
	 * users twitter status to the tweets tweetContent
	 *
	 * @param userID
	 *            userID
	 * @param tweetID
	 *            tweetID
	 */
	public void run(int userID, int tweetID) {
		// read tweet from DB
		Tweet toTweet = DBConnector.getTweetByID(tweetID, userID);

		// check if tweet exists
		if (toTweet == null) {
			return;
		}
		// check if tweetGroup is still enabled
		if (!DBConnector.isEnabledGroup(toTweet.groupID, userID)) {
			return;
		}
		// check if tweet was not tweeted already
		if (toTweet.tweeted) {
			return;
		}
		
		

		// read userConfig (oAuthToken, tokenSecret) from DB
		String[] userConfig = DBConnector.getUserConfig(userID);
		String token = userConfig[1];
		String tokenSecret = userConfig[2];

		// tweeting
		Twitter twitter = new TwitterTemplate(appID, appSecret, token, tokenSecret);

		// TweetData tweetData = new TweetData(toTweet.content);
		String tweet = toTweet.content;
		TweetData tweetData = new TweetData(tweet);
		
		//check if tweet is a reply
		long replyID = 0;
		if(DBConnector.isThreadedGroup(toTweet.groupID, userID)){
							System.out.println("is threaded Group");
			replyID = DBConnector.getReplyID(tweetID, toTweet.groupID, userID, DBConnector.getOrderID(tweetID, userID, toTweet.groupID));
							System.out.println("replyID: " + replyID);
		}

		// add image
		if (toTweet.imageUrl != null) {
			try {
				Resource img = new UrlResource(toTweet.imageUrl);
				tweetData = tweetData.withMedia(img);
			} catch (MalformedURLException e) {
				tweetData = new TweetData(tweet + " " + toTweet.imageUrl);
			}
		}

		// add flashcard
		if (toTweet.adjustedLength() > 140) {
			TweetFactory tf = new TweetFactory(dateformats);
			String flashcard = appDomain + "/flashcard/" + toTweet.tweetID;

			try {
				tweetData = new TweetData(tf.trimToTweet(tweet, null)).withMedia(new UrlResource(flashcard));
			} catch (MalformedURLException e) {
				tweetData = new TweetData(tf.trimToTweet(tweet + " " + flashcard, null));
			}
		}

		// add Geo-Locations
		if (toTweet.longitude != 0 || toTweet.latitude != 0) {
			System.out.println("long: " + toTweet.longitude);
			System.out.println("lat: " + toTweet.latitude);
			tweetData = tweetData.atLocation(toTweet.longitude, toTweet.latitude).displayCoordinates(true);
		}
		
	

		// update Tweet-Status in DB
		DBConnector.flagAsTweeted(tweetID, userID);

		// update Status
		org.springframework.social.twitter.api.Tweet statusUpdate;
		if(replyID > 0){
			statusUpdate = twitter.timelineOperations().updateStatus(tweetData.inReplyToStatus(replyID));
		}
		else{
			statusUpdate = twitter.timelineOperations().updateStatus(tweetData);
		}
		//write statusID in db
		System.out.println("status-ID: "+ statusUpdate.getId());
		DBConnector.addStatusID(tweetID, statusUpdate.getId());
	}

}
