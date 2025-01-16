package org.example;

import org.apache.commons.codec.DecoderException;
import org.p2p.solanaj.core.Account;
import org.p2p.solanaj.core.Transaction;
import org.p2p.solanaj.rpc.RpcException;

public class Bettooor {
    Account fromAccount;

    public Bettooor() {
        fromAccount = AppConstants.getFromAccount();
    }

    //TODO: INSERT YOUR CONDITIONAL STRATEGY ON WHEN TO BET, BEST ON THE STATS THE PROGRAM IS COLLECTING
    public void doConditionalBet(long newStartingRound) {
        AppConstants.liveReadProperties();
        if (!AppConstants.isBettingEnabled()) {
            return;
        }
        try {
            double solBalance = CryptoUtils.getSolanaBalance(fromAccount.getPublicKey());
            if (solBalance < 0.1) { //claim balance from winnings so we can keep betting
                claimExistingBalance();
            }

            doBet(newStartingRound,0.01); //TODO: INSERT YOUR CONDITIONAL STRATEGY ON WHEN TO BET, BEST ON THE STATS THE PROGRAM IS COLLECTING
        } catch (RpcException | DecoderException e) {
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
