package dev.morewater.qalab.tests.bank;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.core.BaseTest;
import dev.morewater.qalab.pages.bank.BankTransferPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** HU: capturar y confirmar una transferencia SPEI a un beneficiario guardado. */
class BankTransferFormTests extends BaseTest {

    @Test
    @Tag("obligatorio")
    @DisplayName("A_BANK_011_TransferenciaValidacionesDeCamposVacios")
    void A_BANK_011_TransferenciaValidacionesDeCamposVacios() {
        BankTransferPage p = new BankTransferPage(driver).open();
        assertThat(p.stepperCurrent()).isEqualTo("Datos");
        assertThat(p.beneficiaryOptions().get(0)).isEqualTo("Ana Torres · Banco Nimbo ···5675");
        p.next();
        assertThat(p.amountError()).isEqualTo("El monto mínimo es $1.00.");
        assertThat(p.conceptError()).isEqualTo("Escribe un concepto.");
        assertThat(p.onConfirm()).isFalse();
        p.setValue("bank-amount", "100");
        p.next();
        assertThat(p.hasAmountError()).isFalse();
        assertThat(p.conceptError()).isEqualTo("Escribe un concepto.");
        p.setValue("bank-concept", "Pago");
        p.next();
        assertThat(p.hasConceptError()).isFalse();
        assertThat(p.confirmAmount()).isEqualTo("$100.00");
        assertThat(p.stepperCurrent()).isEqualTo("Confirmar");
    }

    @Test
    @Tag("recomendado")
    @DisplayName("A_BANK_012_TransferenciaMontoMinimo100Frontera099100")
    void A_BANK_012_TransferenciaMontoMinimo100Frontera099100() {
        BankTransferPage p = new BankTransferPage(driver).open();
        p.fill("0.99", "Frontera").next();
        assertThat(p.amountError()).isEqualTo("El monto mínimo es $1.00.");
        p.setValue("bank-amount", "1.00");
        p.next();
        assertThat(p.confirmAmount()).isEqualTo("$1.00");
    }

    @Test
    @Tag("recomendado")
    @DisplayName("A_BANK_014_TransferenciaSaldoInsuficienteYFronteraDelSaldo")
    void A_BANK_014_TransferenciaSaldoInsuficienteYFronteraDelSaldo() {
        BankTransferPage p = new BankTransferPage(driver).open();
        p.setWallet(50000);
        p.reload().waitAnyStep();
        assertThat(p.amountHint()).contains("saldo $500.00");
        p.fill("500.01", "Saldo").next();
        assertThat(p.amountError()).isEqualTo("Saldo insuficiente.");
        p.setValue("bank-amount", "500.00");
        p.next();
        assertThat(p.confirmAmount()).isEqualTo("$500.00");
        p.sendCode().submitCorrectCode();
        assertThat(p.walletCents()).isZero();
    }

    @Test
    @Tag("opcional")
    @DisplayName("A_BANK_016_TransferenciaConceptoSoloConEspaciosCuentaComoVacio")
    void A_BANK_016_TransferenciaConceptoSoloConEspaciosCuentaComoVacio() {
        BankTransferPage p = new BankTransferPage(driver).open();
        p.fill("100", "     ").next();
        assertThat(p.conceptError()).isEqualTo("Escribe un concepto.");
        assertThat(p.onConfirm()).isFalse();
        p.setValue("bank-concept", " Renta ");
        p.next();
        assertThat(p.confirmConcept()).isEqualTo("Renta");
    }

    @Test
    @Tag("recomendado")
    @DisplayName("A_BANK_017_ListaDeBeneficiariosPrecargadosConBancoYUltimos4Digitos")
    void A_BANK_017_ListaDeBeneficiariosPrecargadosConBancoYUltimos4Digitos() {
        BankTransferPage p = new BankTransferPage(driver).open();
        assertThat(p.beneficiaryOptions()).containsExactly(
                "Ana Torres · Banco Nimbo ···5675",
                "Luis Mendoza · Caja Aurora ···9012",
                "Renta departamento · Banco Cóndor ···2107",
                "Nuevo beneficiario…");
        p.chooseBeneficiary("b2").toConfirm("250", "Cena");
        assertThat(p.confirmName()).isEqualTo("Luis Mendoza");
        assertThat(p.confirmClabe()).isEqualTo("902 320 00045678901 2");
        assertThat(p.confirmBank()).isEqualTo("Caja Aurora");
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_BANK_018_PantallaDeConfirmacionMuestraTodosLosDatosCorrectos")
    void A_BANK_018_PantallaDeConfirmacionMuestraTodosLosDatosCorrectos() {
        BankTransferPage p = new BankTransferPage(driver).open().toConfirm("1500.50", "Renta de octubre");
        assertThat(p.stepperCurrent()).isEqualTo("Confirmar");
        assertThat(p.confirmName()).isEqualTo("Ana Torres");
        assertThat(p.confirmClabe()).isEqualTo("901 180 00001234567 5");
        assertThat(p.confirmBank()).isEqualTo("Banco Nimbo");
        assertThat(p.confirmAmount()).isEqualTo("$1,500.50");
        assertThat(p.confirmFee()).isEqualTo("$0.00");
        assertThat(p.confirmConcept()).isEqualTo("Renta de octubre");
    }

    @Test
    @Tag("recomendado")
    @DisplayName("A_BANK_019_EditarDatosRegresaAlFormularioConservandoLoCapturado")
    void A_BANK_019_EditarDatosRegresaAlFormularioConservandoLoCapturado() {
        BankTransferPage p = new BankTransferPage(driver).open().toConfirm("50", "hola");
        assertThat(p.confirmAmount()).isEqualTo("$50.00");
        p.edit();
        assertThat(p.value("bank-amount")).isEqualTo("50");
        assertThat(p.value("bank-concept")).isEqualTo("hola");
        p.setValue("bank-amount", "75");
        p.next();
        assertThat(p.confirmAmount()).isEqualTo("$75.00");
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_BANK_020_AlRecargarSeConservaLaTransferenciaEnElPasoConfirmarYCodigo")
    void A_BANK_020_AlRecargarSeConservaLaTransferenciaEnElPasoConfirmarYCodigo() {
        BankTransferPage p = new BankTransferPage(driver).open().toConfirm("100", "Pago de prueba");
        p.reload().waitAnyStep();
        assertThat(p.onConfirm()).as("paso Confirmar tras recargar").isTrue();
        assertThat(p.confirmAmount()).isEqualTo("$100.00");
        assertThat(p.confirmName()).isEqualTo("Ana Torres");
        p.sendCode();
        p.reload().waitAnyStep();
        assertThat(p.onOtp()).as("paso Código tras recargar").isTrue();
        p.submitCorrectCode();
        assertThat(p.onConfirm()).isFalse();
    }
}
