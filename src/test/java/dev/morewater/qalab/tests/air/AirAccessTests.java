package dev.morewater.qalab.tests.air;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.core.DriverFactory;
import dev.morewater.qalab.pages.LoginPage;
import dev.morewater.qalab.pages.air.AirFlowPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Acceso sin sesión: esta prueba gestiona su propio inicio de sesión (no usa BaseTest). */
class AirAccessTests {
    @AfterEach
    void tearDown() { DriverFactory.quit(); }

    @Test @Tag("obligatorio")
    @DisplayName("A_AIR_104_AccesoAAirSinSesionRedirigeALoginYRegresa")
    void A_AIR_104_AccesoAAirSinSesionRedirigeALoginYRegresa() {
        var driver = DriverFactory.create();
        AirFlowPage air = new AirFlowPage(driver).openPath("/air/trips/");
        air.waitPath("/id");
        assertThat(air.currentUrl()).contains("next=");
        new LoginPage(driver).loginAs("estandar", "qalab123");
        air.waitPath("/air/trips");
        assertThat(air.path()).isEqualTo("/air/trips/");
        assertThat(air.userChipVisible()).isTrue();
    }
}
