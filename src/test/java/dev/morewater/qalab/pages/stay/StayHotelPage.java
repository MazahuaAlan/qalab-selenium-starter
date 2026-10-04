package dev.morewater.qalab.pages.stay;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** /stay/hotel/ — ficha del hotel y habitaciones. */
public class StayHotelPage extends StayBase {
    public StayHotelPage(WebDriver driver) { super(driver); }

    public StayHotelPage waitLoaded() { visible("stay-hotel-title"); return this; }

    public StayHotelPage open(String hotelId, String in, String out, int adults, int kids) {
        go("/stay/hotel/?id=" + hotelId + "&in=" + in + "&out=" + out + "&adults=" + adults + "&kids=" + kids);
        visible("stay-hotel-title");
        return this;
    }

    /** Espera a que termine la consulta de disponibilidad (habitaciones o error). */
    public StayHotelPage waitRooms() {
        wait.until(d -> present("stay-room-0") || present("stay-rooms-error"));
        return this;
    }

    public StayHotelPage waitRoomsOk() { visible("stay-room-0"); return this; }

    public String title() { return text("stay-hotel-title"); }
    public String subtitle() { return driver.findElement(By.cssSelector("[data-test='stay-hotel-title'] + p")).getText().trim(); }
    public String policy() { return text("stay-policy"); }
    public boolean roomsLoading() { return present("stay-rooms-loading"); }
    public boolean roomsError() { return present("stay-rooms-error"); }
    public String roomsErrorText() { return text("stay-rooms-error"); }
    public void retry() { click("stay-rooms-retry"); }
    public void waitExpired() { visible("stay-hold-expired"); }
    public boolean expiredNotice() { return present("stay-hold-expired"); }
    public String expiredText() { return text("stay-hold-expired"); }

    public String roomName(int i) { return text("stay-room-name-" + i); }
    public String roomRate(int i) { return text("stay-room-rate-" + i); }
    public String roomLeft(int i) { return text("stay-room-left-" + i); }
    public String roomText(int i) { return text("stay-room-" + i); }
    public boolean reserveEnabled(int i) { return enabled("stay-room-reserve-" + i); }
    public String reserveText(int i) { return text("stay-room-reserve-" + i); }

    public StayBookPage reserve(int i) {
        click("stay-room-reserve-" + i);
        return new StayBookPage(driver).waitForm();
    }
}
