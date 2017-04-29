package com.echeng.eventbus;

import android.os.Handler;
import android.os.Looper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by che on 2017/4/29.
 */

public class EventBus {
    private static EventBus instance;
    private  Map<Object, List<SubcribeMethod>> cacheMap;
    private Handler handler;
    private ExecutorService executors;

    private EventBus() {
        cacheMap = new HashMap<>();
        handler = new Handler(Looper.getMainLooper());
        executors = Executors.newCachedThreadPool();
    }

    public static EventBus getDefault() {
        if (instance == null) {
            synchronized (EventBus.class) {
                if (instance == null) {
                    instance = new EventBus();
                }
            }
        }
        return instance;

    }


    public void regist(Object object){
        List<SubcribeMethod> list = cacheMap.get(object);
        if (list ==null){
            List<SubcribeMethod> methods = findSubscribeMethod(object);
            cacheMap.put(object,methods);
        }

    }

    private List<SubcribeMethod> findSubscribeMethod(Object object) {
        List<SubcribeMethod>  list = new CopyOnWriteArrayList<>();
        Class<?> clazz =  object.getClass();

        while (clazz!=null){
            String clazzName = clazz.getName();
            if (clazzName.startsWith("java.")|| clazzName.startsWith("javax.")|| clazzName.startsWith("android.")){
                break;
            }
            Method[]  methods =clazz.getDeclaredMethods();
            for (Method method :methods){
                Subscribe subscribe = method.getAnnotation(Subscribe.class);
                if (subscribe ==null){
                    continue;
                }

                Class<?>[] parameters =  method.getParameterTypes();
                if (parameters.length!=1){
                    throw new RuntimeException("eventbus must be one parameter");
                }
                Class<?> paramClass = parameters[0];//参数的class类型
                ThreadMode threadMode = subscribe.threadMode();

                SubcribeMethod subcribeMethod = new SubcribeMethod(method,paramClass,threadMode);
                list.add(subcribeMethod);
            }

          clazz =  clazz.getSuperclass();
        }
        return list;
    }


    public void post(final Object object){

       Set<Map.Entry<Object,List<SubcribeMethod>>> set = cacheMap.entrySet();
       for(Map.Entry<Object,List<SubcribeMethod>> entry:set){
           final Object activity = entry.getKey();
           List<SubcribeMethod> list=  entry.getValue();

           for (final SubcribeMethod subcribeMethod:list){
               /**
                instanceof 针对实例
                isAssignableFrom针对class对象
                isAssignableFrom   是用来判断一个类Class1和另一个类Class2是否相同或是另一个类的超类或接口。
                其中instanceof是子-->父
                isAssignableFrom是父-->子
                */
               if (subcribeMethod.getEventType().isAssignableFrom(object.getClass())){
                   //线程切换
                   switch ( subcribeMethod.getThreadMode()){

                       case POSTING:
                           invoke(activity,subcribeMethod,object);
                           break;
                       case MAIN:
                           //发送在主线程
                           if (Looper.myLooper() ==Looper.getMainLooper()){
                               invoke(activity,subcribeMethod,object);
                           }else{
                               handler.post(new Runnable() {
                                   @Override
                                   public void run() {
                                       invoke(activity,subcribeMethod,object);
                                   }
                               });
                           }
                           break;
                       case BACKGROUND:
                           //发送在主线程
                           if (Looper.myLooper() ==Looper.getMainLooper()){
                               executors.execute(new Runnable() {
                                   @Override
                                   public void run() {
                                       invoke(activity,subcribeMethod,object);
                                   }
                               });
                           }else{
                               invoke(activity,subcribeMethod,object);
                           }
                           break;
                   }
               }
           }
       }
    }

    private void invoke(Object activity, SubcribeMethod subcribeMethod, Object object) {
        try {
            if (subcribeMethod.getMethod().getModifiers()!= Modifier.PUBLIC)
               subcribeMethod.getMethod().setAccessible(true);
            subcribeMethod.getMethod().invoke(activity,object);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public void unregist(Object object){
        if (cacheMap!=null && cacheMap.containsKey(object)){
            cacheMap.remove(object);
        }

    }
}
