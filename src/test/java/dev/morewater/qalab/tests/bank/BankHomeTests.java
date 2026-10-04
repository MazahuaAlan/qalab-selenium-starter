package dev.morewater.qalab.tests.bank;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.core.BaseTest;
import dev.morewater.qalab.pages.bank.BankHomePage;
import dev.morewater.qalab.pages.bank.BankMovementsPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** HU: resumen de cuentas y traspasos entre cuentas propias. */
class BankHomeTests extends BaseTest {
    private static final long WALLET = 6_600_000L;
    private static final long SAVINGS = 500_000L;
    private static final String MY_CLABE = "900 180 00987654321 7";

    @Test
    @Tag("obligatorio")
    @DisplayName("A_BANK_001_ResumenMuestraSaldosInicialesYCLABE")
    void A_BANK_001_ResumenMuestraSaldosInicialesYCLABE() {
        BankHomePage h = new BankHomePage(driver).open();
        assertThat(h.balanceMainText()).isEqualTo("$66,000.00");
        assertThat(h.balanceSavingsText()).isEqualTo("$5,000.00");
        assertThat(h.chipCents()).isEqualTo(WALLET);
        assertThat(h.clabeText()).isEqualTo(MY_CLABE);
    }

    @Test
    @Tag("recomendado")
    @DisplayName("A_BANK_002_AccesosRapidosDelResumenNaveganATransferirYMovimientos")
    void A_BANK_002_AccesosRapidosDelResumenNaveganATransferirYMovimientos() {
        new BankHomePage(driver).open();
        driver.findElement(org.openqa.selenium.By.cssSelector("[data-test='bank-go-transfer']")).click();
        assertThat(waitUrl("/bank/transfer")).isTrue();
        assertThat(driver.findElement(org.openqa.selenium.By.tagName("h1")).getText()).contains("Transferencia SPEI");
        driver.findElement(org.openqa.selenium.By.cssSelector("[data-test='nav-bank']")).click();
        assertThat(waitUrl("/bank/")).isTrue();
        assertThat(driver.findElement(org.openqa.selenium.By.tagName("h1")).getText()).contains("Tus cuentas");
        driver.findElement(org.openqa.selenium.By.cssSelector("[data-test='bank-go-movements']")).click();
        assertThat(waitUrl("/bank/movements")).isTrue();
        assertThat(driver.findElement(org.openqa.selenium.By.tagName("h1")).getText()).contains("Movimientos");
    }

    private boolean waitUrl(String part) {
        return new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(10))
                .until(d -> d.getCurrentUrl().replaceAll("/+$", "").endsWith(part.replaceAll("/+$", "")));
    }

    @Test
    @Tag("recomendado")
    @DisplayName("A_BANK_003_UltimosMovimientos5ElementosDelMasRecienteAlMasAntiguo")
    void A_BANK_003_UltimosMovimientos5ElementosDelMasRecienteAlMasAntiguo() {
        BankHomePage h = new BankHomePage(driver).open();
        assertThat(h.recentItems()).hasSize(5);
        // El primero coincide con la primera fila de Movimientos ordenada por fecha (descendente por defecto).
        String concept = h.recentConcept(0);
        long cents = h.recentCents(0);
        BankMovementsPage m = new BankMovementsPage(driver).open();
        assertThat(m.concept(0)).isEqualTo(concept);
        assertThat(m.signedCents(0)).isEqualTo(cents);
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_BANK_005_TraspasoDeLaCuentaMoreWaterAlAhorro")
    void A_BANK_005_TraspasoDeLaCuentaMoreWaterAlAhorro() {
        BankHomePage h = new BankHomePage(driver).open();
        String msg = h.submitMoveAndGetMessage(true, "1000");
        assertThat(msg).isEqualTo("Traspaso de $1,000.00 realizado.");
        assertThat(h.moveAmountValue()).isEmpty();
        assertThat(h.mainCents()).isEqualTo(WALLET - 100_000);
        assertThat(h.savingsCents()).isEqualTo(SAVINGS + 100_000);
        assertThat(h.chipCents()).isEqualTo(WALLET - 100_000);
        assertThat(h.recentConcept(0)).isEqualTo("Traspaso a cuenta de ahorro");
        assertThat(h.recentCents(0)).isEqualTo(-100_000);
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_BANK_006_TraspasoDelAhorroALaCuentaMoreWater")
    void A_BANK_006_TraspasoDelAhorroALaCuentaMoreWater() {
        BankHomePage h = new BankHomePage(driver).open();
        assertThat(h.submitMoveAndGetMessage(false, "2000.50")).isEqualTo("Traspaso de $2,000.50 realizado.");
        assertThat(h.mainCents()).isEqualTo(WALLET + 200_050);
        assertThat(h.savingsCents()).isEqualTo(SAVINGS - 200_050);
        assertThat(h.recentConcept(0)).isEqualTo("Traspaso desde cuenta de ahorro");
        assertThat(h.recentCents(0)).isEqualTo(200_050);
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_BANK_007_TraspasoMontoVacioCeroNegativoONoNumerico")
    void A_BANK_007_TraspasoMontoVacioCeroNegativoONoNumerico() {
        BankHomePage h = new BankHomePage(driver).open();
        String first = h.recentText(0);
        for (String v : new String[] {"", "0", "-100", "abc", "0.004"}) {
            assertThat(h.submitMoveAndGetMessage(true, v)).as("monto «%s»", v).isEqualTo("Escribe un monto mayor a cero.");
            assertThat(h.moveMsgIsError()).isTrue();
        }
        assertThat(h.mainCents()).isEqualTo(WALLET);
        assertThat(h.savingsCents()).isEqualTo(SAVINGS);
        assertThat(h.recentText(0)).isEqualTo(first);
    }

    @Test
    @Tag("recomendado")
    @DisplayName("A_BANK_008_TraspasoSaldoInsuficienteEnLaCuentaDeOrigen")
    void A_BANK_008_TraspasoSaldoInsuficienteEnLaCuentaDeOrigen() {
        BankHomePage h = new BankHomePage(driver).open();
        String over = String.format("%.2f", (WALLET + 1) / 100.0);
        assertThat(h.submitMoveAndGetMessage(true, over)).isEqualTo("Saldo insuficiente en la cuenta de origen.");
        assertThat(h.submitMoveAndGetMessage(false, "5000.01")).isEqualTo("Saldo insuficiente en la cuenta de origen.");
        assertThat(h.mainCents()).isEqualTo(WALLET);
        assertThat(h.savingsCents()).isEqualTo(SAVINGS);
    }

    @Test
    @Tag("recomendado")
    @DisplayName("A_BANK_010_LosTraspasosPersistenAlRecargarYAparecenEnMovimientos")
    void A_BANK_010_LosTraspasosPersistenAlRecargarYAparecenEnMovimientos() {
        BankHomePage h = new BankHomePage(driver).open();
        assertThat(h.submitMoveAndGetMessage(true, "1000")).contains("realizado");
        h.reload();
        assertThat(h.mainCents()).isEqualTo(WALLET - 100_000);
        assertThat(h.savingsCents()).isEqualTo(SAVINGS + 100_000);
        BankMovementsPage m = new BankMovementsPage(driver).open();
        assertThat(m.concept(0)).isEqualTo("Traspaso a cuenta de ahorro");
        assertThat(m.rowApp(0)).isEqualTo("Bank");
        assertThat(m.signedCents(0)).isEqualTo(-100_000);
        assertThat(h.chipCents()).isEqualTo(WALLET - 100_000);
    }
}
