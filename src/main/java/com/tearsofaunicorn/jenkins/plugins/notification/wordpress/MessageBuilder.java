package com.tearsofaunicorn.jenkins.plugins.notification.wordpress;

import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.scm.ChangeLogSet;
import jenkins.model.Jenkins;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Build the message to be used as the WordPress post content
 */
public class MessageBuilder {

    private static final Logger LOGGER = Logger.getLogger(MessageBuilder.class.getName());

    private final AbstractBuild<?, ?> build;
    private Result result;

    public MessageBuilder(AbstractBuild<?, ?> build) {
        this.build = build;
        this.result = build.getResult();
    }

    public MessageBuilder withResult(Result result) {
        this.result = result;
        return this;
    }

    public String buildTitle() {
        return getSimpleMessage();
    }

    public String buildContent() {
        return getMessage();
    }

    private String getSimpleMessage() {
        String resultString = result.toString();
        return build.getProject().getName() + " " + build.getDisplayName() + ": " + resultString;
    }

    private String getMessage() {
        String resultString = result.toString();
        //if (!smartNotify && result == Result.SUCCESS) resultString = resultString.toLowerCase();
        String message = build.getProject().getName() + " " + build.getDisplayName() + " \"" + getChangeString() + "\": " + resultString;
        //if (smartNotify || result != Result.SUCCESS)) {
            message = message + " (" + Jenkins.getInstance().getRootUrl() + build.getUrl() + ")";
        //}
        return message;
    }

    private String getChangeString() {
        String changeString = "No changes";
        if (!build.hasChangeSetComputed()) {
            changeString = "Changes not determined";
        } else if (build.getChangeSet().iterator().hasNext()) {
            ChangeLogSet changeSet = build.getChangeSet();
            ChangeLogSet.Entry entry = build.getChangeSet().iterator().next();
            // note: iterator should return recent changes first, but GitChangeSetList currently reverses the log entries
            if (changeSet.getClass().getSimpleName().equals("GitChangeSetList")) {
                String exceptionLogMsg = "Workaround to obtain latest commit info from git plugin failed";
                try {
                    // find the sha for the first commit in the changelog file, and then grab the corresponding entry from the changeset, yikes!
                    String changeLogPath = build.getRootDir().toString() + File.separator + "changelog.xml";
                    String sha = getCommitHash(changeLogPath);
                    if (!"".equals(sha)) {
                        Method getIdMethod = entry.getClass().getDeclaredMethod("getId");
                        for(ChangeLogSet.Entry nextEntry : build.getChangeSet()) {
                            if ( ( (String)getIdMethod.invoke(entry) ).compareTo(sha) != 0 ) entry = nextEntry;
                        }
                    }
                } catch ( IOException e ){
                    LOGGER.log(Level.WARNING, exceptionLogMsg, e);
                } catch ( NoSuchMethodException e ) {
                    LOGGER.log(Level.WARNING, exceptionLogMsg, e);
                } catch ( IllegalAccessException e ) {
                    LOGGER.log(Level.WARNING, exceptionLogMsg, e);
                } catch ( SecurityException e ) {
                    LOGGER.log(Level.WARNING, exceptionLogMsg, e);
                } catch ( Exception e ) {
                    throw new RuntimeException(e.getClass().getName() + ": " + e.getMessage(), e);
                }
            }
            String commitMsg = entry.getMsg().trim();
            if (!"".equals(commitMsg)) {
                if (commitMsg.length() > 47) {
                    commitMsg = commitMsg.substring(0, 46)  + "...";
                }
                changeString = commitMsg + " - " + entry.getAuthor().toString();
            }
        }
        return changeString;
    }

    private String getCommitHash(String changeLogPath) throws IOException {
        String sha = "";
        BufferedReader reader = new BufferedReader(new FileReader(changeLogPath));
        String line;
        while((line = reader.readLine()) != null) {
            if (line.matches("^commit [a-zA-Z0-9]+$")) {
                sha = line.replace("commit ", "");
                break;
            }
        }
        reader.close();
        return sha;
    }
}
