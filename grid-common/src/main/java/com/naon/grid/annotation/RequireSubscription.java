package com.naon.grid.annotation;

import com.naon.grid.enums.UserLevel;
import java.lang.annotation.*;

/**
 * 标注在 Controller 方法上，表示该接口需要指定级别的会员订阅才能访问。
 * 由 SubscriptionAspect 切面处理鉴权逻辑。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireSubscription {
    UserLevel value() default UserLevel.VIP;
}
