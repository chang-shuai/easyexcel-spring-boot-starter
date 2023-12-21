package easyexcel.validator.errors;

/**
 * @author chang
 */
public class DefaultExcelObjectError implements ExcelValidObjectError {

    /**
     * 行号
     */
    private Integer row;

    /**
     * 错误消息
     */
    private String message;

    public DefaultExcelObjectError(Integer row, String message) {
        this.row = row;
        this.message = message;
    }

    @Override
    public Integer getRow() {
        return row;
    }

    @Override
    public String getMessage() {
        return message;
    }


    @Override
    public String toString() {
        return "DefaultExcelObjectError{" +
                "row=" + row +
                ", message='" + message + '\'' +
                '}';
    }
}
