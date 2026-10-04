package dev.morewater.qalab.pages.stay;

import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/** /wallet/ — saldo y movimientos de la billetera. */
public class WalletPage extends StayBase {
    public WalletPage(WebDriver driver) { super(driver); }

    public WalletPage open() { go("/wallet/"); visible("wallet-balance"); return this; }
    public WalletPage openFromChip() { click("wallet-chip"); visible("wallet-balance"); return this; }
    public String balanceText() { return text("wallet-balance"); }
    public long balance() { return cents(balanceText()); }

    public List<WebElement> rows() { return driver.findElements(t("movement")); }
    public void waitRows() { wait.until(d -> !rows().isEmpty()); }
    private String cell(int row, int col) { return rows().get(row).findElements(By.tagName("td")).get(col).getText().trim(); }
    public String label(int row) { return cell(row, 1); }
    public String app(int row) { return cell(row, 2); }
    public String amount(int row) { return cell(row, 3); }
}
