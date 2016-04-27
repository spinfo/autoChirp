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

/**
 * This class executes the actual twitter status-update using
 * Spring Social Twitter API
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
	 * updates the users twitter-status to the tweets content. 1. reads the tweet
	 * with the given tweetID from the database 2. checks if the related
	 * tweetGroup is still enabled and tweet wasn't tweeted already 3. reads the
	 * users oAuthToken and oAuthTokenSecret from the database 4. updates the
	 * users twitter status to the tweets tweetContent
	 * 
	 * @param userID
	 * 		userID
	 * @param tweetID
	 * tweetID
	 */
	public void run(int userID, int tweetID) {
		
		//read tweet from DB
		Tweet toTweet = DBConnector.getTweetByID(tweetID, userID);
		if (toTweet == null) {
			return;
		}
		
		// check if tweet was not tweeted already
		if (toTweet.tweeted) {
			return;
		}
		// check if tweetGroup is still enabled
		if (!DBConnector.isEnabledGroup(toTweet.groupID, userID)) {
			return;
		}
		
		// read userConfig (oAuthToken, tokenSecret) from DB
		String[] userConfig = DBConnector.getUserConfig(userID);
		String token = userConfig[1];
		String tokenSecret = userConfig[2];
		
		// Tweeting
		Twitter twitter = new TwitterTemplate(appID, appSecret, token, tokenSecret);
		
		//Add TweetContent
		String tweet = toTweet.content;
		TweetData tweetData = new TweetData(tweet);
		
		//add image
		if(toTweet.imageUrl != null){
			try {
				Resource img = new UrlResource(toTweet.imageUrl);
				tweetData = tweetData.withMedia(img);
			} catch (MalformedURLException e) {
				tweetData = new TweetData(tweet+" "+toTweet.imageUrl);
			}
			
		}
		
		//add Geo-Locations
		if(toTweet.longitude != 0 || toTweet.latitude != 0){
			System.out.println("long: " + toTweet.longitude);
			System.out.println("lat: " + toTweet.latitude);
			tweetData = tweetData.atLocation(toTweet.longitude, toTweet.latitude).displayCoordinates(true);
		}

		//update Status
		twitter.timelineOperations().updateStatus(tweetData);
		
		//update Tweet-Status in DB
		DBConnector.flagAsTweeted(tweetID, userID);	
	}

}
