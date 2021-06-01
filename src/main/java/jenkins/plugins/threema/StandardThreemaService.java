package jenkins.plugins.threema;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.ProxyConfiguration;
import hudson.model.Run;
import jenkins.model.Jenkins;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.ssl.SSLContexts;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StandardThreemaService implements ThreemaService {

    private static final Logger logger = Logger.getLogger(StandardThreemaService.class.getName());
    public static final String THREEMA_API_URL = "https://msgapi.threema.ch/send_simple";

    private final String credentialsId;
    private final String[] recipients;

    public StandardThreemaService(String credentialsId, String recipient) {
        super();
        this.credentialsId = credentialsId;
        this.recipients = recipient.split("[,;]+");
    }

    public static String createRegexFromGlob(String glob) {
        StringBuilder out = new StringBuilder("^");
        for (int i = 0; i < glob.length(); ++i) {
            final char c = glob.charAt(i);
            switch (c) {
                case '*':
                    out.append(".*");
                    break;
                case '?':
                    out.append('.');
                    break;
                case '.':
                    out.append("\\.");
                    break;
                case '\\':
                    out.append("\\\\");
                    break;
                default:
                    out.append(c);
            }
        }
        out.append('$');
        return out.toString();
    }

    private static ThreemaNotifier.DescriptorImpl getServiceDescriptor() {
        ThreemaNotifier.DescriptorImpl threemaNotifierDescriptor =
                Jenkins.get().getDescriptorByType(ThreemaNotifier.DescriptorImpl.class);

        // taken from other class, not really sure about the use case here :)
        if (threemaNotifierDescriptor == null) {
            logger.fine("Could not getThreemaNotifier descriptor by class, trying by ID..");
            threemaNotifierDescriptor = (ThreemaNotifier.DescriptorImpl) Jenkins.get().getDescriptor("threemaNotifier");//junit test fallback
        }
        return threemaNotifierDescriptor;
    }


    @Override
    public boolean publish(@NonNull Run<?, ?> run, String message) {
        boolean result = true;
        for (String recipient : recipients) {
            //String url = endpoint;
            URL url;
            try {
                url = new URL(THREEMA_API_URL);
                HttpHost httpHost = new HttpHost(url.getHost(), url.getPort(), url.getProtocol());
                HttpClientBuilder clientBuilder = HttpClients.custom();
                clientBuilder.setSSLContext(SSLContexts.createDefault());
                RequestConfig.Builder reqconfigconbuilder = RequestConfig.custom();
                reqconfigconbuilder.setConnectTimeout(10000);
                reqconfigconbuilder.setSocketTimeout(10000);

                ProxyConfiguration globalProxy = Jenkins.get().proxy;

                if (globalProxy != null && isProxyRequired(ProxyConfiguration.getNoProxyHostPatterns(globalProxy.getNoProxyHost()))) {
                    setupProxy(globalProxy, clientBuilder, reqconfigconbuilder);
                }

                RequestConfig config = reqconfigconbuilder.build();
                CloseableHttpClient client = clientBuilder.build();
                RequestBuilder requestBuilder = RequestBuilder.post(url.toURI());
                requestBuilder.setConfig(config);
                requestBuilder.setCharset(StandardCharsets.UTF_8);

                StandardUsernamePasswordCredentials credentials = CredentialsProvider.findCredentialById(
                        this.credentialsId,
                        StandardUsernamePasswordCredentials.class,
                        run,
                        Collections.emptyList()
                );

                if (credentials == null) {
                    logger.log(Level.SEVERE, String.format("Credentials not found: %s", this.credentialsId));
                    return false;
                }

                String text = getBuildStatusMessage(run);
                if (message != null) {
                    text += " " + message;
                }

                requestBuilder.addParameter("from", credentials.getUsername());
                requestBuilder.addParameter("to", recipient);
                requestBuilder.addParameter("text", text);
                requestBuilder.addParameter("secret", credentials.getPassword().getPlainText());
                CloseableHttpResponse execute = client.execute(httpHost, requestBuilder.build());
                int responseCode = execute.getStatusLine().getStatusCode();
                if (responseCode != HttpStatus.SC_OK) {
                    result = false;
                    logHttpErrorStatus(execute, responseCode, url);
                } else
                    logger.info("Status " + responseCode + ": to " + url.getHost() + " " + message);
            } catch (java.net.URISyntaxException | java.io.IOException e) {
                logger.log(Level.WARNING, "Error posting to Threema", e);
                result = false;
            }
        }
        return result;
    }

    @Override
    public boolean publish(@NonNull Run<?, ?> run) {
        return publish(run, null);
    }

    String getBuildStatusMessage(Run<?, ?> r) {
        MessageBuilder message = new MessageBuilder(r);
        message.appendStatusMessage();
        message.appendDuration();
        return message.toString();
    }


    private void logHttpErrorStatus(CloseableHttpResponse execute, int responseCode, URL hosturl) throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(execute.getEntity().getContent(), Charset.defaultCharset()))) {
            String collect = bufferedReader.lines().collect(Collectors.joining(" "));
            logger.log(Level.WARNING, "WARN Status " + responseCode + ": to " + hosturl.getHost() + ": " + collect);
        }
    }

    private RequestConfig.Builder setupProxy(ProxyConfiguration proxy, HttpClientBuilder clientBuilder, RequestConfig.Builder reqconfigconbuilder) throws MalformedURLException {
        HttpHost proxyHost = new HttpHost(proxy.name, proxy.port);
        DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxyHost);
        clientBuilder.setRoutePlanner(routePlanner);
        reqconfigconbuilder.setProxy(proxyHost);

        setupProxyAuth(proxy, clientBuilder, proxyHost);
        return reqconfigconbuilder;
    }

    private void setupProxyAuth(ProxyConfiguration proxy, HttpClientBuilder clientBuilder, HttpHost proxyHost) {
        String username = proxy.getUserName();
        String password = proxy.getPassword();
        // Consider it to be passed if username specified. Sufficient?
        if (username != null && !username.isEmpty()) {
            logger.info("Using proxy authentication (user=" + username + ")");
            BasicCredentialsProvider basicCredentialsProvider = new BasicCredentialsProvider();
            basicCredentialsProvider.setCredentials(
                    new org.apache.http.auth.AuthScope(proxyHost.getHostName(), proxy.port),
                    new org.apache.http.auth.UsernamePasswordCredentials(username, password));

            clientBuilder.setDefaultCredentialsProvider(basicCredentialsProvider);
        }
    }

    protected boolean isProxyRequired(List<Pattern> noProxyHosts) {
        try {
            URL url = new URL(THREEMA_API_URL);
            for (Pattern p : noProxyHosts) {
                if (p.matcher(url.getHost()).matches()) return false;
            }
        } catch (MalformedURLException e) {
            logger.log(
                    Level.WARNING,
                    "A malformed URL [" + THREEMA_API_URL + "] is defined as endpoint, please check your settings");
            // default behavior : proxy still activated
            return true;
        }
        return true;
    }

    @Deprecated
    protected boolean isProxyRequired(String... noProxyHost) {//
        if (noProxyHost == null)
            return false;
        List<String> lst = Arrays.asList(noProxyHost);
        List<Pattern> collect = lst.stream()
                .filter(Objects::nonNull)
                .map(StandardThreemaService::createRegexFromGlob)
                .map(Pattern::compile)
                .collect(Collectors.toList());
        return isProxyRequired(collect);
    }
}
