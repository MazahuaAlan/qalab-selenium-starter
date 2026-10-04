package dev.morewater.qalab.pages.bank;

import dev.morewater.qalab.pages.BasePage;
import java.util.List;
import java.util.stream.Collectors;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

/** Flujo de transferencia SPEI (/bank/transfer/): Datos → Confirmar → Código. */
public class BankTransferPage extends BasePage {
    public BankTransferPage(WebDriver driver) { super(driver); }

    public BankTransferPage open() { go("/bank/transfer/"); visible("bank-transfer-form"); return this; }

    public BankTransferPage reload() { driver.navigate().refresh(); return this; }

    /** Espera a que la página (hidratada) muestre alguno de los pasos. */
    public BankTransferPage waitAnyStep() {
        wait.until(d -> present("bank-transfer-form") || present("bank-confirm") || present("bank-otp-form"));
        return this;
    }

    public boolean onForm() { return present("bank-transfer-form"); }
    public boolean onConfirm() { return present("bank-confirm"); }
    public boolean onOtp() { return present("bank-otp-form"); }

    public BankTransferPage chooseBeneficiary(String value) { select("bank-beneficiary", value); return this; }

    public List<String> beneficiaryOptions() {
        return new Select(visible("bank-beneficiary")).getOptions().stream().map(o -> o.getText().trim()).collect(Collectors.toList());
    }

    public BankTransferPage fill(String amount, String concept) {
        setValue("bank-amount", amount);
        setValue("bank-concept", concept);
        return this;
    }

    /** type() hace clear(), que en inputs controlados por React es fiable; si el valor es vacío solo limpia. */
    public void setValue(String dt, String text) {
        Fields.replace(visible(dt), text);
    }

    public String value(String dt) { return visible(dt).getAttribute("value"); }

    public BankTransferPage next() { click("bank-transfer-next"); return this; }

    /** Datos → Confirmar con beneficiario ya elegido. */
    public BankTransferPage toConfirm(String amount, String concept) {
        fill(amount, concept).next();
        visible("bank-confirm");
        return this;
    }

    public String amountError() { return text("bank-amount-error"); }
    public String conceptError() { return text("bank-concept-error"); }
    public boolean hasAmountError() { return present("bank-amount-error"); }
    public boolean hasConceptError() { return present("bank-concept-error"); }

    public String confirmName() { return text("bank-confirm-name"); }
    public String confirmClabe() { return text("bank-confirm-clabe"); }
    public String confirmAmount() { return text("bank-confirm-amount"); }
    public String confirmFee() { return text("bank-confirm-fee"); }
    public String confirmConcept() { return text("bank-confirm-concept"); }
    public String confirmBank() { return visible("bank-confirm").findElement(By.xpath(".//th[text()='Banco']/following-sibling::td")).getText().trim(); }
    public String stepperCurrent() { return visible("bank-steps").findElement(By.cssSelector("[aria-current='step']")).getText().trim(); }

    public BankTransferPage edit() { click("bank-edit"); visible("bank-transfer-form"); return this; }

    /** Pulsa Enviar código y espera el formulario del código. */
    public BankTransferPage sendCode() {
        long before = calls();
        click("bank-send-code");
        waitForCallsAfter(before);
        visible("bank-otp-form");
        return this;
    }

    /** Código vigente leído del estado de la página (equivale al SMS de /bank/messages/). */
    public String currentCode() { return js("return window.qalab.state().bank.otp && window.qalab.state().bank.otp.code"); }

    public BankTransferPage enterCode(String code) { setValue("bank-otp", code); return this; }

    /** Envía el código y espera a que termine la petición (éxito o error). */
    public BankTransferPage submitCode() {
        long before = calls();
        click("bank-otp-submit");
        waitForCallsAfter(before);
        return this;
    }

    public BankTransferPage submitCorrectCode() {
        enterCode(currentCode());
        submitCode();
        visible("bank-receipt-title");
        return this;
    }

    /** Transferencia completa a Ana Torres hasta el comprobante. */
    public BankTransferPage complete(String amount, String concept) {
        open().toConfirm(amount, concept).sendCode().submitCorrectCode();
        return this;
    }

    public String otpError() { return text("bank-otp-error"); }
    public boolean hasOtpError() { return present("bank-otp-error"); }
    public boolean resendEnabled() { return visible("bank-resend").isEnabled(); }
    public String resendText() { return text("bank-resend"); }

    public long walletCents() { return cents(text("wallet-chip").replace("Saldo", "")); }

    public String amountHint() { return visible("bank-amount").findElement(By.xpath("following-sibling::span[contains(@class,'hint')]")).getText(); }

    /** Elige «Nuevo beneficiario…» y captura nombre y CLABE. */
    public BankTransferPage newBeneficiary(String name, String clabe) {
        chooseBeneficiary("new");
        visible("bank-new-beneficiary");
        setValue("bank-ben-name", name);
        setValue("bank-ben-clabe", clabe);
        return this;
    }
    public String clabeExample() { return text("bank-clabe-example"); }
    public String clabeError() { return text("bank-ben-clabe-error"); }
    public boolean hasClabeError() { return present("bank-ben-clabe-error"); }
    public boolean saveChecked() { return visible("bank-ben-save").isSelected(); }
    public BankTransferPage toggleSave() { click("bank-ben-save"); return this; }
    public String smsText(int i) { return text("sms-text-" + i); }

    public Number jsExpiresInMs() { return js("return window.qalab.state().bank.otp.expiresAt - Date.now()"); }
    /** Adelanta el vencimiento del código vigente (simula el paso de 2 minutos sin esperarlos). */
    public BankTransferPage expireCodeNow() {
        js("const k='qalab.state.v1'; const s=JSON.parse(localStorage.getItem(k)); s.bank.otp.expiresAt=Date.now()-1000; localStorage.setItem(k, JSON.stringify(s));");
        reload().waitAnyStep();
        visible("bank-otp-form");
        return this;
    }
    public long callsNow() { return calls(); }
    public Number attempts() { return js("return window.qalab.state().bank.otp.attempts"); }
    public BankTransferPage clickResend(long callsBefore) { click("bank-resend"); waitForCallsAfter(callsBefore); return this; }

    /** Escribe el código vigente, pulsa verificar y devuelve los ms hasta ver el comprobante. */
    public long submitCorrectCodeMillis() {
        enterCode(currentCode());
        long t0 = System.nanoTime();
        click("bank-otp-submit");
        visible("bank-receipt-title");
        return (System.nanoTime() - t0) / 1_000_000;
    }

    /** true si tras verificar apareció el comprobante; false si apareció el error del servicio. */
    public boolean submitCodeSucceeded() {
        enterCode(currentCode());
        long before = calls();
        click("bank-otp-submit");
        wait.until(d -> present("bank-receipt-title") || calls() > before);
        wait.until(d -> present("bank-receipt-title") || present("bank-otp-error"));
        return present("bank-receipt-title");
    }

    public boolean otpErrorVisible() { return present("bank-otp-error"); }

    public String smsTextFromState() { return js("return window.qalab.state().bank.sms[0].text"); }

    public BankTransferPage setWallet(long cents) { js("window.qalab.setWallet(arguments[0])", cents); return this; }

    public long smsCount() { return ((Number) js("return window.qalab.state().bank.sms.length")).longValue(); }
}
