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

import com.hivemq.cli.commands.shell.ShellCommand;
import com.hivemq.cli.commands.shell.ShellContextCommand;
import com.hivemq.cli.utils.MqttUtils;
import com.hivemq.client.mqtt.MqttClientConfig;
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedContext;
import com.hivemq.client.mqtt.lifecycle.MqttClientDisconnectedListener;
import com.hivemq.client.mqtt.lifecycle.MqttDisconnectSource;
import org.jetbrains.annotations.NotNull;
import org.pmw.tinylog.Logger;
import org.pmw.tinylog.LoggingContext;

public class ContextClientDisconnectListener implements MqttClientDisconnectedListener {

    @Override
    public void onDisconnected(final @NotNull MqttClientDisconnectedContext context) {

        final String contextBefore = LoggingContext.get("identifier");

        LoggingContext.put("identifier", "CLIENT " + context.getClientConfig().getClientIdentifier().orElse(null));

        if (context.getSource() != MqttDisconnectSource.USER) {
            final Throwable cause = context.getCause();

            if (ShellCommand.VERBOSE) {
                Logger.trace(cause);
            }
            else if (ShellCommand.DEBUG) {
                Logger.debug(cause.getMessage());
            }

            // If the currently active shell client gets disconnected from the server prompt the user to enter
            if (contextEqualsShellContext(context)) {
                Logger.error(MqttUtils.getRootCause(cause).getMessage());
                ShellContextCommand.removeContext();
                ShellCommand.TERMINAL_WRITER.printf("Press ENTER to resume: ");
                ShellCommand.TERMINAL_WRITER.flush();
            }
        }
        else if (contextEqualsShellContext(context)){
            ShellContextCommand.removeContext();
        }

        MqttClientExecutor.getClientDataMap().remove(getKeyFromConfig(context.getClientConfig()));

        LoggingContext.put("identifier", contextBefore);
    }

    private String getKeyFromConfig(final @NotNull MqttClientConfig clientConfig) {
            return MqttUtils.buildKey(clientConfig.getClientIdentifier().get().toString(), clientConfig.getServerHost());
    }

    private boolean contextEqualsShellContext(final @NotNull MqttClientDisconnectedContext context) {
        final MqttClientConfig clientConfig = context.getClientConfig();
        final MqttClientConfig shellClientConfig = ShellContextCommand.contextClient.getConfig();

        return clientConfig.getClientIdentifier().equals(shellClientConfig.getClientIdentifier()) &&
                clientConfig.getServerHost().equals(shellClientConfig.getServerHost());
    }
}