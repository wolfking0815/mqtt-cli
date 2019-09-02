package com.hivemq.cli.commands.shell;

import com.hivemq.cli.commands.cli.ConnectCommand;
import com.hivemq.cli.mqtt.MqttClientExecutor;
import org.jetbrains.annotations.NotNull;
import picocli.CommandLine;

import javax.inject.Inject;

@CommandLine.Command(name = "con",
        aliases = "connect",
        description = "Connects an mqtt client",
        abbreviateSynopsis = true)

public class ShellConnectCommand extends ConnectCommand {

    @Inject
    public ShellConnectCommand(@NotNull final MqttClientExecutor mqttClientExecutor) {
        super(mqttClientExecutor);
    }

    @Override
    public void run() {
        super.run();
        ShellContextCommand.updateContext(client);
    }

    @Override
    public String toString() {
        return "ShellConnectCommand:: " + super.toString();
    }

    @Override
    public boolean isDebug() {
        return ShellCommand.isDebug();
    }

    @Override
    public boolean isVerbose() {
        return ShellCommand.isVerbose();
    }
}
