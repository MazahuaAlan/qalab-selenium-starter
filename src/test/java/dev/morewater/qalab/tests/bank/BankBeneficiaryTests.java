package dev.morewater.qalab.tests.bank;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.core.BaseTest;
import dev.morewater.qalab.pages.bank.BankReceiptPage;
import dev.morewater.qalab.pages.bank.BankTransferPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** HU: alta de beneficiarios nuevos y validación de CLABE. */
class BankBeneficiaryTests extends BaseTest {
    private static final String DEMO_CLABE = "901180000099999992";

    private BankTransferPage newBen(String name, String clabe) {
        BankTransferPage p = new BankTransferPage(driver).open().newBeneficiary(name, clabe);
        p.fill("100", "Prueba");
        return p;
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_BANK_023_AltaDeBeneficiarioNuevoConCLABEValidaYGuardado")
    void A_BANK_023_AltaDeBeneficiarioNuevoConCLABEValidaYGuardado() {
        BankTransferPage p = new BankTransferPage(driver).open().chooseBeneficiary("new");
        assertThat(p.clabeExample()).isEqualTo(DEMO_CLABE);
        assertThat(p.saveChecked()).isTrue();
        p.newBeneficiary("Carla Núñez", p.clabeExample()).fill("100", "Prueba").next();
        assertThat(p.confirmName()).isEqualTo("Carla Núñez");
        assertThat(p.confirmClabe()).isEqualTo("901 180 00009999999 2");
        assertThat(p.confirmBank()).isEqualTo("Banco Nimbo");
        p.sendCode().submitCorrectCode();
        assertThat(new BankReceiptPage(driver).title()).isEqualTo("Transferencia enviada");
        assertThat(new BankTransferPage(driver).open().beneficiaryOptions()).contains("Carla Núñez · Banco Nimbo ···9992");
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_BANK_024_CLABEConLongitudIncorrectaOConLetras")
    void A_BANK_024_CLABEConLongitudIncorrectaOConLetras() {
        BankTransferPage p = newBen("Carla Núñez", "123");
        for (String bad : new String[] {"123", "90118000009999999", "9011800000999999920", "90118000009999999A"}) {
            p.setValue("bank-ben-clabe", bad);
            p.next();
            assertThat(p.clabeError()).as("CLABE «%s»", bad).isEqualTo("La CLABE debe tener 18 dígitos.");
            assertThat(p.onConfirm()).isFalse();
        }
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_BANK_026_CLABEConDigitoVerificadorIncorrecto")
    void A_BANK_026_CLABEConDigitoVerificadorIncorrecto() {
        BankTransferPage p = new BankTransferPage(driver).open().chooseBeneficiary("new");
        String ok = p.clabeExample();
        int last = Character.getNumericValue(ok.charAt(17));
        String bad = ok.substring(0, 17) + ((last + 1) % 10);
        p.newBeneficiary("Carla Núñez", bad).fill("100", "Prueba").next();
        assertThat(p.clabeError()).isEqualTo("La CLABE no es válida (dígito verificador incorrecto).");
        assertThat(p.onConfirm()).isFalse();
    }

    @Test
    @Tag("recomendado")
    @DisplayName("A_BANK_029_NoGuardarElBeneficiarioNuevo")
    void A_BANK_029_NoGuardarElBeneficiarioNuevo() {
        BankTransferPage p = new BankTransferPage(driver).open().chooseBeneficiary("new");
        p.toggleSave();
        assertThat(p.saveChecked()).isFalse();
        p.newBeneficiary("Pedro Ruiz", DEMO_CLABE).fill("100", "Prueba").next();
        p.sendCode().submitCorrectCode();
        assertThat(new BankReceiptPage(driver).row("Beneficiario")).isEqualTo("Pedro Ruiz");
        assertThat(new BankTransferPage(driver).open().beneficiaryOptions())
                .hasSize(4).noneMatch(o -> o.contains("Pedro Ruiz"));
    }

    @Test
    @Tag("opcional")
    @DisplayName("A_BANK_030_NoSeDuplicaUnBeneficiarioConCLABEYaGuardada")
    void A_BANK_030_NoSeDuplicaUnBeneficiarioConCLABEYaGuardada() {
        BankTransferPage p = new BankTransferPage(driver).open();
        p.newBeneficiary("Ana T.", "901180000012345675").fill("100", "Prueba").next();
        assertThat(p.confirmName()).isEqualTo("Ana T.");
        assertThat(p.confirmClabe()).isEqualTo("901 180 00001234567 5");
        p.sendCode().submitCorrectCode();
        assertThat(new BankTransferPage(driver).open().beneficiaryOptions())
                .hasSize(4).noneMatch(o -> o.contains("Ana T."));
    }

    @Test
    @Tag("recomendado")
    @DisplayName("A_BANK_031_SeReconocenLosCuatroBancos900901902903")
    void A_BANK_031_SeReconocenLosCuatroBancos900901902903() {
        String[][] cases = {
            {"900180009876543217", "MoreBank"}, {"902320000456789012", "Caja Aurora"},
            {"903010000765432107", "Banco Cóndor"}, {DEMO_CLABE, "Banco Nimbo"}};
        BankTransferPage p = new BankTransferPage(driver).open().newBeneficiary("Banco Uno", cases[0][0]).fill("100", "Prueba");
        p.next();
        assertThat(p.confirmBank()).isEqualTo(cases[0][1]);
        for (int i = 1; i < cases.length; i++) {
            p.edit();
            p.setValue("bank-ben-clabe", cases[i][0]);
            p.next();
            assertThat(p.confirmBank()).as("CLABE %s", cases[i][0]).isEqualTo(cases[i][1]);
        }
    }
}
