package com.hivemq.cli.mqtt.test.results;

import com.hivemq.cli.utils.Tuple;
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAckReturnCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

public class ClientIdLengthTestResults {
    private final int maxClientIdLength;
    private List<Tuple<@NotNull Integer, @Nullable Mqtt3ConnAckReturnCode>> testResults;

    public ClientIdLengthTestResults(final int maxClientIdLength, final @NotNull List<Tuple<Integer, Mqtt3ConnAckReturnCode>> testResults) {
        this.maxClientIdLength = maxClientIdLength;
        this.testResults = testResults;
    }

    public int getMaxClientIdLength() { return maxClientIdLength; }

    public @NotNull List<Tuple<@NotNull Integer, @Nullable Mqtt3ConnAckReturnCode>> getTestResults() { return testResults; }
}
