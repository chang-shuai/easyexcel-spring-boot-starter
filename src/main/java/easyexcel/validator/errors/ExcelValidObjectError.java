package easyexcel.validator.errors;

/**
 * @author chang
 */
public interface ExcelValidObjectError {

    /**
     * 获取行号，从 1 开始
     *
     * @return
     */
    Integer getRow();

    /**
     * 获取错误消息
     *
     * @return
     */
    String getMessage();
}
