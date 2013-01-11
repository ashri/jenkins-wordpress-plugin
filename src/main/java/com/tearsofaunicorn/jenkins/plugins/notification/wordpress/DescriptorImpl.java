package com.tearsofaunicorn.jenkins.plugins.notification.wordpress;

import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.util.logging.Level;
import java.util.logging.Logger;

public class DescriptorImpl extends BuildStepDescriptor<Publisher> {

    private static final Logger LOGGER = Logger.getLogger(DescriptorImpl.class.getName());

    private boolean enabled = false;
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
    public Publisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
        String endpointUrl = req.getParameter("endpointUrl");
        if (endpointUrl == null || endpointUrl.isEmpty()) {
            endpointUrl = this.endpointUrl;
        }
        String username = req.getParameter("username");
        if (username == null || username.isEmpty()) {
            username = this.username;
        }
        String password = req.getParameter("password");
        if (password == null || password.isEmpty()) {
            password = this.password;
        }
        String category = req.getParameter("category");
        if (category == null || category.isEmpty()) {
            category = this.category;
        }
        String tags = req.getParameter("tags");
        if (tags == null || tags.isEmpty()) {
            tags = this.tags;
        }
        String smartNotify = req.getParameter("smartNotify");
        if ((smartNotify == null || smartNotify.isEmpty()) && this.smartNotify) {
            smartNotify = String.valueOf(this.smartNotify);
        }

        try {
            return new WordpressNotifier(endpointUrl, username, password, category, tags, smartNotify != null);

        } catch (Exception e) {
            String message = "Failed to initialize WordPress Notifier - check your WordPress Notifier configuration settings: " + e.getMessage();
            LOGGER.log(Level.WARNING, message, e);
            throw new FormException(message, e, "");
        }
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        this.endpointUrl = req.getParameter("endpointUrl");
        this.username = req.getParameter("username");
        this.password = req.getParameter("password");
        this.category = req.getParameter("category");
        this.tags = req.getParameter("tags");
        this.smartNotify = req.getParameter("smartNotify") != null;

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

}
