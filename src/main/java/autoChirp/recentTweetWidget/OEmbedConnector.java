package autoChirp.recentTweetWidget;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OEmbedConnector {


    private final static Logger LOGGER = Logger.getLogger(OEmbedConnector.class.getName());

    /**
     *
     * Gives you a JSON object with all necessary components to build a tweet widget
     *
     * @param twitterID Twitter ID of the original poster
     * @param tweetID twitter intern statusID of the tweet
     * @return A oEmbed JSON Object of the requested tweet
     * @throws IOException
     */
    protected static JSONObject getWidgetJson(String twitterID , String tweetID) throws IOException {


        String urlString = "https://publish.twitter.com/oembed?url=https://twitter.com/" + twitterID + "/status/" + tweetID;
        LOGGER.log(Level.INFO, "Building widget with: " + urlString);


        URL url = new URL(urlString);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");


        LOGGER.log(Level.INFO , "Status: " + con.getResponseCode());

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        con.disconnect();

        return new JSONObject(content.toString());
    }



}
