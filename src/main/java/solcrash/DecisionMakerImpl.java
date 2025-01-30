package solcrash;

import lombok.Getter;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import solcrash.model.FallingWindowMinMax;
import solcrash.model.RisingWindowMinMax;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class DecisionMakerImpl implements DecisionMaker {

    public static final int RISING_TREND_THRESHOLD = 8; // threshold between minimum peak and the maximum peak that follow it to be considered substantial for an uptrend
    public static final int FALLING_TREND_THRESHOLD = 8; // threshold between minimum peak and the maximum peak that follow it to be considered substantial for an uptrend
    public static final int NOISE_CUTOFF = 5; //cuttoff minor back and forth in possible uptrend
    public static final int L_TERM_RUNWAY_THRESHOLD = -20; //current winning threshold so we have room to run
    public static final int S_TERM_RUNWAY_THRESHOLD = -10; //current winning threshold so we have room to run

    @Getter
    private boolean longTermBettingEngaged = false;
    @Getter
    private boolean shortTermBettingEngaged = false;

    public DecisionMakerImpl() {
    }

    @Override
    public boolean shouldBet(List<BigDecimal> crashLevelsSinceStart) {
        AppConstants.liveReadProperties();
        EBetMode eBetMode = AppConstants.getEBetMode();
        switch (eBetMode) {
            case OFF -> {
                longTermBettingEngaged = false;
                shortTermBettingEngaged = false;
                return false;
            }
            case ON -> {
                longTermBettingEngaged = true; //allow to keep going if we change to auto
                shortTermBettingEngaged = true;
                return true;
            }
            case AUTO -> {
                if (!longTermBettingEngaged && !shortTermBettingEngaged) {
                    evaluateShouldStartBetting();
                } else {
                    evaluateShouldStopBetting(crashLevelsSinceStart);
                }
                return longTermBettingEngaged || shortTermBettingEngaged;
            }
        }
        return false;
    }


    /**
     * Scan the Queue for the minimum win output peak, then look ahead for reversal clues
     *
     * @return should make the bet or not
     */
    private void evaluateShouldStartBetting() { //Implement your own strategy
        //for the chosen cashout level, get the win outputs per the last X games. (e.g. win outputs from last 30 games or last 100 games)
        BigDecimal cashOutLevel = BigDecimal.valueOf(AppConstants.getECashoutLvl().getCashoutLvl());
        Map<Integer, CircularFifoQueue<Double>> winOutputsPerLastXGames = DataUtils.getLastXGamesWinOutputsPerCashoutLvl().get(cashOutLevel);
        CircularFifoQueue<Double> last100GamesOutputs = winOutputsPerLastXGames.get(100);
        CircularFifoQueue<Double> last30GamesOutputs = winOutputsPerLastXGames.get(30);

        asLongTermBetting(last100GamesOutputs);
        asShortTermBetting(last30GamesOutputs);
    }

    private void asLongTermBetting(CircularFifoQueue<Double> last100GamesOutputs) {
        if (!last100GamesOutputs.isEmpty() && last100GamesOutputs.size() >= 10) {
            RisingWindowMinMax decisionWindow100MinMax = new RisingWindowMinMax();
            populateWindowMinPeak(decisionWindow100MinMax, last100GamesOutputs);
            poulateWindowMaxAfterMinPeak(decisionWindow100MinMax, last100GamesOutputs);
            if (isWindowRisingTrend(decisionWindow100MinMax, last100GamesOutputs, L_TERM_RUNWAY_THRESHOLD)) {
                longTermBettingEngaged = true;
            }
        }
    }

    private void asShortTermBetting(CircularFifoQueue<Double> last30GamesOutputs) {
        if (!last30GamesOutputs.isEmpty() && last30GamesOutputs.size() >= 10) {
            RisingWindowMinMax decisionWindow30MinMax = new RisingWindowMinMax();
            populateWindowMinPeak(decisionWindow30MinMax, last30GamesOutputs);
            poulateWindowMaxAfterMinPeak(decisionWindow30MinMax, last30GamesOutputs);
            if (isWindowRisingTrend(decisionWindow30MinMax, last30GamesOutputs, S_TERM_RUNWAY_THRESHOLD)) {
                longTermBettingEngaged = true;
            }
        }
    }


    private void evaluateShouldStopBetting(List<BigDecimal> crashLevelsSinceStart) {
        BigDecimal cashOutLevel = BigDecimal.valueOf(AppConstants.getECashoutLvl().getCashoutLvl());
        Map<Integer, CircularFifoQueue<Double>> winOutputsPerLastXGames = DataUtils.getLastXGamesWinOutputsPerCashoutLvl().get(cashOutLevel);
        CircularFifoQueue<Double> last100GamesOutputs = winOutputsPerLastXGames.get(100);
        CircularFifoQueue<Double> last30GamesOutputs = winOutputsPerLastXGames.get(30);

        double shortTermCurrentVal = last30GamesOutputs.get(0);
        double longTermCurrentVal = last100GamesOutputs.get(0);

        //resets to stop betting, useful if we had previously identified rising trend wrongly
        if (!last100GamesOutputs.isEmpty() && last100GamesOutputs.size() >= 10) {
            FallingWindowMinMax fallingWindow100MinMax = new FallingWindowMinMax();
            populateWindowMaxPeak(fallingWindow100MinMax, last100GamesOutputs);
            poulateWindowMinAfterMaxPeak(fallingWindow100MinMax, last100GamesOutputs);
            if (isWindowFallingTrend(fallingWindow100MinMax, last100GamesOutputs)) {
                longTermBettingEngaged = false;
                shortTermBettingEngaged = false; //check if wanna split logic for short/long term stop betting
            }
        }


        //aggressive stop at perceived blow off top following rising scenario up until now
        if (shortTermCurrentVal > 20 && longTermCurrentVal > 20 && hadXLossesInRow(3, crashLevelsSinceStart)) {
            longTermBettingEngaged = false;
            shortTermBettingEngaged = false;
        }
    }


    /*********************************************************
     * ****************RISING TREND METHODS*********************
     * *******************************************************
     */
    private void populateWindowMinPeak(RisingWindowMinMax risingWindowMinMax, CircularFifoQueue<Double> lastXGamesOutputs) {
        int minPeakIdx = 0;
        double minPeakVal = lastXGamesOutputs.get(minPeakIdx);

        for (int i = 0; i < lastXGamesOutputs.size(); i++) { //traverse from the oldest historical data to the newest
            if (lastXGamesOutputs.get(i) < minPeakVal) {
                minPeakVal = lastXGamesOutputs.get(i);
                minPeakIdx = i;
            }
        }
        risingWindowMinMax.setMinPeakVal(minPeakVal);
        risingWindowMinMax.setMinPeakIdx(minPeakIdx);
    }


    private void poulateWindowMaxAfterMinPeak(RisingWindowMinMax risingWindowMinMax, CircularFifoQueue<Double> lastXGamesOutputs) {
        //Find the maximum that is AFTER the minimum peak
        int maxAfterMinPeakIdx = risingWindowMinMax.getMinPeakIdx();
        double maxAfterMinPeakVal = risingWindowMinMax.getMinPeakVal();
        for (int i = risingWindowMinMax.getMinPeakIdx(); i < lastXGamesOutputs.size(); i++) { //traverse from the minPeak index to the newest
            if (lastXGamesOutputs.get(i) > maxAfterMinPeakVal) {
                maxAfterMinPeakVal = lastXGamesOutputs.get(i);
                maxAfterMinPeakIdx = i;
            }
        }
        risingWindowMinMax.setMaxAfterMinPeakIdx(maxAfterMinPeakIdx);
        risingWindowMinMax.setMaxAfterMinPeakVal(maxAfterMinPeakVal);
    }

    private boolean isWindowRisingTrend(RisingWindowMinMax risingWindowMinMax, CircularFifoQueue<Double> lastXGamesOutputs, int runwayThreshold) {
        double minMaxPeakDiff = risingWindowMinMax.getMaxAfterMinPeakVal() - risingWindowMinMax.getMinPeakVal();
        double currentVal = lastXGamesOutputs.get(lastXGamesOutputs.size() - 1);
        double minorReversalDiff = risingWindowMinMax.getMaxAfterMinPeakVal() - currentVal; // establish that temporary "noise" that backtracks in an uptrend is ignored but larger is a sign of downtrend begin
        if (minMaxPeakDiff >= RISING_TREND_THRESHOLD && minorReversalDiff < NOISE_CUTOFF && currentVal < runwayThreshold) {
            return true;
        }
        return false;
    }


    /*********************************************************
     * ****************FALLING TREND METHODS*********************
     * *******************************************************
     */

    private boolean hadXLossesInRow(int lossesInRow, List<BigDecimal> crashLevelsSinceStart) {
        return crashLevelsSinceStart.subList(crashLevelsSinceStart.size() - lossesInRow, crashLevelsSinceStart.size())
                .stream()
                .allMatch(crash -> crash.doubleValue() < AppConstants.getECashoutLvl().getCashoutLvl());
    }

    private void populateWindowMaxPeak(FallingWindowMinMax fallingWindowMinMax, CircularFifoQueue<Double> lastXGamesOutputs) {
        int maxPeakIdx = 0;
        double maxPeakVal = lastXGamesOutputs.get(maxPeakIdx);

        for (int i = 0; i < lastXGamesOutputs.size(); i++) { //traverse from the oldest historical data to the newest
            if (lastXGamesOutputs.get(i) > maxPeakVal) {
                maxPeakVal = lastXGamesOutputs.get(i);
                maxPeakIdx = i;
            }
        }
        fallingWindowMinMax.setMaxPeakVal(maxPeakVal);
        fallingWindowMinMax.setMaxPeakIdx(maxPeakIdx);
    }


    private void poulateWindowMinAfterMaxPeak(FallingWindowMinMax fallingWindowMinMax, CircularFifoQueue<Double> lastXGamesOutputs) {
        //Find the minimum that is AFTER the maximum peak
        int minAfterMaxPeakIdx = fallingWindowMinMax.getMaxPeakIdx();
        double minAfterMaxPeakVal = fallingWindowMinMax.getMaxPeakVal();
        for (int i = fallingWindowMinMax.getMaxPeakIdx(); i < lastXGamesOutputs.size(); i++) { //traverse from maxPeak index to the newest
            if (lastXGamesOutputs.get(i) < minAfterMaxPeakVal) {
                minAfterMaxPeakVal = lastXGamesOutputs.get(i);
                minAfterMaxPeakIdx = i;
            }
        }
        fallingWindowMinMax.setMinAfterMaxPeakIdx(minAfterMaxPeakIdx);
        fallingWindowMinMax.setMinAfterMaxPeakVal(minAfterMaxPeakVal);
    }

    private boolean isWindowFallingTrend(FallingWindowMinMax fallingWindowMaxMin, CircularFifoQueue<Double> lastXGamesOutputs) {
        double maxMinPeakDiff = fallingWindowMaxMin.getMaxPeakVal() - fallingWindowMaxMin.getMinAfterMaxPeakVal();
        double currentVal = lastXGamesOutputs.get(lastXGamesOutputs.size() - 1);
        double minorReversalDiff = currentVal - fallingWindowMaxMin.getMinAfterMaxPeakVal(); // establish that temporary "noise" that pumps in a downtrend is ignored, but more than that can be uptrend again
        if (maxMinPeakDiff > FALLING_TREND_THRESHOLD && minorReversalDiff < NOISE_CUTOFF) {
            return true;
        }
        return false;
    }

}
