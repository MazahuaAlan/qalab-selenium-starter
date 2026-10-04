package dev.morewater.qalab.tests.stay;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.core.DriverFactory;
import dev.morewater.qalab.core.ScreenshotOnFailure;
import dev.morewater.qalab.pages.LoginPage;
import dev.morewater.qalab.pages.stay.StaySearchPage;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriver;

/** HU-STAY-12 (acceso sin sesión). Gestiona su propio inicio de sesión, por eso no usa BaseTest. */
@ExtendWith(ScreenshotOnFailure.class)
class AccessTests {
    @AfterEach
    void tearDown() { DriverFactory.quit(); }

    private static String next(String url) {
        String q = url.substring(url.indexOf("next=") + 5);
        int amp = q.indexOf('&');
        return URLDecoder.decode(amp < 0 ? q : q.substring(0, amp), StandardCharsets.UTF_8);
    }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_STAY_074_AccesoSinSesionRedirigeALoginYRegresaAStay")
    void A_STAY_074_AccesoSinSesionRedirigeALoginYRegresaAStay() {
        WebDriver d = DriverFactory.create();
        StaySearchPage stay = new StaySearchPage(d);
        stay.go("/stay/");
        stay.becomesTrue(() -> d.getCurrentUrl().contains("/id/"), 15, 200);
        assertThat(d.getCurrentUrl()).contains("/id/?next=");
        assertThat(next(d.getCurrentUrl())).startsWith("/stay");

        new LoginPage(d).loginAs("estandar", "qalab123");
        stay.becomesTrue(() -> !d.findElements(org.openqa.selenium.By.cssSelector("[data-test='stay-search-form']")).isEmpty(), 15, 200);
        assertThat(stay.path()).startsWith("/stay");
        assertThat(stay.h1()).isEqualTo("¿Dónde quieres hospedarte?");
        assertThat(stay.userChip()).isEqualTo("Usuario estándar");

        stay.logout();
        stay.becomesTrue(() -> d.getCurrentUrl().contains("/id/"), 15, 200);
        stay.go("/stay/stays/");
        stay.becomesTrue(() -> d.getCurrentUrl().contains("/id/?next="), 15, 200);
        assertThat(d.getCurrentUrl()).contains("/id/?next=");
        assertThat(next(d.getCurrentUrl())).startsWith("/stay/stays");
    }
}
