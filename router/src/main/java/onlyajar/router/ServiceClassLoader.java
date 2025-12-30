package onlyajar.router;

import com.google.auto.service.AutoService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.ServiceConfigurationError;

public final class ServiceClassLoader<S> implements Iterable<Class<S>> {

    private static final Map<Class, ServiceClassLoader> SERVICES = new HashMap<>();
    private static final String PREFIX = "META-INF/services/";

    private final Class<S> service;

    private final ClassLoader loader;

    private final LinkedHashMap<String,Class<S>> providers = new LinkedHashMap<>();

    private LazyIterator lookupIterator;

    private final HashMap<String, ServiceInfo> serviceInfoHashMap = new HashMap<>();

    public void reload() {
        providers.clear();
        lookupIterator = new LazyIterator(service, loader);
        loadClass();
    }

    private void loadClass(){
        for (Class<S> clazz : this) {
            AutoService autoService = clazz.getAnnotation(AutoService.class);
            if (autoService == null) {
                throw new RuntimeException("AutoService is null");
            }
            String[] names = autoService.key();
            int priority = autoService.priority();
            if (names.length == 0) {
                serviceInfoHashMap.put(clazz.getName(), new ServiceInfo(clazz.getName(), clazz, priority));
                continue;
            }
            for (String key : names) {
                serviceInfoHashMap.put(key, new ServiceInfo(key, clazz, priority));
            }
        }
    }

    private ServiceClassLoader(Class<S> svc, ClassLoader cl) {
        service = Objects.requireNonNull(svc, "Service interface cannot be null");
        loader = (cl == null) ? ClassLoader.getSystemClassLoader() : cl;
        reload();
    }

    private static void fail(Class<?> service, String msg, Throwable cause)
            throws ServiceConfigurationError
    {
        throw new ServiceConfigurationError(service.getName() + ": " + msg,
                cause);
    }

    private static void fail(Class<?> service, String msg)
            throws ServiceConfigurationError
    {
        throw new ServiceConfigurationError(service.getName() + ": " + msg);
    }

    private static void fail(Class<?> service, URL u, int line, String msg)
            throws ServiceConfigurationError
    {
        fail(service, u + ":" + line + ": " + msg);
    }

    private int parseLine(Class<?> service, URL u, BufferedReader r, int lc,
                          List<String> names)
            throws IOException, ServiceConfigurationError
    {
        String ln = r.readLine();
        if (ln == null) {
            return -1;
        }
        int ci = ln.indexOf('#');
        if (ci >= 0) ln = ln.substring(0, ci);
        ln = ln.trim();
        int n = ln.length();
        if (n != 0) {
            if ((ln.indexOf(' ') >= 0) || (ln.indexOf('\t') >= 0))
                fail(service, u, lc, "Illegal configuration-file syntax");
            int cp = ln.codePointAt(0);
            if (!Character.isJavaIdentifierStart(cp))
                fail(service, u, lc, "Illegal provider-class name: " + ln);
            for (int i = Character.charCount(cp); i < n; i += Character.charCount(cp)) {
                cp = ln.codePointAt(i);
                if (!Character.isJavaIdentifierPart(cp) && (cp != '.'))
                    fail(service, u, lc, "Illegal provider-class name: " + ln);
            }
            if (!providers.containsKey(ln) && !names.contains(ln))
                names.add(ln);
        }
        return lc + 1;
    }

    private Iterator<String> parse(Class<?> service, URL u)
            throws ServiceConfigurationError
    {
        InputStream in = null;
        BufferedReader r = null;
        ArrayList<String> names = new ArrayList<>();
        try {
            in = u.openStream();
            r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            int lc = 1;
            while ((lc = parseLine(service, u, r, lc, names)) >= 0);
        } catch (IOException x) {
            fail(service, "Error reading configuration file", x);
        } finally {
            try {
                if (r != null) r.close();
                if (in != null) in.close();
            } catch (IOException y) {
                fail(service, "Error closing configuration file", y);
            }
        }
        return names.iterator();
    }

    private class LazyIterator implements Iterator<Class<S>> {

        Class<S> service;
        ClassLoader loader;
        Enumeration<URL> configs = null;
        Iterator<String> pending = null;
        String nextName = null;

        private LazyIterator(Class<S> service, ClassLoader loader) {
            this.service = service;
            this.loader = loader;
        }

        private boolean hasNextService() {
            if (nextName != null) {
                return true;
            }
            if (configs == null) {
                try {
                    String fullName = PREFIX + service.getName();
                    if (loader == null)
                        configs = ClassLoader.getSystemResources(fullName);
                    else
                        configs = loader.getResources(fullName);
                } catch (IOException x) {
                    fail(service, "Error locating configuration files", x);
                }
            }
            while ((pending == null) || !pending.hasNext()) {
                if (!configs.hasMoreElements()) {
                    return false;
                }
                pending = parse(service, configs.nextElement());
            }
            nextName = pending.next();
            return true;
        }

        private Class<S> nextService() {
            if (!hasNextService())
                throw new NoSuchElementException();
            String cn = nextName;
            nextName = null;
            Class<?> c = null;
            try {
                c = Class.forName(cn, false, loader);
            } catch (ClassNotFoundException x) {
                fail(service, "Provider " + cn + " not found", x);
            }
            if (!service.isAssignableFrom(c)) {
                // Android-changed: Let the ServiceConfigurationError have a cause.
                ClassCastException cce = new ClassCastException(
                        service.getCanonicalName() + " is not assignable from " + c.getCanonicalName());
                fail(service,
                        "Provider " + cn  + " not a subtype", cce);
            }
            try {
                providers.put(cn, (Class<S>) c);
                return (Class<S>) c;
            } catch (Throwable x) {
                fail(service,
                        "Provider " + cn + " could not be instantiated",
                        x);
            }
            throw new Error();          // This cannot happen
        }

        public boolean hasNext() {
            return hasNextService();
        }

        public Class<S> next() {
            return nextService();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    @Override
    public  Iterator<Class<S>> iterator() {
        return new Iterator<Class<S>>() {

            final Iterator<Map.Entry<String,Class<S>>> knownProviders
                    = providers.entrySet().iterator();

            public boolean hasNext() {
                if (knownProviders.hasNext())
                    return true;
                return lookupIterator.hasNext();
            }

            public Class<S> next() {
                if (knownProviders.hasNext())
                    return knownProviders.next().getValue();
                return lookupIterator.next();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }

        };
    }

    public <T extends S> Class<T> getClass(String key) {
        ServiceInfo serviceInfo = serviceInfoHashMap.get(key);
        return serviceInfo == null? null : (Class<T>) serviceInfo.getImplementationClazz();
    }

    public <T extends S> List<Class<T>> getAllClasses() {
        List<Class<T>> list = new ArrayList<>(serviceInfoHashMap.size());
        for (ServiceInfo serviceInfo : serviceInfoHashMap.values()) {
            Class<T> clazz = (Class<T>) serviceInfo.getImplementationClazz();
            if (clazz != null) {
                list.add(clazz);
            }
        }
        return list;
    }

    public static <S> ServiceClassLoader<S> load(Class<S> interfaceClass, ClassLoader loader) {
        ServiceClassLoader<S> service = SERVICES.get(interfaceClass);
        if (service == null) {
            synchronized (SERVICES) {
                service = SERVICES.get(interfaceClass);
                if (service == null) {
                    service = new ServiceClassLoader<>(interfaceClass, loader);
                    SERVICES.put(interfaceClass, service);
                }
            }
        }
        return service;
    }

    public static <S> ServiceClassLoader<S> load(Class<S> service) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return ServiceClassLoader.load(service, cl);
    }
}
