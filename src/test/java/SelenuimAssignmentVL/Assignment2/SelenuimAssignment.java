package SelenuimAssignmentVL.Assignment2;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import org.testng.Assert;

public class SelenuimAssignment {

    WebDriver driver;
    WebDriverWait wait;

    String searchResultPrice, productDetailsPrice, subtotalPrice;

    @BeforeClass
    public void setUp() {
        // Set up ChromeDriver
        WebDriverManager.chromedriver().setup();
        driver = new ChromeDriver();
        driver.manage().window().maximize();
        driver.get("https://www.amazon.com/");
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @Test
    public void amazonTest() {
        // Add first product without going to cart
        searchAndAddProductToCart("Toys", false);  

        // Add second product and then go to cart
        searchAndAddProductToCart("Toys", true);   

        // Validate prices at the end
        validatePrices();
    }

    // Method to search for products and optionally go to cart after adding
    private void searchAndAddProductToCart(String searchTerm, boolean goToCartAfterAdding) {
        searchProduct(searchTerm);
        List<WebElement> productList = getProductList();
        addFirstProductWithAddToCart(productList, goToCartAfterAdding);
    }

    // Search for the given product
    private void searchProduct(String searchTerm) {
        WebElement searchBox = driver.findElement(By.id("twotabsearchtextbox"));
        searchBox.clear();  // Clear the search box before entering a new search term
        searchBox.sendKeys(searchTerm);
        searchBox.submit();
    }

    // Retrieve the list of products
    private List<WebElement> getProductList() {
        return wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector(".s-main-slot .s-result-item")));
    }

    // Add the first product with an "Add to Cart" button and optionally go to cart
    private void addFirstProductWithAddToCart(List<WebElement> productList, boolean goToCartAfterAdding) {
        for (WebElement product : productList) {
            if (product.getText().contains("Add to cart")) {
                clickProductAndAddToCart(product, goToCartAfterAdding);
                break; // Exit after adding the first product
            }
        }
    }

    // Click on a product, add it to the cart, and optionally go to cart
    private void clickProductAndAddToCart(WebElement product, boolean goToCartAfterAdding) {
        WebElement productTitle = product.findElement(By.cssSelector("h2 a"));
        scrollToElement(productTitle);

        // Extract the price from the product listing (Search Results Page)
        WebElement priceWholeElement = product.findElement(By.cssSelector(".a-price-whole"));
        WebElement priceFractionElement = product.findElement(By.cssSelector(".a-price-fraction"));
        searchResultPrice = priceWholeElement.getText() + "." + priceFractionElement.getText();
        System.out.println("Price on the search result page: $" + searchResultPrice);  // Print the price for confirmation

        // Open product link in a new tab
        String originalWindow = driver.getWindowHandle();
        ((JavascriptExecutor) driver).executeScript("window.open(arguments[0].href);", productTitle);

        // Switch to the new tab (Product Details Page)
        switchToNewTab();

        // Extract the price on the Product Details page
        WebElement detailsPriceWholeElement = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".a-price-whole")));
        WebElement detailsPriceFractionElement = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".a-price-fraction")));
        productDetailsPrice = detailsPriceWholeElement.getText() + "." + detailsPriceFractionElement.getText();  // Assigning to the instance variable
        System.out.println("Price on the product details page: $" + productDetailsPrice);  // Print the price for confirmation

        // Validate Search Results price vs Product Details page price
        validatePriceConsistency(searchResultPrice, productDetailsPrice);

        // Click "Add to Cart" button
        WebElement addToCartButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("add-to-cart-button")));
        addToCartButton.click();

        // Only go to cart after adding the second product (goToCartAfterAdding = true)
        if (goToCartAfterAdding) {
            goToCart();
        }

        // Close the current tab and switch back to the original tab
        driver.close();
        driver.switchTo().window(originalWindow);
    }

    // Click the "Go to Cart" button
    private void goToCart() {
        WebElement goToCartButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//a[@href='/cart?ref_=sw_gtc']")));
        goToCartButton.click();

        // Wait for the Subtotal element to be visible
        WebElement subtotalElement = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("sc-subtotal-amount-activecart")));
        subtotalPrice = subtotalElement.getText().replaceAll("[^0-9.]", "");  // Extract only numeric value

        // Print the Subtotal for confirmation
        System.out.println("Subtotal price on the cart page: $" + subtotalPrice);
    }

    // Validate that the Search Results page price matches the Product Details page price
    private void validatePriceConsistency(String searchPrice, String detailsPrice) {
        Assert.assertEquals(searchPrice, detailsPrice, "Price mismatch between Search Results and Product Details pages.");
    }

    // Validate that the sum of product prices matches the Subtotal on the Cart page
    private void validatePrices() {
        if (productDetailsPrice == null || subtotalPrice == null) {
            throw new IllegalStateException("Product price or subtotal is not available.");
        }

        double expectedSubtotal = Double.parseDouble(productDetailsPrice) * 2;  // Assuming you added two products
        double actualSubtotal = Double.parseDouble(subtotalPrice);

        Assert.assertEquals(actualSubtotal, expectedSubtotal, "Subtotal price on the Cart page does not match the sum of Product Details page prices.");
    }

    // Switch to the newly opened tab
    private void switchToNewTab() {
        Set<String> windowHandles = driver.getWindowHandles();
        for (String windowHandle : windowHandles) {
            if (!windowHandle.equals(driver.getWindowHandle())) {
                driver.switchTo().window(windowHandle);
                break;
            }
        }
    }

    // Scroll to the element for better visibility
    private void scrollToElement(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
    }

    @AfterClass
    public void tearDown() {
        driver.quit();
    }
}
