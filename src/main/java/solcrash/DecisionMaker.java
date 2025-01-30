package solcrash;

import java.math.BigDecimal;
import java.util.List;

public interface DecisionMaker {


    /**
     * Implement and override with your own betting decision strategy
     * @return Whether the bot should make the bet
     */
    default boolean shouldBet(List<BigDecimal> crashLevelsSinceStart){
        return true;
    }
}
