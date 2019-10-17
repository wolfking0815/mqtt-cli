/*
 * Copyright 2019 HiveMQ and the HiveMQ Community
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.hivemq.cli.mqtt;

import com.hivemq.cli.commands.Subscribe;
import com.hivemq.cli.utils.FileUtils;
import com.hivemq.cli.utils.SubscribeMqttPublishCallbackUtils;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;
import org.jetbrains.annotations.NotNull;
import org.pmw.tinylog.Logger;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class SubscribeMqtt3PublishCallback implements Consumer<Mqtt3Publish> {

    @NotNull private final Subscribe subscribe;

    public SubscribeMqtt3PublishCallback(final @NotNull Subscribe subscribe) {
        this.subscribe = subscribe;
    }

    @Override
    public void accept(final @NotNull Mqtt3Publish mqtt3Publish) {

        PrintWriter fileWriter = null;
        if (subscribe.getPublishFile() != null) {
            fileWriter = FileUtils.createFileAppender(subscribe.getPublishFile());
        }


        byte[] payload = mqtt3Publish.getPayloadAsBytes();
        final String payloadMessage = SubscribeMqttPublishCallbackUtils.applyBase64EncodingIfSet(subscribe.isBase64(), payload);

        if (fileWriter != null) {
            fileWriter.println(mqtt3Publish.getTopic() + ": " + payloadMessage);
            fileWriter.flush();
            fileWriter.close();
        }

        if (subscribe.isPrintToSTDOUT()) {
            System.out.println(payloadMessage);
        }

        if (subscribe.isVerbose()) {
            Logger.trace("received PUBLISH: {}, MESSAGE: '{}'", mqtt3Publish, new String(mqtt3Publish.getPayloadAsBytes(), StandardCharsets.UTF_8));
        }
        else if (subscribe.isDebug()) {
            Logger.debug("received PUBLISH: (Topic: '{}', MESSAGE: '{}')", mqtt3Publish.getTopic(), new String(mqtt3Publish.getPayloadAsBytes(), StandardCharsets.UTF_8));
        }

    }

}
