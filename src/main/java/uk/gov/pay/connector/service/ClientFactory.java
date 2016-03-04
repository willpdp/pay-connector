package uk.gov.pay.connector.service;

import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.client.proxy.ProxyConfiguration;
import io.dropwizard.setup.Environment;
import io.dropwizard.util.Duration;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientProperties;
import uk.gov.pay.connector.app.ConnectorConfiguration;

import javax.inject.Inject;
import javax.ws.rs.client.Client;

public class ClientFactory {
    private final Environment environment;
    private final ConnectorConfiguration conf;

    @Inject
    public ClientFactory(Environment environment, ConnectorConfiguration conf) {
        this.environment = environment;
        this.conf = conf;
    }

    public Client createWithDropwizardClient(String name) {
        JerseyClientConfiguration clientConfiguration = conf.getClientConfiguration();
        ApacheConnectorProvider connectorProvider = new ApacheConnectorProvider();

        Duration readTimeout = conf.getCustomJerseyClient().getReadTimeout();
        int readTimeoutInMillis = (int) (readTimeout.toMilliseconds());

        JerseyClientBuilder defaultClientBuilder = new JerseyClientBuilder(environment)
                .using(connectorProvider)
                .using(clientConfiguration)
                .withProperty(ClientProperties.READ_TIMEOUT, readTimeoutInMillis);

        // optionally set proxy; see comment below why this has to be done
        if (conf.getCustomJerseyClient().isProxyEnabled()) {
            defaultClientBuilder
                    .withProperty(ClientProperties.PROXY_URI, proxyUrl(clientConfiguration.getProxyConfiguration()));
        }

        return defaultClientBuilder.build(name);
    }

    /**
     * Constructs the proxy URL required by JerseyClient property ClientProperties.PROXY_URI
     * <p>
     * <b>NOTE:</b> The reason for doing this is, Dropwizard jersey client doesn't seem to work as per
     * http://www.dropwizard.io/0.9.2/docs/manual/configuration.html#proxy where just setting the proxy config in
     * client configuration is only needed. But after several test, that doesn't seem to work, but by setting the
     * native jersey proxy config as per this implementation seems to work
     * <p>
     * similar problem discussed in here -> https://groups.google.com/forum/#!topic/dropwizard-user/AbDSYfLB17M
     * </p>
     * </p>
     *
     * @param proxyConfig from config.yml
     * @return proxy server URL
     */
    private String proxyUrl(ProxyConfiguration proxyConfig) {
        return String.format("%s://%s:%s",
                proxyConfig.getScheme(),
                proxyConfig.getHost(),
                proxyConfig.getPort()
        );
    }

}

