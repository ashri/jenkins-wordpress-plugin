package com.tearsofaunicorn.jenkins.plugins.notification.wordpress;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.scm.ChangeLogSet;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WordpressNotifier extends Notifier {

    private static final Logger LOGGER = Logger.getLogger(CampfireNotifier.class.getName());
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    private WordPressClient client;
    private Category category;
    private Set<Tag> tags;
    private boolean smartNotify;

    public WordpressNotifier() {
        super();
        initialize();
    }

    public WordpressNotifier(String endpointUrl, String username, String password, boolean smartNotify) {
        super();
        initialize(endpointUrl, username, password, category, tags, smartNotify);
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
                           BuildListener listener) throws InterruptedException, IOException {
        // If SmartNotify is enabled, only notify if:
        //  (1) there was no previous build, or
        //  (2) the current build did not succeed, or
        //  (3) the previous build failed and the current build succeeded.
        boolean publish = true;
        if (smartNotify) {
            AbstractBuild previousBuild = build.getPreviousBuild();
            publish =  previousBuild == null
                    || build.getResult() != Result.SUCCESS
                    || previousBuild.getResult() != Result.SUCCESS;
        }
        if (publish) {
            publish(build);
        }
        return true;
    }

    private void publish(AbstractBuild<?, ?> build) throws IOException {
        checkCampfireConnection();
        String message = new MessageBuilder(build, jenkinsUrl).build();
        Post newPost = buildPost();
        wordpressClient.newPost(newPost);
    }

    private void checkWordpressConnection() {
        if (wordpress == null) {
            initialize();
        }
    }

    private void initialize()  {
        initialize(DESCRIPTOR.getEndpointUrl(), DESCRIPTOR.getUsername(), DESCRIPTOR.getPassword(), DESCRIPTOR.getCategory(), DESCRIPTOR.getTags(), DESCRIPTOR.getSmartNotify());
    }

    private void initialize(String endpointUrl, String username, String password, String category, String tags, boolean smartNotify) {
        this.wordpressClient = new WordpressClient(endpointUrl, username, password);
        if (category != null && !category.isEmpty()) {
            this.category = new Category(category);
        }
        this.tags = initializeTags(tags);
        this.smartNotify = smartNotify;
    }

    private Set<Tag> initializeTags(String tagsString) {
        if (tagsString == null || tagsString.isEmpty()) {
            return null;
        }
        Set<Tag> tags = new TreeSet<Tag>();
        for (String tag : tagsString.split(",")) {
            tags.add(new Tag(tag));
        }
        return tags;
    }

}
