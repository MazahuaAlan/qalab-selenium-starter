package dev.morewater.qalab.pages.stay;

import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** /stay/stays/ — mis estancias, modificar fechas y cancelar. */
public class StayStaysPage extends StayBase {
    public final Calendar calendar;

    public StayStaysPage(WebDriver driver) { super(driver); this.calendar = new Calendar(driver); }

    public StayStaysPage open() { go("/stay/stays/"); return waitLoaded(); }

    public StayStaysPage waitLoaded() { wait.until(d -> present("stay-stays") || present("stay-no-stays")); return this; }

    public int count() { return driver.findElements(t("stay-stay")).size(); }
    public String code() { return text("stay-stay-code"); }
    public String status() { return text("stay-stay-status"); }
    public String dates() { return text("stay-stay-dates"); }
    public String hotel() { return text("stay-stay-hotel"); }
    public boolean hasModify() { return present("stay-modify"); }
    public boolean hasCancel() { return present("stay-cancel"); }
    public String message() { return text("stay-stays-msg"); }
    public List<String> statuses() { return texts("stay-stay-status"); }

    // ---- diálogo ----
    public boolean modalOpen() { return present("stay-modal"); }
    public String modalTitle() { return visible("stay-modal").findElement(By.id("modal-title")).getText().trim(); }
    public String modalRole() { return visible("stay-modal").getDomAttribute("role"); }

    public StayStaysPage openModify() { click("stay-modify"); visible("stay-modal"); return this; }
    public StayStaysPage openCancel() { click("stay-cancel"); visible("stay-modal"); return this; }
    public String cancelPolicy() { return text("stay-cancel-policy"); }
    public StayStaysPage confirmCancel() { click("stay-cancel-confirm"); return this; }
    public String modifyDiff() { return text("stay-modify-diff"); }
    public StayStaysPage saveModify() { click("stay-modify-confirm"); return this; }
    public String modifyError() { return text("stay-modify-error"); }
    public boolean hasModifyError() { return present("stay-modify-error"); }
    public void closeModal() { click("stay-modal-close"); wait.until(d -> !modalOpen()); }
    public void waitModalClosed() { wait.until(d -> !modalOpen()); }
}
