package dev.morewater.qalab.pages.bank;

import dev.morewater.qalab.pages.BasePage;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

/** Tabla de movimientos (/bank/movements/): filtros, orden, paginación y exportación CSV. */
public class BankMovementsPage extends BasePage {
    public BankMovementsPage(WebDriver driver) { super(driver); }

    public BankMovementsPage open() { go("/bank/movements/"); return waitTable(); }

    public BankMovementsPage waitTable() { visible("bank-table"); return this; }

    public int rowCount() { return driver.findElements(By.cssSelector("[data-test^='bank-row-']")).size(); }
    public String concept(int i) { return text("bank-concept-" + i); }
    public String debit(int i) { return visible("bank-debit-" + i).getText().trim(); }
    public String credit(int i) { return visible("bank-credit-" + i).getText().trim(); }
    public WebElement debitCell(int i) { return visible("bank-debit-" + i); }
    public WebElement creditCell(int i) { return visible("bank-credit-" + i); }
    /** Importe con signo en centavos de la fila (cargo negativo, abono positivo). */
    public long signedCents(int i) {
        String d = debit(i), c = credit(i);
        return d.isEmpty() ? Math.abs(cents(c)) : -Math.abs(cents(d));
    }
    public String rowApp(int i) { return visible("bank-row-" + i).findElements(By.tagName("td")).get(2).getText().trim(); }
    public String rowDate(int i) { return visible("bank-row-" + i).findElements(By.tagName("td")).get(0).getText().trim(); }

    public String pageInfo() { return text("bank-page-info"); }
    public int pageButtons() { return driver.findElements(By.cssSelector("[data-test^='bank-page-'][data-test$='0'],[data-test^='bank-page-'][data-test$='1'],[data-test^='bank-page-'][data-test$='2'],[data-test^='bank-page-'][data-test$='3'],[data-test^='bank-page-'][data-test$='4'],[data-test^='bank-page-'][data-test$='5'],[data-test^='bank-page-'][data-test$='6'],[data-test^='bank-page-'][data-test$='7'],[data-test^='bank-page-'][data-test$='8'],[data-test^='bank-page-'][data-test$='9']")).stream().filter(e -> e.getAttribute("data-test").matches("bank-page-\\d+")).toList().size(); }
    public BankMovementsPage goPage(int n) { click("bank-page-" + n); return this; }
    public BankMovementsPage next() { click("bank-page-next"); return this; }
    public BankMovementsPage prev() { click("bank-page-prev"); return this; }
    public boolean nextEnabled() { return visible("bank-page-next").isEnabled(); }
    public boolean prevEnabled() { return visible("bank-page-prev").isEnabled(); }

    public BankMovementsPage search(String q) {
        WebElement el = visible("bank-search");
        Fields.replace(el, q);
        return this;
    }
    public BankMovementsPage type(String value) { new Select(visible("bank-type")).selectByValue(value); return this; }
    public BankMovementsPage pageSize(int n) { new Select(visible("bank-page-size")).selectByValue(String.valueOf(n)); return this; }
    public BankMovementsPage sort(String key) { click("bank-sort-" + key); return this; }
    public boolean empty() { return present("bank-empty"); }

    public List<String> concepts() {
        return driver.findElements(By.cssSelector("[data-test^='bank-concept-']")).stream().map(e -> e.getText().trim()).toList();
    }
    public String debitHeader() { return text("bank-th-debit"); }
    public String creditHeader() { return text("bank-th-credit"); }
}
