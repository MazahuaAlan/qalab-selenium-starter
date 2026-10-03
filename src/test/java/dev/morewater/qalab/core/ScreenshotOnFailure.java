package dev.morewater.qalab.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

/** Guarda una captura en target/screenshots cuando una prueba falla. El paso de reporte las sube al panel. */
public class ScreenshotOnFailure implements TestExecutionExceptionHandler {
    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        WebDriver driver = DriverFactory.current();
        if (driver instanceof TakesScreenshot shot) {
            try {
                Path dir = Path.of("target", "screenshots");
                Files.createDirectories(dir);
                String name = context.getRequiredTestClass().getSimpleName() + "-" + context.getRequiredTestMethod().getName();
                Files.write(dir.resolve(name.replaceAll("[^A-Za-z0-9_.-]", "_") + ".png"), shot.getScreenshotAs(OutputType.BYTES));
            } catch (IOException | RuntimeException ignored) {
                // la captura es un extra: nunca debe ocultar el fallo real
            }
        }
        throw throwable;
    }
}
