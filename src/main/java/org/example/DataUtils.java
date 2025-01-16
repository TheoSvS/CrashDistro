package org.example;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DataUtils {


    static void storeBettingOutputs(CrashData crashData, BigDecimal cashOutLvL) {
        List<BigDecimal> crashLevelsSinceStart = crashData.crashLevelsSinceStart();
        List<BigDecimal> last100 = crashData.last100CrashLevels();
        List<BigDecimal> last30 = last100.subList(0, 30);

        BetOutputMessages crashLevelsSinceStartOutputs = calculateWinningOutputs(crashLevelsSinceStart, cashOutLvL);
        BetOutputMessages last10Outputs = calculateWinningOutputs(last100, cashOutLvL);
        BetOutputMessages last3Outputs = calculateWinningOutputs(last30, cashOutLvL);


        String printableCrashLevelsSinceStart = String.format("%-37s", crashLevelsSinceStartOutputs.output() + crashLevelsSinceStartOutputs.winRatio());
        String printable100 = String.format("%-43s", last10Outputs.output() + last10Outputs.winRatio());
        String printable30 = last3Outputs.output() + " " + last3Outputs.winRatio();

        storeBetOutputsToFile(cashOutLvL, getTime() + " =Plrs:" + crashData.finishedRoundPlayers()+"=  ");
        storeBetOutputsToFile(cashOutLvL, printableCrashLevelsSinceStart + "   " + printable100 + "   " + printable30 + System.lineSeparator());
    }

    private static BetOutputMessages calculateWinningOutputs(List<BigDecimal> lastXrounds, BigDecimal cashOutLvL) {
        long losses = lastXrounds.stream().filter(e -> e.compareTo(cashOutLvL) < 0).count();
        long wins = lastXrounds.stream().filter(e -> e.compareTo(cashOutLvL) >= 0).count();

        double winRatio = (double) wins / losses;
        double feesTotal = lastXrounds.size() * 0.03;
        double output = wins * ((cashOutLvL.intValue() - 100) / 100.0) - losses - feesTotal;

        DecimalFormat df = new DecimalFormat("#.##");
        BetOutputMessages betOutputs = new BetOutputMessages(
                "Last" + lastXrounds.size() + ": " + df.format(output) + "x",
                " (W ratio " + df.format(winRatio) + "  W: " + wins + "/L:" + losses + ")");
        return betOutputs;
    }

    static void storeEnemyBankBalance(BigDecimal currentEnemyBalance, BigDecimal enemyBalanceChange) {
        //store the casino's bank balance change from this game
        Path filePath = Paths.get("outputs", "EnemyBalance"); // Create a Path object for the file
        String leftAlignedBalance = String.format("  %-6s", currentEnemyBalance + " SOL");
        String singedBalChange = (enemyBalanceChange.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") + enemyBalanceChange;
        String balanceMessage = leftAlignedBalance + "   (" + singedBalChange + " SOL)" + System.lineSeparator();
        storeToFile(filePath, getTime() + " ===");
        storeToFile(filePath, balanceMessage);
    }


    static void storeBetOutputsToFile(BigDecimal cashOutLvL, String textToAppend) {
        Path filePath = Paths.get("outputs", cashOutLvL.intValue() + "historicalCrashes"); // Create a Path object for the file
        storeToFile(filePath, textToAppend);
    }

    static void storeToFile(Path filePath, String textToAppend) {
        try {
            // Create or confirm the output parent directory exists
            Files.createDirectories(filePath.getParent());
            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
                System.out.println("File created: " + filePath.toAbsolutePath());
            }
            // Append text to the file
            Files.writeString(filePath, textToAppend, StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        return now.format(formatter);
    }
}
