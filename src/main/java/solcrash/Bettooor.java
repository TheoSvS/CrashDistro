package solcrash;

import org.apache.commons.codec.DecoderException;
import org.p2p.solanaj.core.Account;
import org.p2p.solanaj.core.Transaction;
import org.p2p.solanaj.rpc.RpcException;

import java.math.BigDecimal;
import java.util.List;

public class Bettooor {
    Account fromAccount;
    DecisionMaker decisionMaker;

    public Bettooor() {
        fromAccount = AppConstants.getFromAccount();
        decisionMaker = new DecisionMakerImpl();
    }

    //TODO: INSERT YOUR CONDITIONAL STRATEGY ON WHEN TO BET, BASED ON THE STATS THE PROGRAM IS COLLECTING
    public void doConditionalBet(long newStartingRound, List<BigDecimal> crashLevelsSinceStart) {
        try {
            double solBalance = CryptoUtils.getSolanaBalance(fromAccount.getPublicKey());
            if (solBalance < 0.1) { //claim balance from winnings so we can keep betting
                claimExistingBalance();
            }
            if(decisionMaker.shouldBet(crashLevelsSinceStart)) {
                doBet(newStartingRound, 0.02); //min 0.02 some hidden fee throws 0x6e TODO: INSERT YOUR CONDITIONAL STRATEGY ON WHEN TO BET, BASED ON THE STATS THE PROGRAM IS COLLECTING
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void claimExistingBalance() throws DecoderException, RpcException {
        Transaction claimTransaction = CrashGameInstructionBuilder.buildClaimTransaction();
        String signature = AppConstants.getClient().getApi().sendTransaction(claimTransaction, fromAccount);
        System.out.println("Claim transaction signature: " + signature);
    }

    private void doBet(long newStartingRound,double solBet) throws DecoderException, RpcException {
        Transaction claimTransaction = CrashGameInstructionBuilder.buildBetTransaction(newStartingRound,solBet);
        String signature = AppConstants.getClient().getApi().sendTransaction(claimTransaction, fromAccount);
        System.out.println("Bet transaction signature: " + signature);
    }
}
