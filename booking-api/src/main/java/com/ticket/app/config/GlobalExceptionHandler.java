package com.ticket.app.config;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.ticket.app.common.Result;
import com.ticket.app.common.exception.BusinessException;
import com.ticket.app.common.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常: {} - {}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常 - BindingResult
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数绑定异常: {}", message);
        return Result.fail(100600, message);
    }

    /**
     * 处理参数校验异常 - @Valid
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验异常: {}", message);
        return Result.fail(100600, message);
    }

    /**
     * 处理自定义参数校验异常
     */
    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleValidationException(ValidationException e) {
        log.warn("参数校验异常: {}", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理请求参数缺失异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.warn("缺少请求参数: {} - {}", e.getParameterName(), e.getParameterType());
        return Result.fail(100601, "缺少必填参数: " + e.getParameterName());
    }

    /**
     * 处理参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型不匹配: {} - {}", e.getName(), e.getValue());
        return Result.fail(100602, "参数格式不正确: " + e.getName());
    }

    /**
     * 处理请求方法不支持异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<?> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持: {}", e.getMethod());
        return Result.fail(100602, "请求方法不支持: " + e.getMethod());
    }

    /**
     * 处理JSON解析异常 - BUG-002修复
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleHttpMessageNotReadableException(HttpMessageNotReadableException e, HttpServletRequest request) {
        String message = "请求参数格式错误";
        Throwable cause = e.getCause();
        if (cause instanceof InvalidFormatException invalidFormatException) {
            String fieldName = "";
            try {
                fieldName = invalidFormatException.getPath().isEmpty() ? "" : 
                        invalidFormatException.getPath().get(0).getFieldName();
            } catch (Exception ex) {
                // 忽略路径解析异常
            }
            String targetType = invalidFormatException.getTargetType().getSimpleName();
            Object invalidValue = invalidFormatException.getValue();
            message = String.format("参数'%s'的值'%s'无法转换为%s类型", 
                    fieldName != null ? fieldName : "未知", 
                    invalidValue != null ? invalidValue.toString() : "null",
                    targetType);
            log.warn("JSON参数类型错误: {} - {}", fieldName, invalidFormatException.getMessage());
        } else if (cause instanceof JsonMappingException jsonMappingException) {
            message = "JSON解析错误: " + jsonMappingException.getMessage();
            log.warn("JSON解析错误: {}", jsonMappingException.getMessage());
        }
        log.warn("请求消息不可读: {} - {}", request.getRequestURI(), e.getMessage());
        return Result.fail(100600, message);
    }

    /**
     * 处理日期格式解析异常 - BUG-007修复
     */
    @ExceptionHandler(DateTimeParseException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleDateTimeParseException(DateTimeParseException e) {
        log.warn("日期格式解析错误: {}", e.getParsedString());
        return Result.fail(100603, "日期格式不正确，请使用yyyy-MM-dd格式");
    }

    /**
     * 处理数据访问异常 - 数据库操作异常
     */
    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleDataAccessException(DataAccessException e, HttpServletRequest request) {
        log.error("数据库访问异常: {} - {}", request.getRequestURI(), e.getMessage(), e);
        // 根据异常类型返回更友好的消息
        String message = "数据库操作失败";
        if (e.getMessage() != null && e.getMessage().contains("Table")) {
            message = "数据表不存在，请检查数据库配置";
        } else if (e.getMessage() != null && e.getMessage().contains("Connection")) {
            message = "数据库连接失败，请检查数据库配置";
        }
        return Result.fail(200101, message);
    }

    /**
     * 处理媒体类型不支持异常
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public Result<?> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        log.warn("媒体类型不支持: {}", e.getContentType());
        return Result.fail(100600, "请求的Content-Type不支持，请使用application/json");
    }

    /**
     * 处理404异常
     */
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<?> handleNotFoundException(Exception e, HttpServletRequest request) {
        log.warn("资源不存在: {}", request.getRequestURI());
        return Result.fail(404, "接口不存在");
    }

    /**
     * 处理未知异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常: {} - {}", request.getRequestURI(), e.getMessage(), e);
        return Result.fail(200100, "系统繁忙，请稍后再试");
    }
}
