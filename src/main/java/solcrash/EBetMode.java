package solcrash;

import java.util.HashMap;
import java.util.Map;

public enum EBetMode {
    AUTO,
    ON,
    OFF;



    private static final Map<String, EBetMode> stringToEnumMap = new HashMap<>();

    static {
        // Populate the map with string values and corresponding enum constants
        stringToEnumMap.put("true", ON);
        stringToEnumMap.put("t", ON);
        stringToEnumMap.put("on", ON);
        stringToEnumMap.put("auto", AUTO);
        stringToEnumMap.put("a", AUTO);
        stringToEnumMap.put("off", OFF);
        stringToEnumMap.put("false", OFF);
        stringToEnumMap.put("f", OFF);
    }

    public static EBetMode fromString(String betStr) {
        if (betStr == null || betStr.isEmpty()) {
            return EBetMode.OFF; // Default value
        }
        return stringToEnumMap.getOrDefault(betStr.toLowerCase(), EBetMode.OFF);
    }
}
