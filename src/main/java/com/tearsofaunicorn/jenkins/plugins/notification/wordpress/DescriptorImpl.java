package com.tearsofaunicorn.jenkins.plugins.notification.wordpress;

import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DescriptorImpl extends BuildStepDescriptor<Publisher> {

    private static final Logger LOGGER = Logger.getLogger(DescriptorImpl.class.getName());

    private boolean enabled = true;
    private String endpointUrl;
    private String username;
    private String password;
    private String category;
    private String tags;
    private boolean smartNotify;

    public DescriptorImpl() {
        super(WordpressNotifier.class);
        load();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getCategory() {
        return category;
    }

    public String getTags() {
        return tags;
    }

    public boolean getSmartNotify() {
        return smartNotify;
    }

    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
        return true;
    }

    @Override
    public Publisher newInstance(StaplerRequest req, JSONObject json) throws FormException {
        String endpointUrl = json.optString("endpointUrl");
        if (endpointUrl == null || endpointUrl.isEmpty()) {
            endpointUrl = this.endpointUrl;
        }
        String username = json.optString("username");
        if (username == null || username.isEmpty()) {
            username = this.username;
        }
        String password = json.optString("password");
        if (password == null || password.isEmpty()) {
            password = this.password;
        }
        String category = json.getString("category");
        if (category == null || category.isEmpty()) {
            category = this.category;
        }
        String tags = json.getString("tags");
        if (tags == null || tags.isEmpty()) {
            tags = this.tags;
        }
        boolean smartNotify = json.getBoolean("smartNotify");

        try {

            return new WordpressNotifier(endpointUrl, username, password, category, tags, smartNotify);

        } catch (Exception e) {
            String message = "Failed to initialize WordPress Notifier - check your WordPress Notifier configuration settings: " + e.getMessage();
            LOGGER.log(Level.WARNING, message, e);
            throw new FormException(message, e, "");
        }
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        this.endpointUrl = json.getString("endpointUrl");
        this.username = json.getString("username");
        this.password = json.getString("password");
        this.category = json.getString("category");
        this.tags = json.getString("tags");
        this.smartNotify = json.getBoolean("smartNotify");

        try {
            new WordpressNotifier(endpointUrl, username, password, category, tags, smartNotify);

        } catch (Exception e) {
            String message = "Failed to initialize wordpress notifier - check your global wordpress notifier configuration settings: " + e.getMessage();
            LOGGER.log(Level.WARNING, message, e);
            throw new FormException(message, e, "");
        }
        save();
        return super.configure(req, json);
    }

    @Override
    public String getDisplayName() {
        return "WordPress Notification";
    }

    public FormValidation doTestConnection(@QueryParameter String endpointUrl, @QueryParameter String username, @QueryParameter String password) {
        String errorMessage = "Wordpress connection failed";
        try {
            boolean connected = new WordpressNotifier(endpointUrl, username, password, null, null, false).ping();
            if (connected) {
                return FormValidation.ok("Wordpress connection OK");
            }
            return FormValidation.error(errorMessage);
        } catch (Exception e) {
            return FormValidation.error(e, errorMessage);
        }
    }

}
