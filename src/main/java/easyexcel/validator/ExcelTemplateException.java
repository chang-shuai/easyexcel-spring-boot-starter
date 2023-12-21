package easyexcel.validator;

/**
 * Excel 模板校验异常
 * @author chang
 * @since 2022/9/15
 */
public class ExcelTemplateException extends RuntimeException {

    public ExcelTemplateException(String status) {
        super(status);
    }
}
