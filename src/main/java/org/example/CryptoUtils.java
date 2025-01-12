package org.example;

import org.p2p.solanaj.core.PublicKey;
import org.p2p.solanaj.rpc.RpcException;

import java.math.BigDecimal;

import static org.example.AppConstants.*;

public class CryptoUtils {


    public static long solToLamports(double sol) {
        BigDecimal bSol = new BigDecimal(sol);
        return bSol.multiply(SOL_LAMPORTS).longValue();
    }

    public static double lamportsToSol(long lamports) {
        return lamports / 1_000_000_000.0;
    }

    public static int lamportsToMicroLamports(int lamports) {
        return lamports * ((int) Math.pow(10.0, 6.0));
    }

    public static double getSolanaBalance(PublicKey fromPublicKey) throws RpcException {
        long lamports = AppConstants.getClient().getApi().getBalance(fromPublicKey);
        return lamportsToSol(lamports);
    }

    public static String longToHEXUint32LE(long value) {
        // Only keep the lower 32 bits (truncate if value > 2^32 - 1)
        int intValue = (int) (value & 0xFFFFFFFFL);

        byte[] result = new byte[4];
        // Little Endian: least significant byte goes in index 0
        result[0] = (byte) (intValue & 0xFF);
        result[1] = (byte) ((intValue >> 8) & 0xFF);
        result[2] = (byte) ((intValue >> 16) & 0xFF);
        result[3] = (byte) ((intValue >> 24) & 0xFF);

        // Convert each byte to a two-digit hex string
        return String.format("%02x%02x%02x%02x",
                result[0], result[1], result[2], result[3]);
    }

    public static String substituteInstructionParts(String hexInstructionData, String round, String totalBetCost) {
        return hexInstructionData.replace(GAME_ROUND, round).replace(BET_COST_LAMPORTS,totalBetCost);
    }


    //0.05 bet  1.58 cashout
    //UINT32 - Little Endian (DCBA)
    //ce37026a71dc11a301006500d11b030000000000e0d3110300000000013000000065794a316332567958326c6b496a6f694d6a41344e6a41334f44497949697769595730694f6949784c6a5534496e303d
    //ce37026a 71dc11a3 01006500 d11b0300 00000000 e0d31103 00000000 01300000 0065794a 31633256 7958326c 6b496a6f 694d6a41 344e6a41 334f4449 79496977 69595730 694f6949 784c6a55 34496e30 3d(000000)
/*    25 and 26 digit
    where d11b0300    is round  203729
    where d91b0300    is round  203737
    where ec1b0300    is round  203756
    where 071c0300    is round  203783*/

    //ce37026a71dc11a301006500________00000000e0d3110300000000013000000065794a316332567958326c6b496a6f694d6a41344e6a41334f44497949697769595730694f6949784c6a5534496e303d

}
