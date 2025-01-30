package solcrash;

import lombok.Getter;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import solcrash.model.BetOutputMessages;
import solcrash.model.CrashData;
import solcrash.model.LastXWinOutput;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataUtils {

//For desired cashout levels, this concurrent hashmap holds the lastX games winning outputs for the selected history depth
private static final int HISTORICAL_ROUNDS_DEPTH=50;
@Getter
private static final  ConcurrentHashMap<BigDecimal,Map<Integer,CircularFifoQueue<Double>>> lastXGamesWinOutputsPerCashoutLvl = new ConcurrentHashMap<>();

    static void storeBettingOutputs(CrashData crashData, BigDecimal cashOutLvL) {
        List<BigDecimal> crashLevelsSinceStart = crashData.crashLevelsSinceStart();
        List<BigDecimal> last100 = crashData.last100CrashLevels();
        List<BigDecimal> last30 = last100.subList(0, 30);

        LastXWinOutput allSinceStartWinOutput = getLastXwinningOutputs(crashLevelsSinceStart, cashOutLvL);
        LastXWinOutput last100WinOutput = getLastXwinningOutputs(last100, cashOutLvL);
        LastXWinOutput last30WinOutput = getLastXwinningOutputs(last30, cashOutLvL);

        BetOutputMessages allSinceStartOutMessages = calculateWinningOutputMessages(allSinceStartWinOutput, crashLevelsSinceStart.size());
        BetOutputMessages last10OutMessages = calculateWinningOutputMessages(last100WinOutput, last100.size());
        BetOutputMessages last3OutMessages = calculateWinningOutputMessages(last30WinOutput, last30.size());


        String printableCrashLevelsSinceStart = String.format("%-37s", allSinceStartOutMessages.output() + allSinceStartOutMessages.winRatio());
        String printable100 = String.format("%-43s", last10OutMessages.output() + last10OutMessages.winRatio());
        String printable30 = last3OutMessages.output() + " " + last3OutMessages.winRatio();

        storeBetOutputsToFile(cashOutLvL, getTime() + " =Plrs:" + crashData.finishedRoundPlayers()+"=  ");
        storeBetOutputsToFile(cashOutLvL, printableCrashLevelsSinceStart + "   " + printable100 + "   " + printable30 + System.lineSeparator());

        storeBetOutputsToMap(cashOutLvL, last30WinOutput ,30);
        storeBetOutputsToMap(cashOutLvL, last100WinOutput,100);

    }

    private static BetOutputMessages calculateWinningOutputMessages(LastXWinOutput lastXWinOutput, int lastXrounds) {
        DecimalFormat df = new DecimalFormat("#.##");
        BetOutputMessages betOutputs = new BetOutputMessages(
                "Last" + lastXrounds + ": " + df.format(lastXWinOutput.winOutput()) + "x",
                " (W ratio " + df.format(lastXWinOutput.winRatio()) + "  W: " + lastXWinOutput.wins() + "/L:" + lastXWinOutput.losses() + ")");
        return betOutputs;
    }

    private static void storeBetOutputsToMap(BigDecimal cashOutLvL, LastXWinOutput lastXWinOutput, Integer xRounds){
        lastXGamesWinOutputsPerCashoutLvl.putIfAbsent(cashOutLvL, new HashMap<>());
        lastXGamesWinOutputsPerCashoutLvl.get(cashOutLvL).putIfAbsent(xRounds, new CircularFifoQueue<>(HISTORICAL_ROUNDS_DEPTH));
        lastXGamesWinOutputsPerCashoutLvl.get(cashOutLvL).get(xRounds).add(lastXWinOutput.winOutput());
    }

    private static @NotNull LastXWinOutput getLastXwinningOutputs(List<BigDecimal> lastXrounds, BigDecimal cashOutLvL) {
        long losses = lastXrounds.stream().filter(e -> e.compareTo(cashOutLvL) < 0).count();
        long wins = lastXrounds.stream().filter(e -> e.compareTo(cashOutLvL) >= 0).count();

        double winRatio = (double) wins / losses;
        double feesTotal = lastXrounds.size() * 0.03;
        double output = wins * ((cashOutLvL.intValue() - 100) / 100.0) - losses - feesTotal;
        LastXWinOutput lastXWinOutput = new LastXWinOutput(losses, wins, winRatio, output);
        return lastXWinOutput;
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
            // Create or confirm the winOutput parent directory exists
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
