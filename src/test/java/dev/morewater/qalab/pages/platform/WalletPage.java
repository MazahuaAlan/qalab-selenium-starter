package dev.morewater.qalab.pages.platform;

import org.openqa.selenium.WebDriver;

public class WalletPage extends PlatformPage {
    public WalletPage(WebDriver driver) { super(driver); }

    public WalletPage openWallet() { open("/wallet/"); visible("wallet-balance"); return this; }
    public String balanceText() { return text("wallet-balance"); }
    public boolean hasBalance() { return present("wallet-balance"); }
    public boolean hasMovementsTable() { return present("movements"); }
    public String noMovementsText() { return text("no-movements"); }
}
