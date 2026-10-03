package dev.morewater.qalab.core;

import dev.morewater.qalab.config.Config;
import java.net.MalformedURLException;
import java.net.URI;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

public final class DriverFactory {
    private static final ThreadLocal<WebDriver> CURRENT = new ThreadLocal<>();

    private DriverFactory() {}

    public static WebDriver create() {
        ChromeOptions options = new ChromeOptions();
        if (Config.headless()) options.addArguments("--headless=new");
        options.addArguments("--window-size=1280,900", "--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu");
        if (!Config.chromeBinary().isBlank()) options.setBinary(Config.chromeBinary());
        try {
            WebDriver driver;
            if (!Config.remoteUrl().isBlank()) {
                driver = new RemoteWebDriver(URI.create(Config.remoteUrl()).toURL(), options);
            } else if (!Config.chromedriverPath().isBlank()) {
                driver = new ChromeDriver(new ChromeDriverService.Builder().usingDriverExecutable(new java.io.File(Config.chromedriverPath())).build(), options);
            } else {
                driver = new ChromeDriver(options); // Selenium Manager descarga el chromedriver adecuado
            }
            CURRENT.set(driver);
            return driver;
        } catch (MalformedURLException e) {
            throw new IllegalStateException("SELENIUM_REMOTE_URL no es una URL válida: " + Config.remoteUrl(), e);
        }
    }

    public static WebDriver current() { return CURRENT.get(); }

    public static void quit() {
        WebDriver d = CURRENT.get();
        if (d != null) {
            try { d.quit(); } finally { CURRENT.remove(); }
        }
    }
}
