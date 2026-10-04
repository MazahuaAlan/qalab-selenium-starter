package dev.morewater.qalab.tests.bank;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.core.BaseTest;
import dev.morewater.qalab.pages.bank.BankHomePage;
import dev.morewater.qalab.pages.bank.BankMovementsPage;
import dev.morewater.qalab.pages.bank.BankReceiptPage;
import dev.morewater.qalab.pages.bank.BankTransferPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WindowType;

/** HU: límite diario, consistencia de saldos y persistencia de datos. */
class BankLimitsAndBalanceTests extends BaseTest {
    private static final long WALLET = 6_600_000L;

    @Test
    @Tag("obligatorio")
    @DisplayName("A_BANK_088_LimiteDiarioAcumuladoEntreVariasTransferencias")
    @Tag("slow")
    void A_BANK_088_LimiteDiarioAcumuladoEntreVariasTransferencias() {
        BankTransferPage p = new BankTransferPage(driver).complete("6000", "Primera");
        p.open().fill("4000.01", "Segunda").next();
        assertThat(p.amountError()).isEqualTo("Superas el límite diario de $10,000.00 (llevas $6,000.00).");
        assertThat(p.onConfirm()).isFalse();
        p.setValue("bank-amount", "4000.00");
        p.next();
        p.sendCode().submitCorrectCode();
        assertThat(p.walletCents()).isEqualTo(WALLET - 1_000_000);
        p.open().fill("1.00", "Tercera").next();
        assertThat(p.amountError()).contains("llevas $10,000.00");
    }

    @Test
    @Tag("obligatorio")
    @Tag("bug")
    @DisplayName("[bank.daily_limit_ignored] A_BANK_093_PersonaDescuadreElLimiteDiarioDe10000NoSeAplica")
    void A_BANK_093_PersonaDescuadreElLimiteDiarioDe10000NoSeAplica() {
        BankTransferPage p = new BankTransferPage(driver).open().fill("10500", "Grande").next();
        assertThat(p.amountError()).isEqualTo("Superas el límite diario de $10,000.00 (llevas $0.00).");
        assertThat(p.onConfirm()).isFalse();
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_BANK_090_ElSaldoSeReflejaIgualEnElChipYEnElResumenTrasTransferir")
    void A_BANK_090_ElSaldoSeReflejaIgualEnElChipYEnElResumenTrasTransferir() {
        BankTransferPage p = new BankTransferPage(driver).complete("1500.50", "Renta de octubre");
        long expected = WALLET - 150_050;
        assertThat(p.walletCents()).isEqualTo(expected);
        BankHomePage h = new BankHomePage(driver).open();
        assertThat(h.mainCents()).isEqualTo(expected);
        assertThat(h.chipCents()).isEqualTo(expected);
        h.reload();
        assertThat(h.mainCents()).isEqualTo(expected);
        assertThat(h.chipCents()).isEqualTo(expected);
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_BANK_096_PersistenciaDeSaldosBeneficiariosYMovimientosAlRecargar")
    void A_BANK_096_PersistenciaDeSaldosBeneficiariosYMovimientosAlRecargar() {
        BankHomePage h = new BankHomePage(driver).open();
        assertThat(h.submitMoveAndGetMessage(true, "500")).contains("realizado");
        BankTransferPage p = new BankTransferPage(driver).open().newBeneficiary("Carla Núñez", "901180000099999992").fill("100", "Prueba");
        p.next();
        p.sendCode().submitCorrectCode();
        assertThat(new BankReceiptPage(driver).title()).isEqualTo("Transferencia enviada");
        // «Cerrar y reabrir la pestaña»: nueva pestaña en la misma sesión de navegador (el almacenamiento local persiste).
        String old = driver.getWindowHandle();
        driver.switchTo().newWindow(WindowType.TAB);
        driver.switchTo().window(old).close();
        driver.switchTo().window(driver.getWindowHandles().iterator().next());
        BankHomePage h2 = new BankHomePage(driver).open();
        assertThat(h2.mainCents()).isEqualTo(WALLET - 50_000 - 10_000);
        assertThat(h2.savingsCents()).isEqualTo(500_000 + 50_000);
        assertThat(new BankTransferPage(driver).open().beneficiaryOptions()).contains("Carla Núñez · Banco Nimbo ···9992");
        BankMovementsPage m = new BankMovementsPage(driver).open();
        assertThat(m.concept(0)).startsWith("Transferencia a Carla Núñez");
        assertThat(m.concept(1)).isEqualTo("Traspaso a cuenta de ahorro");
        assertThat(m.pageInfo()).contains("de 47");
    }
}
