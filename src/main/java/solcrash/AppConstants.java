package solcrash;

import lombok.Getter;
import org.p2p.solanaj.core.Account;
import org.p2p.solanaj.core.AccountMeta;
import org.p2p.solanaj.core.PublicKey;
import org.p2p.solanaj.rpc.Cluster;
import org.p2p.solanaj.rpc.RpcClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class AppConstants {
    private static final Properties props = new Properties();
    public static final String PROPERTIES_FILE = "solcrash.properties";

    static {
        liveReadProperties();
    }

    @Getter
    private static final List<AccountMeta> betCmdInputAccounts = setBetCmdInputAccounts();
    @Getter
    private static final List<AccountMeta> claimCmdInputAccounts = setClaimCmdInputAccounts();
    @Getter
    private static final RpcClient client = new RpcClient(Cluster.MAINNET);
    @Getter
    private static String secret;
    @Getter
    private static Account fromAccount;
    @Getter
    private static EBetMode eBetMode;
    @Getter
    private static ECashoutLevels eCashoutLvl;
    @Getter
    public static final String GAME_ROUND = "GAME_ROUND";
    public static final String BET_COST_LAMPORTS = "BET_COST_LAMPORTS";
    public static final String BET_CASHOUT158 = "ce37026a71dc11a301006500" + GAME_ROUND + "00000000" + BET_COST_LAMPORTS + "00000000013000000065794a316332567958326c6b496a6f694d6a41344e6a41334f44497949697769595730694f6949784c6a5534496e303d";
    public static final String BET_CASHOUT400 = "ce37026a71dc11a301006500" + GAME_ROUND + "00000000" + BET_COST_LAMPORTS + "00000000013000000065794a316332567958326c6b496a6f694d6a41344e6a41334f44497949697769595730694f6949304c6a4177496e303d";
    public static final double BET_FEE_PCT = 1.02;
    public static final String CLAIM_FUNDS = "3ec6d6c1d59f6cd20100";
    public static final String CRASH_PROGRAM_ID = "CRSHdMVmWgRsarrRWnRXWKMxJhLEUheiU6SEhqCLS4Gm";
    public static final BigDecimal SOL_LAMPORTS = new BigDecimal(1_000_000_000);
    @Getter
    private static final Map<ECashoutLevels, String> cashoutLvlToInstructionMap = Map.of(
            ECashoutLevels.PERCNT_158, BET_CASHOUT158,
            ECashoutLevels.PERCNT_400, BET_CASHOUT400);


    private static List<AccountMeta> setBetCmdInputAccounts() {
        List<AccountMeta> accountMetas = new ArrayList<>();

        accountMetas.add(new AccountMeta(
                new PublicKey("RUMJK5kK2bvxem2XV2qJdB1vzv9tfUgzt8ZGkUnmeqe"),
                false,
                true
        ));

        accountMetas.add(new AccountMeta(
                new PublicKey("Gd2zWTdAs6Rwygn9thhcRBtadBng4csoEgaTdzfr5G3Z"),
                false,
                true
        ));

        accountMetas.add(new AccountMeta(
                new PublicKey("8Ty9jaGZku7TFVwo2EReB94VHw4FKQ2BqoEsamPZATUA"),
                false,
                true
        ));

        accountMetas.add(new AccountMeta(
                new PublicKey("FEEsULscQfrdNdg1HLE7i3A9trnqBauNJckKAvXA2Gsm"),
                false,
                true
        ));

        accountMetas.add(new AccountMeta(
                new PublicKey("EN9rWxRawq7EMjKt5p45P3tf8n9guDSeH2FpviCQbzwK"),
                false,
                true
        ));

        accountMetas.add(new AccountMeta(
                new PublicKey("4rMbRXNVVgF5KKmwEwT84BJRMpSk2Lgs3QUauBH6iPdS"),
                false,
                true
        ));

        accountMetas.add(new AccountMeta(
                fromAccount.getPublicKey(),
                true,
                true
        ));

        // System Program
        accountMetas.add(new AccountMeta(
                PublicKey.valueOf("11111111111111111111111111111111"),
                false,
                false
        ));

        return accountMetas;
    }

    private static List<AccountMeta> setClaimCmdInputAccounts() {
        List<AccountMeta> accountMetas = new ArrayList<>();

        accountMetas.add(new AccountMeta(
                new PublicKey("9jMScJvytEXKiuMDAqJMD9FfHXGrj2jy1m4xL7HpPd6W"),
                false,
                true
        ));

        accountMetas.add(new AccountMeta(
                fromAccount.getPublicKey(),
                true,
                true
        ));

        // System Program
        accountMetas.add(new AccountMeta(
                PublicKey.valueOf("11111111111111111111111111111111"),
                false,
                false
        ));

        return accountMetas;
    }

    public static void liveReadProperties() {
        reload();
        readSecret();
        System.out.println(DataUtils.getTime() + " ===");
        readBettingMode();
        readCashoutLvl();
    }

    private static void readSecret() {
        secret = props.getProperty("secret");
        fromAccount = Account.fromBase58PrivateKey(secret);
    }

    private static void readCashoutLvl() {
        String cashoutLvlStr = props.getProperty("cashoutLvl", "158");
        Integer readCashoutLvl = Integer.parseInt(cashoutLvlStr);
        eCashoutLvl = ECashoutLevels.fromLevel(readCashoutLvl);
        System.out.println("Chosen cashout: " + eCashoutLvl.getCashoutLvl());
    }

    private static void readBettingMode() {
        String betModeStr = props.getProperty("bettingMode", "false");
        eBetMode = EBetMode.fromString(betModeStr);
        System.out.println("Currently Betting: " + eBetMode);
    }

    private static void reload() {
        try (BufferedReader reader = Files.newBufferedReader(Path.of(PROPERTIES_FILE))) {
            props.clear();
            props.load(reader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
