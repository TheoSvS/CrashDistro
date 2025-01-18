package solcrash;

public interface DecisionMaker {
    /**
     * Implement and override with your own betting decision strategy
     * @return Whether the bot should make the bet
     */
    default boolean shouldBet(){
        return true;
    }
}
