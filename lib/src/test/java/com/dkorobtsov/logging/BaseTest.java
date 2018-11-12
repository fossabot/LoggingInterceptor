package com.dkorobtsov.logging;

import static org.junit.Assert.fail;

import com.dkorobtsov.logging.interceptors.ApacheHttpRequestInterceptor;
import com.dkorobtsov.logging.interceptors.ApacheHttpResponseInterceptor;
import com.dkorobtsov.logging.interceptors.Okhttp3LoggingInterceptor;
import com.dkorobtsov.logging.interceptors.OkhttpLoggingInterceptor;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import java.io.IOException;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.BeforeClass;
import org.junit.Rule;

public abstract class BaseTest {

    private static final ConnectionPool connectionPool = new ConnectionPool();
    private static final Dispatcher dispatcher = new Dispatcher();
    private static final int MAX_IDLE_CONNECTIONS = 10;
    private static final int KEEP_ALIVE_DURATION_MS = 60 * 1000;
    private final org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager
        .getLogger(Log4j2LoggerTest.class);

    @Rule
    public MockWebServer server = new MockWebServer();

    @BeforeClass
    public static void cleanAnyExistingJavaUtilityLoggingConfigurations() {
        LogManager.getLogManager().reset();
        Logger globalLogger = Logger.getLogger(java.util.logging.Logger.GLOBAL_LOGGER_NAME);
        globalLogger.setLevel(java.util.logging.Level.OFF);
    }

    /**
     * Returns default OkHttp request for use in tests.
     */
    Request defaultOkhttp3Request() {
        return new Request.Builder()
            .url(String.valueOf(server.url("/")))
            .build();
    }

    HttpUriRequest defaultApacheHttpRequest() {
        return new HttpGet(server.url("/").uri());
    }

    com.squareup.okhttp.Request defaultOkhttpRequest() {
        return new com.squareup.okhttp.Request.Builder()
            .url(String.valueOf(server.url("/")))
            .build();
    }

    /**
     * Returns OkHttpClient for all interceptor tests to use as a starting point.
     *
     * <p>The shared instance allows all tests to share a single connection pool, which prevents
     * idle connections from consuming unnecessary resources while connections wait to be evicted.
     */
    OkHttpClient defaultOkhttp3ClientWithInterceptor(Interceptor interceptor) {
        return new OkHttpClient.Builder()
            .connectionPool(connectionPool)
            .dispatcher(dispatcher)
            .addNetworkInterceptor(interceptor)
            .build();
    }

    HttpClient defaultApacheClientWithInterceptors(ApacheHttpRequestInterceptor requestInterceptor,
        ApacheHttpResponseInterceptor responseInterceptor) {
        return HttpClientBuilder
            .create()
            .addInterceptorFirst(requestInterceptor)
            .addInterceptorFirst(responseInterceptor)
            .setMaxConnTotal(MAX_IDLE_CONNECTIONS)
            .setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy())
            .build();
    }

    com.squareup.okhttp.OkHttpClient defaultOkhttpClientWithInterceptor(
        com.squareup.okhttp.Interceptor interceptor) {
        final com.squareup.okhttp.OkHttpClient okHttpClient = new com.squareup.okhttp.OkHttpClient()
            .setConnectionPool(
                new com.squareup.okhttp.ConnectionPool(MAX_IDLE_CONNECTIONS,
                    KEEP_ALIVE_DURATION_MS))
            .setDispatcher(new com.squareup.okhttp.Dispatcher());
        okHttpClient.interceptors().add(interceptor);
        return okHttpClient;
    }

    void attachLoggerToInterceptorWithDefaultRequest(String interceptorVersion,
        LogWriter log4j2Writer) throws IOException {
        if (interceptorVersion.equals(InterceptorVersion.OKHTTP3.getName())) {
            attachLoggerToOkttp3InterceptorWithDefaultRequest(log4j2Writer);
        } else if (interceptorVersion.equals(InterceptorVersion.OKHTTP.getName())) {
            attachLoggerToOkhttpInterceptorWithDefaultRequest(log4j2Writer);
        } else if (interceptorVersion
            .equals(InterceptorVersion.APACHE_HTTPCLIENT_REQUEST.getName())) {
            attachLoggerToApacheRequestInterceptorWithDefaultRequest(log4j2Writer);
        } else {
            fail(String.format(
                "I couldn't recognize %s interceptor version. I currently support okhttp and okhttp3 versions",
                interceptorVersion));
        }
    }

    void attachLoggerToInterceptor(String interceptorVersion, LogWriter log4j2Writer,
        Request okhttp3Request, com.squareup.okhttp.Request okhttpRequest,
        HttpUriRequest apacheHttpRequest) throws IOException {
        if (interceptorVersion.equals(InterceptorVersion.OKHTTP3.getName())) {
            attachLoggerToOkhttp3Interceptor(log4j2Writer, okhttp3Request);
        } else if (interceptorVersion.equals(InterceptorVersion.OKHTTP.getName())) {
            attachLoggerToOkHttpInterceptor(log4j2Writer, okhttpRequest);
        } else if (interceptorVersion
            .equals(InterceptorVersion.APACHE_HTTPCLIENT_REQUEST.getName())) {
            attachLoggerToApacheRequestInterceptor(log4j2Writer, apacheHttpRequest);
        } else {
            fail(String.format(
                "I couldn't recognize %s interceptor version. I currently support okhttp and okhttp3 versions",
                interceptorVersion));
        }
    }

    private void attachLoggerToOkhttpInterceptorWithDefaultRequest(LogWriter logWriter)
        throws IOException {
        attachLoggerToOkHttpInterceptor(logWriter, defaultOkhttpRequest());
    }

    private void attachLoggerToOkHttpInterceptor(LogWriter logWriter,
        com.squareup.okhttp.Request request) throws IOException {
        OkhttpLoggingInterceptor interceptor = new LoggingInterceptor.Builder()
            .logger(logWriter)
            .buildForOkhttp();
        defaultOkhttpClientWithInterceptor(interceptor)
            .newCall(request)
            .execute();
    }

    private void attachLoggerToOkttp3InterceptorWithDefaultRequest(LogWriter logWriter)
        throws IOException {
        attachLoggerToOkhttp3Interceptor(logWriter, defaultOkhttp3Request());
    }

    private void attachLoggerToOkhttp3Interceptor(LogWriter logWriter, Request request)
        throws IOException {
        Okhttp3LoggingInterceptor interceptor = new LoggingInterceptor.Builder()
            .logger(logWriter)
            .buildForOkhttp3();

        log.debug("Sending request.");
        defaultOkhttp3ClientWithInterceptor(interceptor)
            .newCall(request)
            .execute();
    }

    private void attachLoggerToApacheRequestInterceptorWithDefaultRequest(LogWriter logWriter)
        throws IOException {
        attachLoggerToApacheRequestInterceptor(logWriter, defaultApacheHttpRequest());
    }

    private void attachLoggerToApacheRequestInterceptor(LogWriter logWriter, HttpUriRequest request)
        throws IOException {
        ApacheHttpRequestInterceptor requestInterceptor = new LoggingInterceptor.Builder()
            .logger(logWriter)
            .buildForApacheHttpClientRequest();

        final ApacheHttpResponseInterceptor responseInterceptor = new LoggingInterceptor.Builder()
            .logger(logWriter)
            .builFordApacheHttpClientResponse();
        log.debug("Sending request.");
        defaultApacheClientWithInterceptors(requestInterceptor, responseInterceptor)
            .execute(request);
    }


}
