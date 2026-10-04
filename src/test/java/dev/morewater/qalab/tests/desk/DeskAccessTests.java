package dev.morewater.qalab.tests.desk;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morewater.qalab.core.DriverFactory;
import dev.morewater.qalab.core.ScreenshotOnFailure;
import dev.morewater.qalab.pages.desk.DeskAccessPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** HU-DESK-01: acceso sin sesión. No usa BaseTest porque gestiona su propio inicio de sesión. */
@ExtendWith(ScreenshotOnFailure.class)
class DeskAccessTests {
    @AfterEach
    void tearDown() { DriverFactory.quit(); }

    @Test
    @Tag("obligatorio")
    @DisplayName("A_DESK_004_RedireccionALoginSinSesion")
    void A_DESK_004_RedireccionALoginSinSesion() {
        DeskAccessPage access = new DeskAccessPage(DriverFactory.create());
        assertThat(access.openProtected("/desk/")).contains("/id/?next=%2Fdesk%2F");
        assertThat(access.openProtected("/desk/list/")).contains("/id/?next=%2Fdesk%2Flist%2F");
        access.login("estandar");
        assertThat(access.hasSession()).isTrue();
    }
}
