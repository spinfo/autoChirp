package autoChirp.tweeting;


import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.social.twitter.api.Twitter;
import org.springframework.social.twitter.api.impl.TwitterTemplate;
import org.springframework.stereotype.Component;

import autoChirp.DBConnector;
import autoChirp.tweetCreation.Tweet;

@Component
public class TwitterConnection {

@Value("${spring.social.twitter.appId}")
private String appIDProp;

@Value("${spring.social.twitter.appSecret}")
private String appSecretProp;

private static String appID;
private static String appSecret;

@PostConstruct
public void initializeConnection() {
        this.appID = appIDProp;
        this.appSecret = appSecretProp;
}

public void run(int user_id, int tweetID) {
        Tweet toTweet = DBConnector.getTweetByID(tweetID, user_id);
        if(toTweet == null) {
                return;
        }
        if(toTweet.tweeted) {
                return;
        }
        //check if tweetGroup is still enabled
        if(!DBConnector.isEnabledGroup(toTweet.groupID, user_id)) {
                return;
        }

        String[] userConfig = DBConnector.getUserConfig(user_id);
        String token = userConfig[1];
        String tokenSecret = userConfig[2];

        Twitter twitter = new TwitterTemplate(appID, appSecret, token, tokenSecret);
        twitter.timelineOperations().updateStatus(toTweet.content);
        DBConnector.flagAsTweeted(tweetID, user_id);
}

}
