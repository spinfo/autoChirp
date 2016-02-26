package autoChirp.tweeting;



import org.springframework.social.twitter.api.Twitter;
import org.springframework.social.twitter.api.impl.TwitterTemplate;

import autoChirp.DBConnector;
import autoChirp.tweetCreation.Tweet;


public class TwitterConnection {
	

	private String appID = "eX5ADra0ejYndEarZWfwGDf3L";
	private String appSecret = "iVnE0TBMaPFjXik4AGdxuZ5kkrW0QfXFvA7Nrp3R8oDtEGUsmA";
	
	public void run(int user_id, int tweetID) {
		
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
		Twitter twitter = new TwitterTemplate(appID, appSecret, token, tokenSecret);
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
