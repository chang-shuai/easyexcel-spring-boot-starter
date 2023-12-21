package easyexcel.resolver;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.metadata.CellData;
import com.alibaba.excel.util.ConverterUtils;
import easyexcel.annotation.ExcelParam;
import easyexcel.validator.*;
import easyexcel.validator.errors.ExcelValidErrors;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartRequest;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import javax.servlet.http.HttpServletRequest;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 参数解析器
 * @author chang
 */
public class ExcelParamResolver implements HandlerMethodArgumentResolver, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public boolean supportsParameter(MethodParameter methodParameter) {
        HttpServletRequest request = ((ServletRequestAttributes) (RequestContextHolder.currentRequestAttributes())).getRequest();
        String contentType = request.getContentType();
        boolean multipart = contentType != null && contentType.toLowerCase().startsWith("multipart/");

        ExcelParam excelParam = methodParameter.getParameterAnnotation(ExcelParam.class);
        ResolvableType param = ResolvableType.forMethodParameter(methodParameter);
        return multipart && excelParam != null
                && (ResolvableType.forClass(ReadRows.class).isAssignableFrom(param)
                || ResolvableType.forClass(List.class).isAssignableFrom(param));
    }

    @Override
    public Object resolveArgument(MethodParameter methodParameter, ModelAndViewContainer modelAndViewContainer, NativeWebRequest nativeWebRequest, WebDataBinderFactory webDataBinderFactory) throws Exception {
        ExcelParam excelParam = methodParameter.getParameterAnnotation(ExcelParam.class);
        MultipartRequest request = nativeWebRequest.getNativeRequest(MultipartRequest.class);
        MultipartFile file = request.getFile(excelParam.value());
        if (file == null) {
            if (excelParam.required()) {
                throw new MissingServletRequestPartException(excelParam.value());
            }
            return null;
        }

        ReadRows<Object> readRows = new ReadRows<>();

        ResolvableType[] generics = ResolvableType.forType(methodParameter.getGenericParameterType()).getGenerics();
        Class<?> component = generics[generics.length - 1].resolve();
        EasyExcel.read(file.getInputStream(), component, new AnalysisEventListener<Object>() {

            @Override
            public void invoke(Object data, AnalysisContext context) {
                Integer rowIndex = context.readRowHolder().getRowIndex();
                readRows.getRows().add(new ReadRow<>(rowIndex, data));
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                readRows.setExcelReadHeadProperty(context.currentReadHolder().excelReadHeadProperty());
            }


            @Override
            public void invokeHead(Map<Integer, CellData> headMap, AnalysisContext context) {
                Map<Integer, String> importHeadMap = ConverterUtils.convertToStringMap(headMap, context);
                boolean pass = validateIfApplicable(methodParameter, importHeadMap, context);
                if (!pass) {
                    throw new ExcelTemplateException("Excel模板错误");
                }
            }

            @Override
            public void onException(Exception exception, AnalysisContext context) throws Exception {
                // 表头信息错误, 直接返回, 不继续解析
                throw exception;
            }
        }).sheet().doRead();
        ExcelValidErrors errors = this.validateIfApplicable(methodParameter, readRows);
        if (errors.hasErrors() && isBindExceptionRequired(methodParameter)) {
            throw new ExcelValidException("参数校验有误", errors);
        }
        if (modelAndViewContainer != null) {
            modelAndViewContainer.addAttribute(BindingResult.MODEL_KEY_PREFIX + "excel", errors);
        }

        if (List.class.isAssignableFrom(methodParameter.getParameterType())) {
            return readRows.getRows().stream().map(ReadRow::getData).collect(Collectors.toList());
        }
        if (ReadRows.class == methodParameter.getParameterType()) {
            return readRows;
        }
        return null;
    }

    /**
     * 是否不需要验证
     */
    private boolean needlessValidate(MethodParameter parameter) {
        Annotation[] annotations = parameter.getParameterAnnotations();
        boolean needless = true;
        for (Annotation ann : annotations) {
            Validated validatedAnn = AnnotationUtils.getAnnotation(ann, Validated.class);
            if (validatedAnn != null || ann.annotationType().getSimpleName().startsWith("Valid")) {
                needless = false;
                break;
            }
        }
        return needless;
    }

    private boolean validateIfApplicable(MethodParameter parameter, Map<Integer, String> headMap, AnalysisContext context) {
        if (needlessValidate(parameter)) {
            return true;
        }
        Class<?> headClazz = context.currentReadHolder().excelReadHeadProperty().getHeadClazz();
        List<ExcelHeadValidator<Object>> validators =
                this.applicationContext.getBeansOfType(ExcelHeadValidator.class).values().stream().filter(item -> {
                    Class<?> component = ResolvableType.forInstance(item).as(ExcelHeadValidator.class).resolveGeneric(0);
                    return component == Object.class || component.isAssignableFrom(headClazz);
                }).map(item -> (ExcelHeadValidator<Object>) item).collect(Collectors.toList());
        if (validators.size() > 1) {
            List<String> classList = validators.stream().map(item -> item.getClass().toString()).collect(Collectors.toList());
            throw new NoUniqueBeanDefinitionException(ExcelHeadValidator.class, validators.size(), "存在多个实现类:"+classList);
        }
        if (validators.size() < 1) {
            throw new NoSuchBeanDefinitionException(ExcelHeadValidator.class);
        }
        ExcelHeadValidator<Object> validator = validators.get(0);
        return validator.checkPass(headMap);

    }

    private ExcelValidErrors validateIfApplicable(MethodParameter parameter, ReadRows<Object> readRows) {
        if (needlessValidate(parameter)) {
            return new ExcelValidErrors();
        }
        Class<?> headClazz = readRows.getExcelReadHeadProperty().getHeadClazz();
        List<ExcelValidator<Object>> validators =
                this.applicationContext.getBeansOfType(ExcelValidator.class).values().stream().filter(item -> {
                    Class<?> component = ResolvableType.forInstance(item).as(ExcelValidator.class).resolveGeneric(0);
                    return component == Object.class || component.isAssignableFrom(headClazz);
                }).map(item -> (ExcelValidator<Object>) item).collect(Collectors.toList());
        // TODO 验证排序
        CompositeExcelValidator validator = new CompositeExcelValidator(validators);

        return validator.validate(readRows);
    }

    private boolean isBindExceptionRequired(MethodParameter parameter) {
        int i = parameter.getParameterIndex();
        Class<?>[] paramTypes = parameter.getExecutable().getParameterTypes();
        boolean hasBindingResult = (paramTypes.length > (i + 1) && ExcelValidErrors.class.isAssignableFrom(paramTypes[i + 1]));
        return !hasBindingResult;
    }


}
