package solcrash.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RisingWindowMinMax {
    double minPeakVal;
    int minPeakIdx;
    double maxAfterMinPeakVal;
    double maxAfterMinPeakIdx;
}
