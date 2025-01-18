package solcrash;

import lombok.Getter;

public enum ECashoutLevels {
    PERCNT_158(158),
    PERCNT_400(400);

    @Getter
    private final int cashoutLvl;

    ECashoutLevels(int cashoutLvl) {
        this.cashoutLvl = cashoutLvl;
    }

    public static ECashoutLevels fromLevel(int cashoutLvl) {
        for (ECashoutLevels level : values()) {
            if (level.cashoutLvl == cashoutLvl) {
                return level;
            }
        }
        return PERCNT_158; // as default
    }

}
