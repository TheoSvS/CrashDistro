package solcrash;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.p2p.solanaj.core.PublicKey;
import org.p2p.solanaj.core.Transaction;
import org.p2p.solanaj.core.TransactionInstruction;

import static solcrash.AppConstants.*;

public class CrashGameInstructionBuilder {

    public static Transaction buildBetTransaction(long gameRound, double solBet) throws DecoderException {
        Transaction transaction = new Transaction();
        PublicKey crashProgramPublicKey = new PublicKey(CRASH_PROGRAM_ID);
        long totalBetLamports = CryptoUtils.solToLamports(solBet * BET_FEE_PCT);
        String hexInstructionData = AppConstants.getCashoutLvlToInstructionMap().get(AppConstants.getECashoutLvl());

        String instructionData = CryptoUtils.substituteInstructionParts(hexInstructionData, CryptoUtils.longToHEXUint32LE(gameRound), CryptoUtils.longToHEXUint32LE(totalBetLamports));
        byte[] instructionDataBytes = Hex.decodeHex(instructionData);
        TransactionInstruction transactionInstruction = new TransactionInstruction(crashProgramPublicKey, AppConstants.getBetCmdInputAccounts(), instructionDataBytes);
        transaction.addInstruction(transactionInstruction);
        return transaction;
    }

    public static Transaction buildClaimTransaction() throws DecoderException {
        Transaction transaction = new Transaction();
        PublicKey crashProgramPublicKey = new PublicKey(CRASH_PROGRAM_ID);
        String instructionData = CLAIM_FUNDS;
        byte[] instructionDataBytes = Hex.decodeHex(instructionData);
        TransactionInstruction transactionInstruction = new TransactionInstruction(crashProgramPublicKey, AppConstants.getClaimCmdInputAccounts(), instructionDataBytes);
        transaction.addInstruction(transactionInstruction);
        return transaction;
    }


            /*transaction.addInstruction(ComputeBudgetProgram.setComputeUnitPrice(CryptoUtils.lamportsToMicroLamports(20)));
        transaction.addInstruction(ComputeBudgetProgram.setComputeUnitLimit(600));*/
    //transaction.addInstruction(SystemProgram.transfer(fromAccount.getPublicKey(), crashPublicKey1, lamportsToSend1));

}
