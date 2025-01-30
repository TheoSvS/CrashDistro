package solcrash.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FallingWindowMinMax {
    double maxPeakVal;
    int maxPeakIdx;
    double minAfterMaxPeakVal;
    double minAfterMaxPeakIdx;
}
