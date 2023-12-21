package easyexcel.validator;

import java.util.Map;
import java.util.Objects;

/**
 * 表头校验
 * @author chang
 * @since 2022/9/15
 */
public interface ExcelHeadValidator<T> {

    Map<Integer, String> getStandardHeadMap();

    default boolean checkPass(Map<Integer, String> importHeadMap) {
        Object[] standardHead = getStandardHeadMap().values().toArray();
        Object[] importHead = importHeadMap.values().toArray();
        if (standardHead.length > importHead.length) {
            return false;
        }
        for (int i=0; i<standardHead.length; i++) {
            if (!Objects.equals(standardHead[i], importHead[i])) {
                return false;
            }
        }
        return true;
    }

}
