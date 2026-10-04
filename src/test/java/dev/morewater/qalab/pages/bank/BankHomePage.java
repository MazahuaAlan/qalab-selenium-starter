package dev.morewater.qalab.pages.bank;

import dev.morewater.qalab.pages.BasePage;
import java.util.List;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/** Resumen de cuentas de MoreBank (/bank/): saldos, CLABE, traspasos y últimos movimientos. */
public class BankHomePage extends BasePage {
    public BankHomePage(WebDriver driver) { super(driver); }

    public BankHomePage open() { go("/bank/"); visible("bank-balance-main"); return this; }

    public BankHomePage reload() { driver.navigate().refresh(); visible("bank-balance-main"); return this; }

    public long mainCents() { return cents(text("bank-balance-main")); }
    public long savingsCents() { return cents(text("bank-balance-savings")); }
    public long chipCents() { return cents(text("wallet-chip").replace("Saldo", "")); }
    public String balanceMainText() { return text("bank-balance-main"); }
    public String balanceSavingsText() { return text("bank-balance-savings"); }
    public String clabeText() { return text("bank-my-clabe"); }

    public BankHomePage move(boolean toSavings, String amount) {
        select("bank-move-dir", toSavings ? "toSavings" : "toMain");
        Fields.replace(visible("bank-move-amount"), amount);
        long before = calls();
        click("bank-move-submit");
        wait.until(d -> present("bank-move-msg") || calls() > before);
        return this;
    }

    /** Pulsa Traspasar y espera el mensaje (éxito o error). */
    public String submitMoveAndGetMessage(boolean toSavings, String amount) {
        select("bank-move-dir", toSavings ? "toSavings" : "toMain");
        WebElement in = visible("bank-move-amount");
        Fields.replace(in, amount);
        click("bank-move-submit");
        return text("bank-move-msg");
    }

    public boolean moveMsgIsError() { return visible("bank-move-msg").getAttribute("class").contains("error"); }

    public List<WebElement> recentItems() { return driver.findElements(t("bank-recent-item")); }
    public String recentText(int i) { return recentItems().get(i).getText().replace("\n", " | "); }
    public String recentConcept(int i) { return recentItems().get(i).findElement(org.openqa.selenium.By.cssSelector("span")).getText().trim(); }
    public long recentCents(int i) { return cents(recentItems().get(i).findElement(org.openqa.selenium.By.cssSelector("b")).getText()); }
    public String moveAmountValue() { return visible("bank-move-amount").getAttribute("value"); }
}
