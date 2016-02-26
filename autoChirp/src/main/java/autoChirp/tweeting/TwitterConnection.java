package autoChirp.tweeting;



import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.social.twitter.api.Twitter;
import org.springframework.social.twitter.api.impl.TwitterTemplate;

import autoChirp.DBConnector;
import autoChirp.tweetCreation.Tweet;
import autoChirp.tweetCreation.TweetGroup;

public class TwitterConnection {
	
	@Autowired
	private Environment env;
	
	
	String appId = "eX5ADra0ejYndEarZWfwGDf3L";
	String appSecret = "iVnE0TBMaPFjXik4AGdxuZ5kkrW0QfXFvA7Nrp3R8oDtEGUsmA";

	public void run(int user_id, int tweetID) {
		System.out.println("appSectret: " + appSecret);
		System.out.println("appID;: " + appId);
		Tweet toTweet = DBConnector.getTweetByID(tweetID);
		if(toTweet == null){
			return;
		}
		if(toTweet.tweeted){
			return;
		}
		//check if tweetGroup is still enabled
		if(!DBConnector.isEnabledGroup(toTweet.groupID)){
			return;
		}
		
		String[] userConfig = DBConnector.getUserConfig(user_id);
		//String[] appConfig= DBConnector.getAppConfig();
//		String consumerKey = appConfig[1];
//		String consumerSecret = appConfig[2];
		String token = userConfig[1];
		String tokenSecret = userConfig[2];
		//tweeting with spring-social
		Twitter twitter = new TwitterTemplate(env.getProperty("spring.social.twitter.appId"), env.getProperty("spring.social.twitter.appSecret"), token, tokenSecret);
		twitter.timelineOperations().updateStatus(toTweet.content);
		DBConnector.flagAsTweeted(tweetID, user_id);


		//tweetig with twitter4j
//		Twitter twitter = new TwitterFactory().getInstance();
//		twitter.setOAuthConsumer(consumerKey, consumerSecret);
//		twitter.setOAuthAccessToken(new AccessToken(token, tokenSecret));
//
//	    Status status;
//		try {
//			status = twitter.updateStatus(toTweet);
//			System.out.println("Successfully updated the status to [" + status.getText() + "].");
//		} catch (TwitterException e) {
//			e.printStackTrace();
//		}
	    
		
	}

}
