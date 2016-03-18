package autoChirp;

import autoChirp.tweetCreation.TweetGroup;
import autoChirp.tweeting.TweetScheduler;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

/**
 * Default (SpringBoot-)Application class with main() method and minor
 * extensions: On start a persistent connection to the SQLite database is
 * opened and all relevant Tweets from that database are scheduled.
 *
 * @author Philip Schildkamp
 * @editor Alena Geduldig
 */
@SpringBootApplication
public class Application {

@Value("${dbFilePath}")
private String dbFilePath;

@Value("${createDatabaseFile}")
private String createDatabaseFile;

/**
 * @param args Command line arguments
 * @throws IOException IOException
 */
public static void main(String[] args) throws IOException {
        ApplicationContext ctx = SpringApplication.run(Application.class, args);
}

/**
 * Open database connection and schedule relevant Tweets.
 */
@PostConstruct
private void initializeApplication(){
        File file = new File(dbFilePath);

        if (!file.exists()) {
                DBConnector.connect(dbFilePath);
                DBConnector.createOutputTables(createDatabaseFile);
        } else {
                DBConnector.connect(dbFilePath);
        }

        Map<Integer,List<TweetGroup> > toSchedule = DBConnector.getAllEnabledGroups();
        for (int userID : toSchedule.keySet()) {
                for (TweetGroup group : toSchedule.get(userID)) {
                        TweetScheduler.scheduleTweetsForUser(group.tweets, userID);
                }
        }
}

}
