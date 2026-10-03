package dev.morewater.qalab.pages;

import org.openqa.selenium.WebDriver;

public class BankPage extends BasePage {
    public BankPage(WebDriver driver) { super(driver); }

    public long walletCents() { return cents(text("wallet-chip").replace("Saldo", "")); }

    /** Transferencia a un beneficiario guardado hasta el paso del código SMS. */
    public BankPage startTransfer(String amount, String concept) {
        go("/bank/transfer/");
        select("bank-beneficiary", "b1");
        type("bank-amount", amount);
        type("bank-concept", concept);
        click("bank-transfer-next");
        return this;
    }

    public BankPage amountError() { visible("bank-amount-error"); return this; }

    public String amountErrorText() { return text("bank-amount-error"); }

    public boolean reachedConfirmStep() { return wait.until(d -> present("bank-confirm") || present("bank-amount-error")) && present("bank-confirm"); }

    /** Envía el código y lo lee del estado de la página (también está en /bank/messages/). */
    public BankPage sendCodeAndEnter() {
        long before = calls();
        click("bank-send-code");
        waitForCallsAfter(before);
        visible("bank-otp-form");
        String code = js("return window.qalab.state().bank.otp.code");
        type("bank-otp", code);
        long b2 = calls();
        click("bank-otp-submit");
        waitForCallsAfter(b2);
        visible("bank-receipt-title");
        return this;
    }

    public String receiptFee() { return text("bank-receipt-fee"); }

    public BankPage openMovements() { go("/bank/movements/"); visible("bank-row-0"); return this; }

    public String debitHeader() { return text("bank-th-debit"); }

    public String creditHeader() { return text("bank-th-credit"); }
}
