package dev.morewater.qalab.pages;

import dev.morewater.qalab.config.Config;
import org.openqa.selenium.WebDriver;

public class LoginPage extends BasePage {
    public LoginPage(WebDriver driver) { super(driver); }

    public LoginPage open() { go("/id/"); return this; }

    public LoginPage loginAs(String user, String password) {
        type("username", user);
        type("password", password);
        click("login-button");
        return this;
    }

    /** Inicia sesión con QALAB_USER (por defecto «estandar») y espera a ver el chip de usuario. */
    public void loginAsConfiguredUser() {
        loginAs(Config.user(), Config.password());
        // Con el usuario «bloqueado» no habrá sesión: la prueba falla aquí, que es justo lo esperado.
        wait.until(d -> present("user-chip") || present("login-error"));
        if (!present("user-chip")) throw new AssertionError("No se pudo iniciar sesión como '" + Config.user() + "': " + text("login-error"));
    }

    public String errorMessage() { return text("login-error"); }
}
