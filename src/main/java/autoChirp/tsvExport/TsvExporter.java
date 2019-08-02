package autoChirp.tsvExport;

import autoChirp.tweetCreation.Tweet;

import java.util.List;

public class TsvExporter {

    public static String tweetsToTsv(List<Tweet> tweets) {
        StringBuffer buffer = new StringBuffer();

        for (Tweet tweet: tweets) {
            buffer.append(tweet.tweetDate.split("\\s+")[0]);
            buffer.append("\t");
            buffer.append(tweet.tweetDate.split("\\s+")[1]);
            buffer.append("\t");
            buffer.append(tweet.content);
            buffer.append("\t");
            buffer.append(tweet.imageUrl);
            buffer.append(System.getProperty("line.separator"));
        }

        return buffer.toString();
    }



}
