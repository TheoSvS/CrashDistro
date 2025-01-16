package org.example;

import lombok.Getter;
import lombok.Setter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.*;
import org.openqa.selenium.TimeoutException;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.example.DataUtils.getTime;

public class CrashGameMonitor {
    @Getter
    private WebDriver driver;
    @Getter
    private DevTools devTools;
    @Getter
    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);
    private Bettooor bettooor = new Bettooor();
    static final String gameUrl = "https://crashout.fun/en-us/solana";
    //tracks casino's balance changes since the program was running
    @Getter
    @Setter
    private BigDecimal currentEnemyBalance = BigDecimal.ZERO;

    private Long finishedRound = 0L;
    private Long newStartingRound = 0L;
    private List<BigDecimal> crashLevelsSinceStart = new ArrayList<>();
    private int finishedRoundPlayers;

    public CrashGameMonitor() throws InterruptedException {
        initChromeDriver();
        initDevTools();

        closeBanner(); //close initial banner
    }

    public void startMonitoring() {
        addNewRoundListener();
    }

    /**
     * Finds when a new round is about to start by checking the start game icon request
     */
    private void addNewRoundListener() {
        devTools.addListener(Network.requestWillBeSent(),
                requestWillBeSent -> {
                    Request request = requestWillBeSent.getRequest();
                    String url = request.getUrl();
                    // Check if its the icon-sending request (just the moment the round ends)
                    if (url.contains("icon-sending")) {
                        storeEnemyBankBalanceChangeFromLastRound();
                    } else if (url.contains("icon-handout")) { //as the round counter has incremented to new round
                        Optional<CrashData> crashDataOpt = crawlHistory();
                        conditionalBetAsync(newStartingRound);
                        storeRoundOutputs(crashDataOpt);
                    }
                }
        );
    }

    private void storeEnemyBankBalanceChangeFromLastRound() {
        WebElement playerPanelElement = driver.findElement(By.cssSelector("div.recordmain"));
        String playerPanelHtml = playerPanelElement.getAttribute("innerHTML");

        Document document = Jsoup.parse(playerPanelHtml);
        Elements rowElements = document.select("div.item");
        finishedRoundPlayers = rowElements.size();

        List<PlayerRoundStats> records = new ArrayList<>();
        // For each row select .r2, .r3, .r4
        for (Element row : rowElements) {
            String betAmount = row.select(".r2").text();
            String totalOutput = row.select(".r3").text();
            String cashoutLevel = row.select(".r4").text();

            // Create record object
            PlayerRoundStats record = new PlayerRoundStats(betAmount, totalOutput, cashoutLevel);
            records.add(record);
        }
        BigDecimal enemyBankBalanceChange = records.stream().map(rec -> new BigDecimal(rec.betAmount()).subtract(new BigDecimal(rec.totalOutput()))).reduce(BigDecimal.ZERO, BigDecimal::add);
        currentEnemyBalance = currentEnemyBalance.add(enemyBankBalanceChange);
        DataUtils.storeEnemyBankBalance(currentEnemyBalance, enemyBankBalanceChange);
    }

    private void storeRoundOutputs(Optional<CrashData> crashDataOpt) {
        if (crashDataOpt.isEmpty()) {
            return;
        }
        CrashData crashData = crashDataOpt.get();
        DataUtils.storeToFile(Paths.get("outputs", "CrashHistory"),
                getTime() + " === " + crashData.crashLevelsSinceStart().getLast().intValue() + System.lineSeparator()); // Create a Path object for the file
        DataUtils.storeBettingOutputs(crashData, new BigDecimal("158"));
        DataUtils.storeBettingOutputs(crashData, new BigDecimal("200"));
        DataUtils.storeBettingOutputs(crashData, new BigDecimal("300"));
        DataUtils.storeBettingOutputs(crashData, new BigDecimal("400"));
        DataUtils.storeBettingOutputs(crashData, new BigDecimal("500"));
        DataUtils.storeBettingOutputs(crashData, new BigDecimal("600"));
        DataUtils.storeBettingOutputs(crashData, new BigDecimal("700"));
        DataUtils.storeBettingOutputs(crashData, new BigDecimal("800"));
        DataUtils.storeBettingOutputs(crashData, new BigDecimal("900"));
        DataUtils.storeBettingOutputs(crashData, new BigDecimal("1000"));
        DataUtils.storeBettingOutputs(crashData, new BigDecimal("1200"));
        DataUtils.storeBettingOutputs(crashData, new BigDecimal("1400"));
        DataUtils.storeBettingOutputs(crashData, new BigDecimal("1700"));
        DataUtils.storeBettingOutputs(crashData, new BigDecimal("2000"));
        DataUtils.storeBettingOutputs(crashData, new BigDecimal("2500"));
        DataUtils.storeBettingOutputs(crashData, new BigDecimal("3000"));
        DataUtils.storeBettingOutputs(crashData, new BigDecimal("3000"));
    }


    public Optional<CrashData> crawlHistory() {
        CrashData crashData;
        try {
            openHistoryPanel();
        } catch (Exception e) {
            WebElement maskElement = driver.findElement(By.cssSelector("div.mask.fade-enter-from.fade-leave-from.fade-leave-active"));
            // If the interfering mask still appears, retrieve the complete HTML (including the element itself) to study it
            String fullHtml = maskElement.getAttribute("outerHTML");
            DataUtils.storeBetOutputsToFile(BigDecimal.ONE, fullHtml);
            System.exit(4);
        }
        try {
            awaitUntilCrashValuesExist();
        } catch (Exception e) {
            closeHistoryPanelAsync(); //sometimes history Panel is loaded empty so catch the exception and close the history again.
            System.err.println("Loaded empty history panel, closing again");
            return Optional.empty(); //empty
        }
        crashData = collectCrashValues();
        closeHistoryPanelAsync();
        return Optional.of(crashData);
    }

    private void conditionalBetAsync(long newStartingRound) {
        //scheduled executor. Give me a few seconds to think if I want to change the bettingEnabled property for the following rounds..
        executorService.schedule(() -> bettooor.doConditionalBet(newStartingRound), 13, TimeUnit.SECONDS);
    }

    /**
     * close history panel asynchronously after collecting data with a random delay to make it seem more "humanlike"
     */
    private void closeHistoryPanelAsync() {
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

    private CrashData collectCrashValues() {
        List<BigDecimal> last100CrashLevels = new ArrayList<>();
        List<WebElement> rows = driver.findElements(By.cssSelector(".r_item"));
        for (WebElement row : rows) {
            List<WebElement> cells = row.findElements(By.cssSelector(".td"));
            String crashValueStr = cells.get(3).getText().replace("%", "");
            String roundStr = cells.get(4).getText();
            if (crashValueStr.toLowerCase().contains("progress")) {
                newStartingRound = Long.parseLong(roundStr);
                continue; // IN PROGRESS is the newly starting round, no crashData to process yet
            }
            BigDecimal crashValue = new BigDecimal(crashValueStr);
            Long round = Long.parseLong(roundStr);
            if (round > finishedRound) { //from the history panel of rounds, add only the latest finished round to the crashLevels since start list
                addFinishedRoundCrashData(round, crashValue);
            }
            last100CrashLevels.add(crashValue);
        }
        return new CrashData(last100CrashLevels, crashLevelsSinceStart, finishedRoundPlayers);
    }

    //from the history panel of rounds, add only the latest finished round to the crashLevels since start list
    private void addFinishedRoundCrashData(Long round, BigDecimal crashValue) {
        finishedRound = round;
        crashLevelsSinceStart.add(crashValue);
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
            try {
                wait.until(ExpectedConditions.invisibilityOfElementLocated(
                        By.cssSelector("div.mask.fade-enter-from, div.mask.fade-leave-from, div.mask.fade-leave-active")
                ));
            } catch (TimeoutException e) {
                System.err.println("You little pest..");
                // If it never disappeared within 5 seconds, remove/hide it via JavaScript
                JavascriptExecutor js = (JavascriptExecutor) driver;
                for (WebElement mask : fadeMasks) {
                    js.executeScript("arguments[0].style.display='none';", mask);
                    // Remove it from the DOM altogether
                    // js.executeScript("arguments[0].remove();", mask);
                }
            }
        }
    }

    private void awaitUntilCrashValuesExist() {
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


    private void initDevTools() {
        // Cast driver to HasDevTools to access DevTools
        devTools = ((HasDevTools) driver).getDevTools();
        devTools.createSession();

        // Enable Network events
        devTools.send(Network.enable(
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
        ));

        // Add a listener for 'requestWillBeSent'
        // Inject script to redefine navigator.webdriver and obfuscate this is an automation robot
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
    }

    private void initChromeDriver() {
        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("excludeSwitches",
                Arrays.asList("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                + "AppleWebKit/537.36 (KHTML, like Gecko) "
                + "Chrome/117.0.0.0 Safari/537.36");
        options.addArguments("--disable-blink-features=AutomationControlled");

        driver = new ChromeDriver(options);
        WebDriverManager.chromedriver().setup();
        // Disable DevTools info logs
        Logger.getLogger("org.openqa.selenium.devtools").setLevel(Level.SEVERE);
        driver.get(gameUrl);
    }
}

