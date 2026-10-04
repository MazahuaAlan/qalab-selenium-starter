package dev.morewater.qalab.pages.bank;

import dev.morewater.qalab.pages.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** Comprobante de transferencia (/bank/receipt/?folio=...). */
public class BankReceiptPage extends BasePage {
    public BankReceiptPage(WebDriver driver) { super(driver); }

    public BankReceiptPage waitLoaded() { visible("bank-receipt"); return this; }
    public String title() { return text("bank-receipt-title"); }
    public String folio() { return text("bank-receipt-folio"); }
    public String amount() { return text("bank-receipt-amount"); }
    public String fee() { return text("bank-receipt-fee"); }
    /** Valor de una fila de la tabla del comprobante por su encabezado (Fecha, Beneficiario, Banco, CLABE, Concepto...). */
    public String row(String header) {
        return visible("bank-receipt").findElement(By.xpath(".//th[text()='" + header + "']/following-sibling::td")).getText().trim();
    }
    public void backHome() { click("bank-receipt-home"); }
    public void toMovements() { click("bank-receipt-movements"); }
}
