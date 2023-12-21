package easyexcel.validator.errors;

/**
 * @author chang
 */
public class DefaultExcelValidFieldError extends DefaultExcelObjectError implements ExcelValidFieldError {

    private Integer column;

    public DefaultExcelValidFieldError(Integer row, Integer column, String message) {
        super(row, message);
        this.column = column;
    }

    @Override
    public Integer getColumn() {
        return column;
    }

    @Override
    public String toString() {
        return "DefaultExcelValidFieldError{" +
                "column=" + column +
                '}';
    }
}
