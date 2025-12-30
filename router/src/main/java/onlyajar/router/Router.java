package onlyajar.router;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class Router {
    public static <S> List<S> loadService(Class<S> service){
        Iterable<S> services = ServiceLoader.load(service);
        List<S> list = new ArrayList<>();
        for (S s: services) {
            list.add(s);
        }
        return list;
    }

    /**
     * 根据key获取实现类的Class。注意，对于声明了singleton的实现类，获取Class后还是可以创建新的实例。
     *
     * @return 找不到或获取失败，则返回null
     */
    public static <S> S getService(Class<S> clazz, String key) {
        try {
            Class<S> serviceClass = getServiceClass(clazz, key);
            return serviceClass == null? null : serviceClass.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            e.fillInStackTrace();
        }
        return null;
    }

    /**
     * 根据key获取实现类的Class。注意，对于声明了singleton的实现类，获取Class后还是可以创建新的实例。
     *
     * @return 找不到或获取失败，则返回null
     */
    public static <S> Class<S> getServiceClass(Class<S> clazz, String key) {
        return ServiceClassLoader.load(clazz).getClass(key);
    }

    /**
     * 获取所有实现类的Class。注意，对于声明了singleton的实现类，获取Class后还是可以创建新的实例。
     *
     * @return 可能返回EmptyList，List中的元素不为空
     */
    public static <S> List<Class<S>> getAllServiceClasses(Class<S> clazz) {
        return ServiceClassLoader.load(clazz).getAllClasses();
    }

}
