package dev.morewater.qalab.pages.desk;

import dev.morewater.qalab.config.Config;
import org.openqa.selenium.WebDriver;

/** Acceso a rutas protegidas de Desk sin sesión previa. */
public class DeskAccessPage extends DeskBasePage {
    public DeskAccessPage(WebDriver driver) { super(driver); }

    /** Abre una ruta protegida sin sesión y espera la redirección al inicio de sesión. */
    public String openProtected(String path) {
        go(path);
        wait.until(d -> d.getCurrentUrl().contains("/id/"));
        visible("username");
        return driver.getCurrentUrl();
    }

    public void login(String user) {
        type("username", user);
        type("password", Config.password());
        click("login-button");
        visible("user-chip");
    }

    public boolean hasSession() { return present("user-chip"); }
}
