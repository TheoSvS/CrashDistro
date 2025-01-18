package solcrash.model;

import java.math.BigDecimal;
import java.util.List;

public record CrashData(List<BigDecimal> last100CrashLevels, List<BigDecimal> crashLevelsSinceStart, int finishedRoundPlayers) {
}
