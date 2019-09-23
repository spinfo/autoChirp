package autoChirp.tsvExport;

import autoChirp.tweetCreation.Tweet;

import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.SimpleFormatter;

public class TsvExporter {


    public static String tweetsToTsv(List<Tweet> tweets, Schedule schedule) {

        switch (schedule) {
            case ALL:
                return exportAll(tweets);
            case NEXT_YEAR:
                return nextYear(tweets);
            case ONLY_IN_THE_PAST:
                return onlyInThePast(tweets);
            case NEXT_POSSIBLE_DATE:
                return nextPossibleDate(tweets);
            case ONLY_IN_THE_FUTURE:
                return onlyInTheFuture(tweets);
            default:
                return "";


        }
    }

    private static String onlyInTheFuture(List<Tweet> tweets) {
        StringBuffer buffer = new StringBuffer();
        for (Tweet tweet : tweets) {
            if (isInTheFuture(tweet)) {
                appendTweet(tweet, buffer);
            }
        }
        return buffer.toString();
    }

    private static String nextPossibleDate(List<Tweet> tweets) {
        return null;
    }

    private static String onlyInThePast(List<Tweet> tweets) {
        return null;
    }

    private static String nextYear(List<Tweet> tweets) {
        StringBuffer buffer = new StringBuffer();
        for (Tweet tweet : tweets) {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date tweetDate = dateFormat.parse(tweet.tweetDate);
                tweetDate.setYear(new Date().getYear() + 1);
                tweet.tweetDate = dateFormat.format(tweetDate);
                appendTweet(tweet, buffer);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return buffer.toString();
    }


    private static String exportAll(List<Tweet> tweets) {
        StringBuffer buffer = new StringBuffer();
        for (Tweet tweet : tweets) {
            appendTweet(tweet, buffer);
        }
        return buffer.toString();
    }


    private static boolean isInTheFuture(Tweet tweet) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            Date tweetDate = dateFormat.parse(tweet.tweetDate);
            Date now = new Date();

            return now.before(tweetDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static void appendTweet(Tweet tweet, StringBuffer buffer) {
        buffer.append(tweet.tweetDate.split("\\s+")[0]);
        buffer.append("\t");
        buffer.append(tweet.tweetDate.split("\\s+")[1]);
        if(tweet.content != null){
            buffer.append("\t");
            buffer.append(tweet.content);
        }
        if(tweet.imageUrl != null){
            buffer.append("\t");
            buffer.append(tweet.imageUrl);
        }
        buffer.append(System.getProperty("line.separator"));
    }


}
