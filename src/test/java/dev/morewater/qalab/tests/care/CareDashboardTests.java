package dev.morewater.qalab.tests.care;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.core.BaseTest;
import dev.morewater.qalab.pages.care.CarePage;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/** HU-CARE-02: resumen y progreso del perfil. */
class CareDashboardTests extends BaseTest {

    private double fillRatio(CarePage care) {
        Number r = care.run("const f=document.querySelector(\"[data-test='care-progress-fill']\"), b=document.querySelector(\"[data-test='care-progress-bar']\");"
                + "return f.getBoundingClientRect().width / b.getBoundingClientRect().width;");
        return r.doubleValue();
    }

    @Test
    @Tag("recomendado")
    @DisplayName("Progreso 100 % tras guardar un perfil completo (CP-CARE-010)")
    void progressIs100AfterFullProfile() {
        CarePage care = new CarePage(driver);
        care.completeProfile();
        care.clickOn("nav-care");
        assertThat(care.el("care-progress-text").getText()).isEqualTo("100 %");
        assertThat(care.el("care-progress-bar").getAttribute("aria-valuenow")).isEqualTo("100");
        assertThat(care.el("care-go-profile").getText()).isEqualTo("Ver mi perfil");
        assertThat(fillRatio(care)).isBetween(0.99, 1.01);
    }

    @Test
    @Tag("bug")
    @Tag("opcional")
    @DisplayName("[care.visual_progress] Persona visual: la barra de progreso no coincide con el porcentaje (CP-CARE-011)")
    void progressBarMatchesPercentage() {
        CarePage care = new CarePage(driver);
        care.completeProfile();
        care.clickOn("nav-care");
        assertThat(care.el("care-progress-text").getText()).isEqualTo("100 %");
        assertThat(fillRatio(care)).as("ancho del relleno respecto a la barra").isBetween(0.99, 1.01);
    }

    @Test
    @Tag("opcional")
    @DisplayName("Resumen del rol Médico (CP-CARE-013)")
    void doctorRoleSummary() {
        CarePage care = new CarePage(driver);
        care.openPath("/care/");
        care.el("care-role");
        care.choose("care-role", "medico");
        assertThat(care.el("care-go-doctor")).isNotNull();
        assertThat(driver.findElement(By.tagName("h1")).getText()).isEqualTo("Panel del médico");
        List<WebElement> links = driver.findElements(By.cssSelector("nav[aria-label='Navegación de la app'] a"));
        assertThat(links.stream().map(WebElement::getText).toList()).containsExactly("Resumen", "Agenda del médico");
        care.clickOn("care-go-doctor");
        care.becomesTrue(15, d -> d.getCurrentUrl().contains("/care/doctor"));
        assertThat(driver.getCurrentUrl()).contains("/care/doctor");
        care.becomesTrue(15, d -> d.findElement(By.tagName("h1")).getText().contains("Agenda del médico"));
        assertThat(driver.findElement(By.tagName("h1")).getText()).contains("Agenda del médico");
    }
}
