package easyexcel.validator;

import easyexcel.validator.errors.ExcelValidErrors;

/**
 * 数据校验
 *
 * @author chang
 */
public interface ExcelValidator<T> {

    /**
     * 校验
     *
     * @param readRows 读取的行信息
     * @return
     */
    ExcelValidErrors validate(ReadRows<T> readRows);

}
