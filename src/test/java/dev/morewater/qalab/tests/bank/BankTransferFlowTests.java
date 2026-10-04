package dev.morewater.qalab.tests.bank;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.core.BaseTest;
import dev.morewater.qalab.pages.bank.BankMovementsPage;
import dev.morewater.qalab.pages.bank.BankReceiptPage;
import dev.morewater.qalab.pages.bank.BankTransferPage;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.support.ui.WebDriverWait;

/** HU: ejecutar la transferencia, comprobante, débito exacto y rendimiento/estabilidad del servicio. */
class BankTransferFlowTests extends BaseTest {
    private static final long WALLET = 6_600_000L;

    @Test
    @Tag("obligatorio")
    @DisplayName("Comprobante con todos los campos (CP-BANK-043)")
    void receiptHasAllFields() {
        BankTransferPage p = new BankTransferPage(driver).complete("1500.50", "Renta de octubre");
        BankReceiptPage r = new BankReceiptPage(driver).waitLoaded();
        assertThat(r.title()).isEqualTo("Transferencia enviada");
        assertThat(r.folio()).matches("TR[0-9A-Z]+");
        String today = (String) ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("return new Date().toLocaleDateString('es-MX')");
        assertThat(r.row("Fecha")).startsWith(today);
        assertThat(r.row("Beneficiario")).isEqualTo("Ana Torres");
        assertThat(r.row("Banco")).isEqualTo("Banco Nimbo");
        assertThat(r.row("CLABE")).isEqualTo("901 180 00001234567 5");
        assertThat(r.row("Concepto")).isEqualTo("Renta de octubre");
        assertThat(r.amount()).isEqualTo("$1,500.50");
        assertThat(r.fee()).isEqualTo("$0.00");
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("El débito coincide con el comprobante (sin comisión) (CP-BANK-044)")
    void debitMatchesReceipt() {
        BankTransferPage p = new BankTransferPage(driver).complete("1500.50", "Renta de octubre");
        BankReceiptPage r = new BankReceiptPage(driver).waitLoaded();
        String folio = r.folio();
        assertThat(p.walletCents()).isEqualTo(WALLET - 150_050);
        r.toMovements();
        BankMovementsPage m = new BankMovementsPage(driver).waitTable();
        assertThat(m.concept(0)).isEqualTo("Transferencia a Ana Torres · " + folio);
        assertThat(m.signedCents(0)).isEqualTo(-150_050);
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("[bank.transfer_fee_mismatch] Persona descuadre: se descuenta una comisión oculta de $5.00 (CP-BANK-049)")
    @Tag("bug")
    void noHiddenFee() {
        BankTransferPage p = new BankTransferPage(driver).complete("200", "Pago de prueba");
        BankReceiptPage r = new BankReceiptPage(driver).waitLoaded();
        assertThat(r.amount()).isEqualTo("$200.00");
        assertThat(r.fee()).isEqualTo("$0.00");
        assertThat(p.walletCents()).as("saldo tras transferir $200").isEqualTo(WALLET - 20_000);
        BankMovementsPage m = new BankMovementsPage(driver).open();
        assertThat(m.signedCents(0)).as("cargo en Movimientos").isEqualTo(-20_000);
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("Transferencia propia aparece primera en Movimientos y persiste (CP-BANK-060)")
    void ownTransferIsFirstAndPersists() {
        new BankTransferPage(driver).complete("100", "Pago de prueba");
        BankMovementsPage m = new BankMovementsPage(driver).open();
        long total = totalRows(m);
        assertThat(m.concept(0)).startsWith("Transferencia a Ana Torres");
        assertThat(m.rowApp(0)).isEqualTo("Bank");
        assertThat(m.signedCents(0)).isEqualTo(-10_000);
        assertThat(m.pageInfo()).contains("de 46");
        driver.navigate().refresh();
        m.waitTable();
        assertThat(m.concept(0)).startsWith("Transferencia a Ana Torres");
        assertThat(totalRows(m)).isEqualTo(total);
    }

    private long totalRows(BankMovementsPage m) {
        String info = m.pageInfo();
        return Long.parseLong(info.substring(info.lastIndexOf(' ') + 1));
    }

    @Test
    @Tag("recomendado")
    @Tag("bug")
    @DisplayName("[bank.slow_transfer] Persona lento: confirmar la transferencia tarda ~4 s (CP-BANK-040)")
    void confirmIsFast() {
        BankTransferPage p = new BankTransferPage(driver).open().toConfirm("100", "Pago de prueba").sendCode();
        long ms = p.submitCorrectCodeMillis();
        assertThat(ms).as("ms hasta el comprobante").isLessThan(2_500);
    }

    @Test
    @Tag("obligatorio")
    @Tag("bug")
    @Tag("slow")
    @DisplayName("[bank.flaky_transfer] Persona intermitente: transferencia fallida ya debitó y el reintento duplica el cargo (CP-BANK-041)")
    void failedTransferDoesNotChargeAndRetryChargesOnce() {
        BankTransferPage p = new BankTransferPage(driver).open();
        p.setWallet(2_000_000);
        long amount = 10_000;
        for (int round = 1; round <= 8; round++) {
            long before = p.open().walletCents();
            p.toConfirm("100", "Ronda " + round).sendCode();
            boolean ok = p.submitCodeSucceeded();
            int tries = 0;
            while (!ok && tries++ < 20) {
                assertThat(p.otpError()).contains("El servicio de transferencias no respondió");
                assertThat(p.walletCents()).as("ronda %d: un fallo no debe cobrar", round).isEqualTo(before);
                ok = p.submitCodeSucceeded();
            }
            assertThat(ok).isTrue();
            assertThat(p.walletCents()).as("ronda %d: un solo cargo tras reintentar", round).isEqualTo(before - amount);
        }
    }

    @Test
    @Tag("obligatorio")
    @Tag("bug")
    @Tag("slow")
    @DisplayName("[bank.otp_short] Persona expira: el código SMS caduca a los 10 s (CP-BANK-042)")
    void codeDoesNotExpireAfter10Seconds() {
        BankTransferPage p = new BankTransferPage(driver).open().toConfirm("100", "Pago de prueba").sendCode();
        long t0 = System.currentTimeMillis();
        new WebDriverWait(driver, Duration.ofSeconds(20)).until(d -> System.currentTimeMillis() - t0 > 11_000);
        String sms = p.smsTextFromState();
        assertThat(sms).contains("Vence en 2 minutos");
        p.submitCorrectCode();
        assertThat(new BankReceiptPage(driver).title()).isEqualTo("Transferencia enviada");
    }
}
