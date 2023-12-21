package easyexcel.annotation;

import java.lang.annotation.*;

/**
 * @author chang
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelResponse {

    String fileName() default "default";

    String sheetName() default "Sheet1";

}
