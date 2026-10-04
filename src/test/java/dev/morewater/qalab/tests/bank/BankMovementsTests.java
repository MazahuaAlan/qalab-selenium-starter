package dev.morewater.qalab.tests.bank;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.core.BaseTest;
import dev.morewater.qalab.pages.bank.BankMovementsPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

/** HU: consultar movimientos (carga, búsqueda, filtros, formato) y exportarlos. */
class BankMovementsTests extends BaseTest {

    @Test
    @Tag("obligatorio")
    @DisplayName("A_BANK_050_CargaInicial45Movimientos10PorPagina")
    void A_BANK_050_CargaInicial45Movimientos10PorPagina() {
        BankMovementsPage m = new BankMovementsPage(driver).open();
        assertThat(m.pageInfo()).isEqualTo("Mostrando 1–10 de 45");
        assertThat(m.rowCount()).isEqualTo(10);
        assertThat(driver.findElements(By.cssSelector("[data-test='bank-row-10']"))).isEmpty();
        assertThat(m.pageButtons()).isEqualTo(5);
        assertThat(driver.findElement(By.cssSelector("[data-test='bank-page-1']")).getAttribute("aria-current")).isEqualTo("page");
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_BANK_053_BusquedaPorConceptoInsensibleAMayusculasYConEspacio")
    void A_BANK_053_BusquedaPorConceptoInsensibleAMayusculasYConEspacio() {
        BankMovementsPage m = new BankMovementsPage(driver).open();
        m.search("nómina");
        assertThat(m.concepts()).isNotEmpty().allMatch(c -> c.contains("Nómina"));
        String info = m.pageInfo();
        java.util.List<String> first = m.concepts();
        m.search("NÓMINA");
        assertThat(m.pageInfo()).isEqualTo(info);
        assertThat(m.concepts()).isEqualTo(first);
        m.search("  nómina  ");
        assertThat(m.pageInfo()).isEqualTo(info);
        assertThat(m.concepts()).isEqualTo(first);
        m.search("");
        assertThat(m.pageInfo()).isEqualTo("Mostrando 1–10 de 45");
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_BANK_055_FiltroPorTipoSoloCargosYSoloAbonos")
    void A_BANK_055_FiltroPorTipoSoloCargosYSoloAbonos() {
        BankMovementsPage m = new BankMovementsPage(driver).open();
        m.type("cargo");
        assertThat(m.rowCount()).isPositive();
        for (int i = 0; i < m.rowCount(); i++) {
            assertThat(m.debit(i)).as("cargo fila %d", i).isNotEmpty();
            assertThat(m.credit(i)).as("abono fila %d", i).isEmpty();
        }
        m.type("abono");
        assertThat(m.rowCount()).isPositive();
        for (int i = 0; i < m.rowCount(); i++) {
            assertThat(m.credit(i)).as("abono fila %d", i).isNotEmpty();
            assertThat(m.debit(i)).as("cargo fila %d", i).isEmpty();
        }
        m.type("all");
        assertThat(m.pageInfo()).isEqualTo("Mostrando 1–10 de 45");
    }

    @Test
    @Tag("recomendado")
    @Tag("bug")
    @DisplayName("[bank.slow_movements] A_BANK_062_PersonaLentoLaTablaDeMovimientosTarda35S")
    void A_BANK_062_PersonaLentoLaTablaDeMovimientosTarda35S() {
        BankMovementsPage m = new BankMovementsPage(driver);
        m.go("/bank/movements/");
        long ms = m.millisToFirstRow();
        assertThat(ms).as("ms hasta la primera fila").isLessThan(3_000);
    }

    @Test
    @Tag("obligatorio")
    @Tag("bug")
    @DisplayName("[bank.flaky_movements] A_BANK_063_PersonaIntermitenteMovimientosFallanCon503AlAzarYReintentar")
    void A_BANK_063_PersonaIntermitenteMovimientosFallanCon503AlAzarYReintentar() {
        BankMovementsPage m = new BankMovementsPage(driver);
        for (int i = 1; i <= 8; i++) {
            m.go("/bank/movements/");
            m.waitTableOrError();
            assertThat(m.hasError()).as("carga %d sin error 503", i).isFalse();
        }
        // Reintentar funciona: se fuerza un fallo con el panel de caos y se recupera.
        m.setChaosFail(100).reload().waitTableOrError();
        assertThat(m.hasError()).isTrue();
        assertThat(m.errorText()).contains("No pudimos cargar los movimientos");
        m.setChaosFail(0).retry();
        m.waitTable();
        assertThat(m.hasError()).isFalse();
        assertThat(m.rowCount()).isEqualTo(10);
    }

    @Test
    @Tag("recomendado")
    @Tag("bug")
    @DisplayName("[bank.visual_signs] A_BANK_064_PersonaVisualCargosEnVerdeConYAbonosEnRojoCon")
    void A_BANK_064_PersonaVisualCargosEnVerdeConYAbonosEnRojoCon() {
        BankMovementsPage m = new BankMovementsPage(driver).open();
        m.type("cargo");
        assertThat(m.debit(0)).startsWith("−");
        assertThat(m.amountClass(true, 0)).contains("amt-neg");
        m.type("abono");
        assertThat(m.credit(0)).startsWith("+");
        assertThat(m.amountClass(false, 0)).contains("amt-pos");
    }

    @Test
    @Tag("recomendado")
    @Tag("bug")
    @DisplayName("[bank.visual_columns] A_BANK_065_PersonaVisualEncabezadosCargoYAbonoIntercambiados")
    void A_BANK_065_PersonaVisualEncabezadosCargoYAbonoIntercambiados() {
        BankMovementsPage m = new BankMovementsPage(driver).open();
        assertThat(m.debitHeader()).startsWith("Cargo");
        assertThat(m.creditHeader()).isEqualTo("Abono");
        m.type("cargo");
        assertThat(m.debit(0)).isNotEmpty();
    }

    @Test
    @Tag("recomendado")
    @Tag("bug")
    @DisplayName("[bank.csv_total_mismatch] A_BANK_070_PersonaDescuadreElCSVOmiteElUltimoMovimientoPeroElTotalLo")
    void A_BANK_070_PersonaDescuadreElCSVOmiteElUltimoMovimientoPeroElTotalLo() {
        BankMovementsPage m = new BankMovementsPage(driver).open();
        String[] lines = m.exportCsv().strip().split("\n");
        assertThat(lines[0]).isEqualTo("fecha,concepto,app,cargo,abono");
        String total = lines[lines.length - 1];
        assertThat(total).startsWith("TOTAL");
        long dataRows = lines.length - 2;
        assertThat(dataRows).as("filas de datos").isEqualTo(45);
        long sumC = 0, sumA = 0;
        for (int i = 1; i < lines.length - 1; i++) {
            // el concepto puede traer comillas; los 2 últimos campos son cargo y abono
            String l = lines[i];
            int c2 = l.lastIndexOf(','), c1 = l.lastIndexOf(',', c2 - 1);
            String cargo = l.substring(c1 + 1, c2), abono = l.substring(c2 + 1);
            if (!cargo.isEmpty()) sumC += Math.round(Double.parseDouble(cargo) * 100);
            if (!abono.isEmpty()) sumA += Math.round(Double.parseDouble(abono) * 100);
        }
        String[] t = total.split(",", -1);
        assertThat(Math.round(Double.parseDouble(t[3]) * 100)).as("total cargos").isEqualTo(sumC);
        assertThat(Math.round(Double.parseDouble(t[4]) * 100)).as("total abonos").isEqualTo(sumA);
    }
}
