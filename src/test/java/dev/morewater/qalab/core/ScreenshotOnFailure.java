package dev.morewater.qalab.core;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestWatcher;
import org.openqa.selenium.WebDriver;

/**
 * Extensión de evidencia: abre el bloque de la prueba, captura el estado final (o el fallo con su mensaje) y escribe el bloque
 * en el PDF del módulo (target/evidence). No escribe PNG: las imágenes viven en memoria. Ver docs/guides/05-evidencia-pdf.md.
 * (Conserva el nombre histórico porque BaseTest lo registra con @ExtendWith.)
 */
public class ScreenshotOnFailure implements BeforeEachCallback, AfterTestExecutionCallback, TestExecutionExceptionHandler,
        LifecycleMethodExecutionExceptionHandler, TestWatcher {

    @Override
    public void beforeEach(ExtensionContext c) {
        Evidence.begin(c.getDisplayName(), c.getRequiredTestClass());
    }

    @Override
    public void handleTestExecutionException(ExtensionContext c, Throwable t) throws Throwable {
        Evidence.fail(DriverFactory.raw(), t);
        throw t;
    }

    @Override
    public void handleBeforeEachMethodExecutionException(ExtensionContext c, Throwable t) throws Throwable {
        Evidence.fail(DriverFactory.raw(), t);
        throw t;
    }

    @Override
    public void afterTestExecution(ExtensionContext c) {
        if (c.getExecutionException().isEmpty()) Evidence.capture(DriverFactory.raw(), "Estado final", false, true);
    }

    @Override public void testSuccessful(ExtensionContext c) { Evidence.end(true); }

    @Override public void testFailed(ExtensionContext c, Throwable cause) { Evidence.end(false); }

    @Override public void testAborted(ExtensionContext c, Throwable cause) { Evidence.end(true); }
}
