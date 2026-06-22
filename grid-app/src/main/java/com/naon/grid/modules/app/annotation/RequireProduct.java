package com.naon.grid.modules.app.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireProduct {
    String[] value() default {};           // 产品代码，如 "VOCAB"、"PLUS"
    RequireOrgRole orgRole() default RequireOrgRole.NONE;
}
