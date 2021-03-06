package com.dkorobtsov.logging;

import static org.junit.Assert.assertTrue;

import com.dkorobtsov.logging.interceptors.Okhttp3LoggingInterceptor;
import java.io.IOException;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnitParamsRunner.class)
public class OutputResizingTest extends BaseTest {

    private static final String TEST_JSON = "{name: \"John\", age: 31, city: \"New York\"}";

    @Test
    @Parameters({
        "okhttp, false",
        "okhttp3, false",
        "apacheHttpclientRequest, false"
    })
    public void printerOutputResizingValidation(String loggerVersion,
        boolean provideExecutor) throws IOException {
        final List<String> loggerOutput = interceptedRequest(RequestBody
                .create(MediaType.parse("application/json"), TEST_JSON),
            10, loggerVersion, provideExecutor, false);

        assertTrue("Interceptor should be able to log simple json body.",
            loggerOutput.contains("Method: @P"));
    }

    @Test(expected = IllegalArgumentException.class)
    @Parameters({
        "9", "501"
    })
    public void invalidOutputLengthHandling(String maxLineLength) {
        LoggingInterceptor.builder()
            .maxLineLength(Integer.parseInt(maxLineLength))
            .buildForOkhttp3();
    }

    @Test
    @Parameters({
        "10", "500"
    })
    public void validOutputLengthHandling(String maxLineLength) {
        Okhttp3LoggingInterceptor interceptor = LoggingInterceptor.builder()
            .maxLineLength(Integer.parseInt(maxLineLength))
            .buildForOkhttp3();

        Assert.assertEquals(Integer.parseInt(maxLineLength),
            interceptor.loggerConfig().maxLineLength);
    }

}
