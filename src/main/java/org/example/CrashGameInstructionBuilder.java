package org.example;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.p2p.solanaj.core.PublicKey;
import org.p2p.solanaj.core.Transaction;
import org.p2p.solanaj.core.TransactionInstruction;

import static org.example.AppConstants.*;

public class CrashGameInstructionBuilder {

    public static Transaction buildBetTransaction(long gameRound) throws DecoderException {
        Transaction transaction = new Transaction();
        PublicKey crashProgramPublicKey = new PublicKey(CRASH_PROGRAM_ID);
        String instructionData = CryptoUtils.substituteRoundPart(BET_0_05_CASHOUT158, CryptoUtils.longToHEXUint32LE(gameRound));
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
