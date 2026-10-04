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
    @DisplayName("A_BANK_043_ComprobanteConTodosLosCampos")
    void A_BANK_043_ComprobanteConTodosLosCampos() {
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
    @DisplayName("A_BANK_044_ElDebitoCoincideConElComprobanteSinComision")
    void A_BANK_044_ElDebitoCoincideConElComprobanteSinComision() {
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
    @DisplayName("[bank.transfer_fee_mismatch] A_BANK_049_PersonaDescuadreSeDescuentaUnaComisionOcultaDe500")
    @Tag("bug")
    void A_BANK_049_PersonaDescuadreSeDescuentaUnaComisionOcultaDe500() {
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
    @DisplayName("A_BANK_060_TransferenciaPropiaAparecePrimeraEnMovimientosYPersiste")
    void A_BANK_060_TransferenciaPropiaAparecePrimeraEnMovimientosYPersiste() {
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
    @DisplayName("[bank.slow_transfer] A_BANK_040_PersonaLentoConfirmarLaTransferenciaTarda4S")
    void A_BANK_040_PersonaLentoConfirmarLaTransferenciaTarda4S() {
        BankTransferPage p = new BankTransferPage(driver).open().toConfirm("100", "Pago de prueba").sendCode();
        long ms = p.submitCorrectCodeMillis();
        assertThat(ms).as("ms hasta el comprobante").isLessThan(2_500);
    }

    @Test
    @Tag("obligatorio")
    @Tag("bug")
    @Tag("slow")
    @DisplayName("[bank.flaky_transfer] A_BANK_041_PersonaIntermitenteTransferenciaFallidaYaDebitoYElReintento")
    void A_BANK_041_PersonaIntermitenteTransferenciaFallidaYaDebitoYElReintento() {
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
    @DisplayName("[bank.otp_short] A_BANK_042_PersonaExpiraElCodigoSMSCaducaALos10S")
    void A_BANK_042_PersonaExpiraElCodigoSMSCaducaALos10S() {
        BankTransferPage p = new BankTransferPage(driver).open().toConfirm("100", "Pago de prueba").sendCode();
        long t0 = System.currentTimeMillis();
        new WebDriverWait(driver, Duration.ofSeconds(20)).until(d -> System.currentTimeMillis() - t0 > 11_000);
        String sms = p.smsTextFromState();
        assertThat(sms).contains("Vence en 2 minutos");
        p.submitCorrectCode();
        assertThat(new BankReceiptPage(driver).title()).isEqualTo("Transferencia enviada");
    }
}
