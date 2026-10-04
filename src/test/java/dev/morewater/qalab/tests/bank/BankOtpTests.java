package dev.morewater.qalab.tests.bank;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.core.BaseTest;
import dev.morewater.qalab.pages.bank.BankReceiptPage;
import dev.morewater.qalab.pages.bank.BankTransferPage;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.support.ui.WebDriverWait;

/** HU: autorizar la transferencia con un código SMS (envío, intentos, vigencia y reenvío). */
class BankOtpTests extends BaseTest {
    private static final long WALLET = 6_600_000L;

    private BankTransferPage toOtp() {
        BankTransferPage p = new BankTransferPage(driver).open().toConfirm("100", "Pago de prueba");
        return p.sendCode();
    }

    private String wrongCode(BankTransferPage p) {
        String c = p.currentCode();
        return c.equals("111111") ? "222222" : "111111";
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("Envío del código genera SMS y bloquea el reenvío 10 s (CP-BANK-032)")
    @Tag("slow")
    void sendCodeCreatesSmsAndBlocksResend() {
        BankTransferPage p = toOtp();
        assertThat(p.stepperCurrent()).isEqualTo("Código");
        assertThat(p.resendEnabled()).isFalse();
        assertThat(p.resendText()).startsWith("Reenviar código (");
        new WebDriverWait(driver, Duration.ofSeconds(20)).until(d -> p.resendEnabled());
        assertThat(p.resendText()).isEqualTo("Reenviar código");
        p.go("/bank/messages/");
        String sms = p.smsText(0);
        Matcher m = Pattern.compile("^MoreBank: tu código para confirmar la transferencia es (\\d{6})\\. Vence en 2 minutos\\. No lo compartas\\.$").matcher(sms);
        assertThat(m.matches()).as("SMS: %s", sms).isTrue();
        assertThat(Integer.parseInt(m.group(1))).isBetween(100000, 999999);
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("Código correcto autoriza y debita exactamente el monto (CP-BANK-033)")
    void correctCodeDebitsExactAmount() {
        BankTransferPage p = toOtp();
        long before = p.walletCents();
        assertThat(before).isEqualTo(WALLET);
        p.submitCorrectCode();
        BankReceiptPage r = new BankReceiptPage(driver);
        assertThat(r.title()).isEqualTo("Transferencia enviada");
        assertThat(r.amount()).isEqualTo("$100.00");
        assertThat(p.walletCents()).isEqualTo(before - 10_000);
    }

    @Test
    @Tag("recomendado")
    @DisplayName("Código incorrecto: aviso de intentos restantes (CP-BANK-034)")
    void wrongCodeShowsRemainingAttempts() {
        BankTransferPage p = toOtp();
        String bad = wrongCode(p);
        p.enterCode(bad).submitCode();
        assertThat(p.otpError()).contains("Código incorrecto. Te quedan 2 intentos.");
        p.enterCode(bad).submitCode();
        assertThat(p.otpError()).contains("Código incorrecto. Te quedan 1 intento.");
        assertThat(p.onOtp()).isTrue();
        assertThat(p.walletCents()).isEqualTo(WALLET);
    }

    @Test
    @Tag("recomendado")
    @DisplayName("Tres códigos incorrectos cancelan la transferencia (CP-BANK-035)")
    void threeWrongCodesCancel() {
        BankTransferPage p = toOtp();
        String bad = wrongCode(p);
        for (int i = 0; i < 3; i++) p.enterCode(bad).submitCode();
        assertThat(p.otpError()).contains("Demasiados intentos. La transferencia se canceló por seguridad.");
        assertThat(p.onOtp()).isFalse();
        assertThat(p.onConfirm()).isFalse();
        assertThat(p.onForm()).isTrue();
        assertThat(p.walletCents()).isEqualTo(WALLET);
    }

    @Test
    @Tag("recomendado")
    @Tag("slow")
    @DisplayName("Código vigente 2 minutos (frontera 11 s válido, 125 s expira) (CP-BANK-036)")
    void codeValidFor2Minutes() {
        BankTransferPage p = toOtp();
        long t0 = System.currentTimeMillis();
        new WebDriverWait(driver, Duration.ofSeconds(20)).until(d -> System.currentTimeMillis() - t0 > 11_000);
        p.submitCorrectCode();
        assertThat(new BankReceiptPage(driver).title()).isEqualTo("Transferencia enviada");
        // Segundo flujo: se hace expirar el código adelantando su vencimiento (125 s sería impracticable de esperar).
        BankTransferPage q = new BankTransferPage(driver).open().toConfirm("100", "Expira").sendCode();
        long left = ((Number) q.jsExpiresInMs()).longValue();
        assertThat(left).as("vigencia inicial en ms").isBetween(110_000L, 120_000L);
        q.expireCodeNow();
        q.enterCode(q.currentCode()).submitCode();
        assertThat(q.otpError()).contains("El código expiró");
    }

    @Test
    @Tag("recomendado")
    @Tag("slow")
    @DisplayName("Reenviar código invalida el anterior y reinicia intentos (CP-BANK-037)")
    void resendInvalidatesPreviousCode() {
        BankTransferPage p = toOtp();
        String first = p.currentCode();
        String bad = first.equals("111111") ? "222222" : "111111";
        p.enterCode(bad).submitCode();
        assertThat(p.otpError()).contains("Te quedan 2 intentos");
        new WebDriverWait(driver, Duration.ofSeconds(20)).until(d -> p.resendEnabled());
        long smsBefore = p.smsCount();
        long calls = p.callsNow();
        p.clickResend(calls);
        assertThat(p.smsCount()).isEqualTo(smsBefore + 1);
        String second = p.currentCode();
        assertThat(((Number) p.attempts()).intValue()).as("intentos reiniciados").isZero();
        if (!second.equals(first)) {
            p.enterCode(first).submitCode();
            assertThat(p.otpError()).contains("Código incorrecto. Te quedan 2 intentos.");
        }
        p.enterCode(second).submitCode();
        assertThat(new BankReceiptPage(driver).title()).isEqualTo("Transferencia enviada");
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("[bank.amnesia_transfer] Persona amnesia: al recargar se pierde la transferencia en curso (CP-BANK-022)")
    @Tag("bug")
    void draftSurvivesReloadAtCodeStep() {
        BankTransferPage p = new BankTransferPage(driver).open().toConfirm("100", "Pago de prueba");
        p.sendCode();
        p.reload().waitAnyStep();
        assertThat(p.onOtp()).as("el paso Código se conserva tras recargar").isTrue();
        assertThat(p.onForm()).isFalse();
    }
}
