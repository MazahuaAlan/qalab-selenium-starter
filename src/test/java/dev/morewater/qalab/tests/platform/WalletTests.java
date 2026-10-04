package dev.morewater.qalab.tests.platform;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.core.BaseTest;
import dev.morewater.qalab.pages.platform.IdPage;
import dev.morewater.qalab.pages.platform.WalletPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Billetera compartida (/wallet/) con la sesión del usuario configurado. */
class WalletTests extends BaseTest {

    @Test
    @Tag("obligatorio")
    @DisplayName("Saldo inicial y sin movimientos (CP-PLAT-032)")
    void saldoInicialSinMovimientos() {
        var w = new WalletPage(driver);
        w.openWallet();
        assertThat(w.walletCents()).isEqualTo(6_600_000L);
        assertThat(w.balanceText()).isEqualTo("$66,000.00");
        assertThat(w.noMovementsText()).contains("Aún no hay movimientos.");
        assertThat(w.hasMovementsTable()).isFalse();
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("setWallet actualiza saldo y chip (CP-PLAT-033)")
    void setWalletActualizaSaldoYChip() {
        var w = new WalletPage(driver).openWallet();
        w.setWallet(1_234_567);
        w.until(() -> w.balanceText().equals("$12,345.67"));
        assertThat(w.walletChipText()).isEqualTo("Saldo $12,345.67");
    }

    @Test
    @Tag("recomendado")
    @DisplayName("Saldo persiste tras recargar (CP-PLAT-037)")
    void saldoPersisteTrasRecargar() {
        var w = new WalletPage(driver).openWallet();
        w.setWallet(50_000);
        w.until(() -> w.balanceText().equals("$500.00"));
        driver.navigate().refresh();
        w.waitReady();
        assertThat(w.balanceText()).isEqualTo("$500.00");
        String raw = w.eval("return localStorage.getItem('qalab.state.v1')");
        assertThat(raw).contains("\"walletCents\":50000");
    }

    @Test
    @Tag("recomendado")
    @DisplayName("Wallet chip visible solo con sesión y enlaza a /wallet/ (CP-PLAT-038)")
    void walletChipSoloConSesion() {
        var p = new IdPage(driver);
        p.open("/");
        p.reset();
        driver.navigate().refresh();
        p.waitReady();
        p.until(p::hasLoginLink);
        assertThat(p.hasWalletChip()).isFalse();
        p.openId().loginOk(dev.morewater.qalab.config.Config.user());
        assertThat(p.walletChipText()).isEqualTo("Saldo " + IdPage.mxn(p.walletCents()));
        p.walletChip().click();
        p.until(() -> p.path().equals("/wallet/"));
        assertThat(new WalletPage(driver).balanceText()).isEqualTo(IdPage.mxn(p.walletCents()));
    }
}
