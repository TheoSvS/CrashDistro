package solcrash;

public class DecisionMakerImpl implements DecisionMaker{

    public DecisionMakerImpl() {
    }

    @Override
    public boolean shouldBet() { //Implement your own strategy
        return DecisionMaker.super.shouldBet();
    }
}
