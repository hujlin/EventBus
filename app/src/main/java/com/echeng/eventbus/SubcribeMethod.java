package com.echeng.eventbus;

import java.lang.reflect.Method;

/**
 * Created by che on 2017/4/29.
 */

public class SubcribeMethod {
    private Method method;
    private Class<?> eventType;
    private ThreadMode threadMode;

    public SubcribeMethod(Method method, Class<?> eventType, ThreadMode threadMode) {
        this.method = method;
        this.eventType = eventType;
        this.threadMode = threadMode;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Class<?> getEventType() {
        return eventType;
    }

    public void setEventType(Class<?> eventType) {
        this.eventType = eventType;
    }

    public ThreadMode getThreadMode() {
        return threadMode;
    }

    public void setThreadMode(ThreadMode threadMode) {
        this.threadMode = threadMode;
    }
}
