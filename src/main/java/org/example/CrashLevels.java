package org.example;

import java.math.BigDecimal;
import java.util.List;

public record CrashLevels(List<BigDecimal> last100CrashLevels, List<BigDecimal> crashLevelsSinceStart) {
}
