package uk.gov.pay.connector.app;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;

public class CardExecutorServiceConfig extends Configuration {

    private int threadsPerCpu;
    private int timeoutInSeconds;

    public int getThreadsPerCpu() {
        return threadsPerCpu;
    }

    public int getTimeoutInSeconds() {
        return timeoutInSeconds;
    }
}