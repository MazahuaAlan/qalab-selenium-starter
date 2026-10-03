package dev.morewater.qalab.tests;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.core.BaseTest;
import dev.morewater.qalab.pages.BankPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BankTests extends BaseTest {

    @Test
    @DisplayName("[bank.transfer_fee_mismatch] Una transferencia descuenta solo el monto indicado")
    void transferDebitsOnlyTheAmount() {
        BankPage bank = new BankPage(driver);
        bank.startTransfer("200", "Pago de prueba").reachedConfirmStep();
        long before = bank.walletCents();
        bank.sendCodeAndEnter();
        assertThat(bank.receiptFee()).isEqualTo("$0.00");
        assertThat(before - bank.walletCents()).as("centavos descontados").isEqualTo(20000);
    }

    @Test
    @DisplayName("[bank.daily_limit_ignored] No permite transferir por encima del límite diario")
    void dailyLimitIsEnforced() {
        BankPage bank = new BankPage(driver).startTransfer("10500", "Monto alto");
        bank.amountError();
        assertThat(bank.amountErrorText()).contains("límite diario");
    }

    @Test
    @DisplayName("[bank.visual_columns] Los encabezados Cargo y Abono corresponden a sus columnas")
    void columnHeadersMatchTheirData() {
        BankPage bank = new BankPage(driver).openMovements();
        assertThat(bank.debitHeader()).startsWith("Cargo");
        assertThat(bank.creditHeader()).isEqualTo("Abono");
    }
}
