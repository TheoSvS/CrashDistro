package org.example;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.HasDevTools;
import org.openqa.selenium.devtools.v131.network.Network;
import org.openqa.selenium.devtools.v131.network.model.Request;
import org.openqa.selenium.devtools.v131.runtime.Runtime;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CrawlHistoryPanel {
    static WebDriver driver;
    static ExecutorService executorService = Executors.newCachedThreadPool();

    //tracks casino's balance changes since the program was running
    private BigDecimal currentEnemyBalance = BigDecimal.ZERO;

    private Long latestRound = 0L;
    private List<BigDecimal> crashLevelsSinceStart = new ArrayList<>();

    public CrawlHistoryPanel() throws InterruptedException {
        ChromeOptions options = new ChromeOptions();

// 1. Remove the �enable-automation� flag
        options.setExperimentalOption("excludeSwitches",
                Arrays.asList("enable-automation"));

// 2. Hide the �Chrome is being controlled by automated test software� infobar
        options.setExperimentalOption("useAutomationExtension", false);

// 3. Potentially override user agent
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                + "AppleWebKit/537.36 (KHTML, like Gecko) "
                + "Chrome/117.0.0.0 Safari/537.36");

// 4. �AutomationControlled� � some older advice suggests you might remove it:
        options.addArguments("--disable-blink-features=AutomationControlled");

        //driver = new ChromeDriver();
        driver = new ChromeDriver(options);

        WebDriverManager.chromedriver().setup();
        // Disable DevTools info logs
        Logger.getLogger("org.openqa.selenium.devtools").setLevel(Level.SEVERE);

        driver.get("https://crashout.fun/en-us/solana");

        closeBanner(); //close initial banner
    }

    public void startCrawling() {
        addNewRoundListener();
    }

    /**
     * Finds when a new round is about to start by checking the start game icon request
     */
    private void addNewRoundListener() {
        // Cast driver to HasDevTools to access DevTools
        DevTools devTools = ((HasDevTools) driver).getDevTools();
        devTools.createSession();

        // Enable Network events
        devTools.send(Network.enable(
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
        ));

        // Add a listener for 'requestWillBeSent'
        // Potentially inject script to redefine navigator.webdriver


        devTools.send(
                Runtime.evaluate(
                        "Object.defineProperty(navigator, 'webdriver', {get: () => undefined})",
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                )
        );

        devTools.addListener(Network.requestWillBeSent(),
                requestWillBeSent -> {
                    Request request = requestWillBeSent.getRequest();
                    String url = request.getUrl();

                    // Check if its the icon-sending request (signifying the end of game round)
                    if (url.contains("icon-sending")) {
                        storeEnemyBankBalanceChange();
                        calculateOutputWinningRatios();
                    }
                }
        );
    }

    private void storeEnemyBankBalanceChange() {
        //Locate all row elements under recordmain:
        List<WebElement> rowElements = driver.findElements(
                By.cssSelector("div.recordmain div.item")
        );

        List<PlayerRoundStats> records = new ArrayList<>();

        // For each row select .r2, .r3, .r4
        for (WebElement row : rowElements) {
            String betAmount = row.findElement(By.cssSelector(".r2")).getText();
            String totalOutput = row.findElement(By.cssSelector(".r3")).getText();
            String cashoutLevel = row.findElement(By.cssSelector(".r4")).getText();

            // Create record object
            PlayerRoundStats record = new PlayerRoundStats(betAmount, totalOutput, cashoutLevel);
            records.add(record);
        }
        BigDecimal enemyBankBalanceChange = records.stream().map(rec -> new BigDecimal(rec.betAmount()).subtract(new BigDecimal(rec.totalOutput()))).reduce(BigDecimal.ZERO, BigDecimal::add);
        currentEnemyBalance = currentEnemyBalance.add(enemyBankBalanceChange);
        Utils.storeEnemyBankBalance(currentEnemyBalance, enemyBankBalanceChange);
    }

    private void calculateOutputWinningRatios() {
        try {
            Optional<CrashLevels> crashLevelsOpt = crawlHistory();
            Utils.storeBettingOutputs(crashLevelsOpt, new BigDecimal("158"));
            Utils.storeBettingOutputs(crashLevelsOpt, new BigDecimal("200"));
            Utils.storeBettingOutputs(crashLevelsOpt, new BigDecimal("300"));
            Utils.storeBettingOutputs(crashLevelsOpt, new BigDecimal("400"));
            Utils.storeBettingOutputs(crashLevelsOpt, new BigDecimal("500"));
            Utils.storeBettingOutputs(crashLevelsOpt, new BigDecimal("600"));
            Utils.storeBettingOutputs(crashLevelsOpt, new BigDecimal("700"));
            Utils.storeBettingOutputs(crashLevelsOpt, new BigDecimal("800"));
            Utils.storeBettingOutputs(crashLevelsOpt, new BigDecimal("900"));
            Utils.storeBettingOutputs(crashLevelsOpt, new BigDecimal("1000"));
            Utils.storeBettingOutputs(crashLevelsOpt, new BigDecimal("1200"));
            Utils.storeBettingOutputs(crashLevelsOpt, new BigDecimal("1400"));
            Utils.storeBettingOutputs(crashLevelsOpt, new BigDecimal("1700"));
            Utils.storeBettingOutputs(crashLevelsOpt, new BigDecimal("2000"));
            Utils.storeBettingOutputs(crashLevelsOpt, new BigDecimal("2500"));
            Utils.storeBettingOutputs(crashLevelsOpt, new BigDecimal("3000"));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    public Optional<CrashLevels> crawlHistory() throws InterruptedException {
        CrashLevels crashLevels;
        try {
            openHistoryPanel();
        } catch (Exception e) {
            WebElement maskElement = driver.findElement(By.cssSelector("div.mask.fade-enter-from.fade-leave-from.fade-leave-active"));
            // Retrieve the complete HTML (including the element itself)
            String fullHtml = maskElement.getAttribute("outerHTML");
            Utils.storeBetOutputData(BigDecimal.ONE, fullHtml);
            System.exit(4);
        }
        try {
            awaitUntilCrashValuesExist();
        } catch (Exception e) {
            closeHistoryPanel(); //sometimes history Panel is loaded empty so catch the exception and close the history again.
            System.err.println("Loaded empty history panel, closing again");
            return Optional.empty(); //empty
        }
        crashLevels = collectCrashValues();
        closeHistoryPanel();

        return Optional.of(crashLevels);
    }

    /**
     * close history panel asynchronously after collecting data with a random delay to make it seem more "humanlike"
     */
    private void closeHistoryPanel() {
        executorService.execute(() ->
        {
            try {
                Thread.sleep(getRandom(2, 5) * 1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            WebElement closeButton = driver.findElement(By.cssSelector(".header .close"));
            closeButton.click();
        });
    }

//    private List<BigDecimal> collectCrashValues() {
//        List<BigDecimal> crashData = new ArrayList<>();
//        List<WebElement> crashElements = driver.findElements(By.cssSelector(".r_item .td:nth-child(4)")); //get the crash values from rows
//        for (int i = 0; i < crashElements.size(); i++) {
//            String crashVal = crashElements.get(i).getText();
//            if (crashVal.toLowerCase().contains("progress")) {
//                continue; // Sometimes IN PROGRESS game is already loaded so skip it from parsing
//            }
//
//            crashData.add(new BigDecimal(crashVal.replace("%", "")));
//        }
//        return crashData;
//    }


    private CrashLevels collectCrashValues() {
        List<BigDecimal> last100CrashLevels = new ArrayList<>();
        List<WebElement> rows = driver.findElements(By.cssSelector(".r_item"));
        for (WebElement row : rows) {
            List<WebElement> cells = row.findElements(By.cssSelector(".td"));
            String crashValueStr = cells.get(3).getText().replace("%", "");
            String roundStr = cells.get(4).getText();
            if (crashValueStr.toLowerCase().contains("progress")) {
                continue; // Sometimes IN PROGRESS game is already loaded so skip it from parsing
            }
            BigDecimal crashValue = new BigDecimal(crashValueStr);
            Long round = Long.parseLong(roundStr);
            setLatestRound(round, crashValue);
            last100CrashLevels.add(crashValue);
        }
        return new CrashLevels(last100CrashLevels, crashLevelsSinceStart);
    }

    private void setLatestRound(Long round, BigDecimal crashValue) {
        if (round > latestRound) {
            latestRound = round;
            crashLevelsSinceStart.add(crashValue);
        }
    }

    //String latestRndStr = crashElements.get(i).getText();
    //latestRound

    private void openHistoryPanel() {
        awaitInterferingMask();
        // Locate and click the button to open the popup
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10)); // 100-ms timeout
        WebElement historyButton = wait.until(ExpectedConditions.presenceOfElementLocated(By.className("i_his")));
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".icon.i_his")));
        historyButton.click();
    }

    private void awaitInterferingMask() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        //Check if the fade mask is currently present
        List<WebElement> fadeMasks = driver.findElements(
                By.cssSelector("div.mask.fade-enter-from, div.mask.fade-leave-from, div.mask.fade-leave-active")
        );

        //If found, wait for it to go away
        if (!fadeMasks.isEmpty()) {
            //wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("div.mask")));
            wait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.cssSelector("div.mask.fade-enter-from, div.mask.fade-leave-from, div.mask.fade-leave-active")
            ));
        }
    }

    private void awaitUntilCrashValuesExist() {
        //Thread.sleep(getRandom(2,5)*1000);
        WebDriverWait crashValuesWait = new WebDriverWait(driver, Duration.ofSeconds(10));
        crashValuesWait.until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver webDriver) {
                List<WebElement> crashValues = webDriver.findElements(
                        By.cssSelector(".r_item .td:nth-child(4)"));

                //ensure the list isn't empty
                if (crashValues.isEmpty()) {
                    return false;  // Keep waiting
                }
                // Check each element for non-empty text
                for (WebElement crashValue : crashValues) {
                    if (crashValue.getText().trim().isEmpty()) {
                        return false;  // At least one is empty, keep waiting
                    }
                }
                // All elements are non-empty
                return true;
            }
        });
    }

    private void closeBanner() throws InterruptedException {
        // close initial banner with a random delay to make interaction seem more "humanlike"
        Thread.sleep(getRandom(3, 5) * 1000);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10)); // 100-ms timeout
        WebElement closeButton = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("i.icon-close")));

        // Click the close button to dismiss the banner
        closeButton.click();

    }

    public static int getRandom(int from, int to) {
        //to is exclusive
        return ThreadLocalRandom.current().nextInt(from, to + 1);
    }

    public BigDecimal getCurrentEnemyBalance() {
        return currentEnemyBalance;
    }

    public void setCurrentEnemyBalance(BigDecimal currentEnemyBalance) {
        this.currentEnemyBalance = currentEnemyBalance;
    }

    //class="mask fade-enter-from fade-leave-from fade-leave-active">

    // Locate the panel and extract the content
    //By historyBoardLocatedBy = By.cssSelector(".mask .main.pc");
    //WebDriverWait waitHistoryPage = new WebDriverWait(driver, Duration.ofMillis(1000)); // 1000-ms timeout
    //WebElement mainPcElement = waitHistoryPage.until(ExpectedConditions.presenceOfElementLocated(historyBoardLocatedBy));
    // Get the full HTML of the element
    //String elementHTML = mainPcElement.getAttribute("outerHTML");
    //String rawData = mainPcElement.getText();
}

