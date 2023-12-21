package easyexcel.annotation;

import java.lang.annotation.*;

/**
 * @author chang
 */
@Documented
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelParam {

    /**
     * 字段名称
     *
     * @return
     */
    String value() default "file";

    /**
     * 是否必须
     *
     * @return
     */
    boolean required() default true;

}
