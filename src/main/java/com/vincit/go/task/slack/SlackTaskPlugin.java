/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.vincit.go.task.slack;

import com.google.gson.GsonBuilder;
import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.exceptions.UnhandledRequestTypeException;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.vincit.go.task.slack.model.*;
import com.vincit.go.task.slack.utils.MessageFormatter;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.vincit.go.task.slack.utils.FieldUtils.createField;
import static com.vincit.go.task.slack.utils.FileUtils.getFileContents;
import static com.vincit.go.task.slack.utils.JSONUtils.responseAsJson;

@Extension
public class SlackTaskPlugin implements GoPlugin {

    public static final String CHANNEL = "Channel";
    public static final String CHANNEL_TYPE = "ChannelType";
    public static final String MESSAGE = "Message";
    public static final String TITLE = "Title";
    public static final String ICON_OR_EMOJI = "IconOrEmoji";
    public static final String EXTENSION_ID = "task";
    public static final String WEBHOOK_URL = "WebhookUrl";
    public static final String DISPLAY_NAME = "DisplayName";
    public static final String COLOR = "Color";
    public static final String COLOR_TYPE = "ColorType";

    private Logger logger = Logger.getLoggerFor(SlackTaskPlugin.class);

    @Override
    public void initializeGoApplicationAccessor(GoApplicationAccessor goApplicationAccessor) {
    }

    @Override
    public GoPluginApiResponse handle(GoPluginApiRequest request) throws UnhandledRequestTypeException {
        logger.info("Handle received message of type: " + request.requestName());

        if ("configuration".equals(request.requestName())) {
            return handleGetConfigRequest();
        } else if ("validate".equals(request.requestName())) {
            return handleValidation(request);
        } else if ("execute".equals(request.requestName())) {
            return handleTaskExecution(request);
        } else if ("view".equals(request.requestName())) {
            return handleTaskView();
        }
        throw new UnhandledRequestTypeException(request.requestName());
    }

    private GoPluginApiResponse handleTaskView() {
        int responseCode = DefaultGoApiResponse.SUCCESS_RESPONSE_CODE;
        HashMap view = new HashMap();
        view.put("displayValue", "Slack");
        try {
            view.put("template", getFileContents("/views/task.template.html"));
        } catch (Exception e) {
            responseCode = DefaultGoApiResponse.INTERNAL_ERROR;
            String errorMessage = "Failed to find template: " + e.getMessage();
            view.put("exception", errorMessage);
            logger.error(errorMessage, e);
        }
        return responseAsJson(responseCode, view);
    }

    private GoPluginApiResponse handleTaskExecution(GoPluginApiRequest request) {
        logger.info("handleTaskExecution");

        TaskConfig executionRequest = new GsonBuilder().create().fromJson(request.requestBody(), TaskConfig.class);
        Config config = executionRequest.getConfig();
        Context context = executionRequest.getContext();

        try {
            String webhookUrl = config.getWebhookUrl();
            SlackExecutor executor = new SlackExecutor(webhookUrl);
            MessageFormatter messageFormatter = new MessageFormatter(context.getEnvironmentVariables());

            TaskSlackMessage message = new TaskSlackMessage(
                    messageFormatter.format(config.getTitle()),
                    messageFormatter.format(config.getMessage()),
                    config.getIconOrEmoji(),
                    config.getColor(),
                    messageFormatter.format(config.getDisplayName())
            );

            executor.sendMessage(config.getChannelType(), config.getChannel(), message);
        } catch (IOException e) {
            throw new RuntimeException("Could not send message to slack", e);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return responseAsJson(DefaultGoApiResponse.SUCCESS_RESPONSE_CODE, result);
    }

    private GoPluginApiResponse handleValidation(GoPluginApiRequest request) {
        HashMap validationResult = new HashMap();
        int responseCode = DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE;
        return responseAsJson(responseCode, validationResult);
    }

    private GoPluginApiResponse handleGetConfigRequest() {
        HashMap config = new HashMap();
        config.put(WEBHOOK_URL, createField("Webhook URL", "", 0, true));
        config.put(CHANNEL, createField("Channel", "", 1, true));
        config.put(CHANNEL_TYPE, createField("Channel Type", ChannelType.CHANNEL.name(), 2, true));
        config.put(TITLE, createField("Title", "", 3, false));
        config.put(ICON_OR_EMOJI, createField("Icon or Emoji", "", 4, false));
        config.put(MESSAGE, createField("Message", "", 5, false));
        config.put(DISPLAY_NAME, createField("Display Name", "", 6, false));
        config.put(COLOR_TYPE, createField("Color Type", ColorType.NONE.name(), 7, false));
        config.put(COLOR, createField("Color", "", 8, false));
        return responseAsJson(DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE, config);
    }

    @Override
    public GoPluginIdentifier pluginIdentifier() {
        return new GoPluginIdentifier(EXTENSION_ID, Arrays.asList("1.0"));
    }
}
