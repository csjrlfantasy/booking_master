package com.ticket.app.config.mybatis;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class MyBatisPlusConfigTest {

    @Test
    void shouldRegisterExactlyOneMysqlPaginationInterceptor() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.scan("com.ticket.app.config.mybatis");
            context.refresh();

            Map<String, MybatisPlusInterceptor> beans = context.getBeansOfType(
                    MybatisPlusInterceptor.class
            );
            assertEquals(1, beans.size());
            MybatisPlusInterceptor interceptor = beans.values().iterator().next();
            List<InnerInterceptor> innerInterceptors = interceptor.getInterceptors();

            assertEquals(1, innerInterceptors.size());
            PaginationInnerInterceptor pagination = assertInstanceOf(
                    PaginationInnerInterceptor.class,
                    innerInterceptors.get(0)
            );
            assertEquals(DbType.MYSQL, pagination.getDbType());
        }
    }
}
