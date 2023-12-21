package easyexcel.validator.errors;

/**
 * @author chang
 */
public interface ExcelValidFieldError extends ExcelValidObjectError {


    /**
     * 获取列，从 1 开始
     *
     * @return
     */
    Integer getColumn();
}
