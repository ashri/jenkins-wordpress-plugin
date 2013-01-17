package com.tearsofaunicorn.jenkins.plugins.notification.wordpress;

import com.tearsofaunicorn.wordpress.api.WordpressClient;
import com.tearsofaunicorn.wordpress.api.WordpressClientConfig;
import com.tearsofaunicorn.wordpress.api.model.Category;
import com.tearsofaunicorn.wordpress.api.model.Post;
import com.tearsofaunicorn.wordpress.api.model.Tag;
import com.tearsofaunicorn.wordpress.api.transport.XmlRpcBridge;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import java.util.TreeSet;

public class WordpressNotifier extends Notifier {

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    private WordpressClient client;
    private Category category;
    private Set<Tag> tags;
    private boolean smartNotify;

    public WordpressNotifier() {
        super();
        initialize();
    }

    public WordpressNotifier(String endpointUrl, String username, String password, String category, String tags, boolean smartNotify) {
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
            publish = previousBuild == null
                    || build.getResult() != Result.SUCCESS
                    || previousBuild.getResult() != Result.SUCCESS;
        }
        if (publish) {
            publish(build);
        }
        return true;
    }

    private void initialize() {
        initialize(DESCRIPTOR.getEndpointUrl(), DESCRIPTOR.getUsername(), DESCRIPTOR.getPassword(), DESCRIPTOR.getCategory(), DESCRIPTOR.getTags(), DESCRIPTOR.getSmartNotify());
    }

    private void initialize(String endpointUrl, String username, String password, String category, String tags, boolean smartNotify) {
        this.client = buildClient(username, password, endpointUrl);

        if (category != null && !category.isEmpty()) {
            this.category = new Category(category);
        }
        this.tags = initializeTags(tags);
        this.smartNotify = smartNotify;
    }

    private WordpressClient buildClient(String username, String password, String endpointUrl) {
        URL url = convertEndpointUrl(endpointUrl);
        WordpressClientConfig config = new WordpressClientConfig(username, password, url);
        XmlRpcBridge bridge = new XmlRpcBridge(config);
        return new WordpressClient(bridge);
    }

    private URL convertEndpointUrl(String endpointUrl) {
        try {
            return new URL(endpointUrl);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("URL for endpoint failed to validate", e);
        }
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

    private void publish(AbstractBuild<?, ?> build) throws IOException {
        checkWordpressConnection();
        MessageBuilder messageBuilder = new MessageBuilder(build);
        Post newPost = buildPost(messageBuilder);
        client.newPost(newPost);
    }

    private void checkWordpressConnection() {
        if (this.client == null) {
            initialize();
        }
    }

    private Post buildPost(MessageBuilder messageBuilder) {
        String title = messageBuilder.buildTitle();
        String content = messageBuilder.buildContent();
        Post post = new Post(title, content);
        if (this.category != null) {
            post.setCategory(this.category);
        }
        if (this.tags != null) {
            for (Tag tag : this.tags) {
                post.addTag(tag);
            }
        }
        return post;
    }

}
