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

@SpringBootApplication
public class Application {

@Value("${dbFilePath}")
private String dbFilePath;

@Value("${createDatabaseFile}")
private String createDatabaseFile;

public static void main(String[] args) throws IOException {
        ApplicationContext ctx = SpringApplication.run(Application.class, args);
}

@PostConstruct
private void connectDatabase(){
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
                        TweetScheduler.scheduleGroup(group.tweets, userID);
                }
        }
}

}
