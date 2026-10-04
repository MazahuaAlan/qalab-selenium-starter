package dev.morewater.qalab.pages.platform;

import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/** /id/: formulario de entrada y lista de usuarios de prueba. */
public class IdPage extends PlatformPage {
    public static final String PASSWORD = "qalab123";
    public static final List<String> PERSONAS = List.of("estandar", "bloqueado", "lento", "intermitente", "visual", "amnesia", "expira", "descuadre");

    public IdPage(WebDriver driver) { super(driver); }

    public IdPage openId() { open("/id/"); visible("login-form"); return this; }
    public IdPage openId(String query) { open("/id/" + query); visible("login-form"); return this; }

    public IdPage fill(String user, String pass) { type("username", user); type("password", pass); return this; }
    public IdPage clickPersona(String id) { click("persona-" + id); return this; }
    public IdPage submit() { click("login-button"); return this; }
    public IdPage loginAs(String user, String pass) { fill(user, pass); return submit(); }

    /** Entra con la contraseña común y espera el chip de usuario. */
    public IdPage loginOk(String user) {
        loginAs(user, PASSWORD);
        wait.until(d -> hasUserChip() || present("login-error"));
        return this;
    }

    public String username() { return visible("username").getAttribute("value"); }
    public String password() { return visible("password").getAttribute("value"); }
    public String attr(String dataTest, String name) { return visible(dataTest).getDomAttribute(name); }
    public String buttonText() { return text("login-button"); }
    public boolean buttonEnabled() { return visible("login-button").isEnabled(); }
    public String error() { return text("login-error"); }
    public boolean hasError() { return present("login-error"); }
    public void waitError() { visible("login-error"); }
    public int personaButtons() { return visible("persona-list").findElements(By.cssSelector("button[data-test^='persona-']")).size(); }
    public String sharedPassword() { return text("shared-password"); }
    public boolean hasExpiredNotice() { return present("session-expired"); }
    public WebElement expiredNotice() { return visible("session-expired"); }
    public String visibleErrorRole() { return visible("login-error").getDomAttribute("role"); }
}
