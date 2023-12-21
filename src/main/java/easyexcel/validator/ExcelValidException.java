package easyexcel.validator;


import easyexcel.validator.errors.ExcelValidErrors;

/**
 * 校验异常
 *
 * @author chang
 */
public class ExcelValidException extends RuntimeException {

    private ExcelValidErrors errors;

    public ExcelValidException(String message, ExcelValidErrors errors) {
        super(message);
        this.errors = errors;
    }

    public ExcelValidErrors getErrors() {
        return errors;
    }

}
