package com.dkorobtsov.logging.interceptors;

import static com.dkorobtsov.logging.ClientPrintingExecutor.printFileResponse;
import static com.dkorobtsov.logging.ClientPrintingExecutor.printJsonResponse;
import static com.dkorobtsov.logging.TextUtils.isFileRequest;

import com.dkorobtsov.logging.Level;
import com.dkorobtsov.logging.LoggerConfig;
import com.dkorobtsov.logging.ResponseDetails;
import java.io.IOException;
import java.util.Objects;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.protocol.HttpContext;

public class ApacheHttpResponseInterceptor implements HttpResponseInterceptor {

    private final boolean isDebug;
    private final LoggerConfig loggerConfig;

    public ApacheHttpResponseInterceptor(LoggerConfig loggerConfig) {
        this.loggerConfig = loggerConfig;
        this.isDebug = loggerConfig.isDebug;
    }

    @Override
    public void process(HttpResponse response, HttpContext context) throws IOException {
        if (isDebug && loggerConfig.level != Level.NONE) {
            String subtype = null;
            if (Objects.requireNonNull(response.getEntity()).getContentType() != null) {
                subtype = Objects.requireNonNull(response.getEntity().getContentType()).getValue();
            }

            ResponseDetails responseDetails = ResponseDetails
                .from(response, isFileRequest(subtype));

            if (isFileRequest(subtype)) {
                printFileResponse(responseDetails, loggerConfig);
            } else {
                printJsonResponse(responseDetails, loggerConfig);
            }
        }
    }

    public LoggerConfig loggerConfig() {
        return this.loggerConfig;
    }
}
