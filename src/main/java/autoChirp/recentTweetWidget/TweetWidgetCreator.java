package autoChirp.recentTweetWidget;

import autoChirp.DBConnector;
import autoChirp.tweetCreation.Tweet;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public class TweetWidgetCreator {

    public static String getRecentTweetWidget() {


        /**TODO
         * Just a workaround until the DB gets fixed
         * Normal tweet URl: http://twitter/{twitterID}/status/{statusID}
         *
         * In this case used tweet URL: http://twitter/{ANYTHING}/status/{statusID}
         * -> Gets redirected to the first link structure atm.
         *
         * TwitterID is not set correctly. ID is to long to store in an INTEGER in user DB
         *
         */

        StringBuffer buffer = new StringBuffer();

        DBConnector.getLatestTweets().stream().forEach(tweet -> {
            try {
                buffer.append(OEmbedConnector.getWidgetJson("placeholder", String.valueOf(tweet.statusID)).get("html"));
            } catch (FileNotFoundException e) {
                System.err.println("Tweet with Status ID '" + tweet.statusID + "' not found");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        return buffer.toString();

    }


}
