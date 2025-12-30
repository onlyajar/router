package onlyajar.router;

class ServiceInfo {
    private final String key;
    private final Class implementationClazz;
    private final int priority;

    public ServiceInfo(String key, Class implementation, int priority) {
        if (key == null || implementation == null) {
            throw new RuntimeException("key and class is not empty");
        }
        this.key = key;
        this.implementationClazz = implementation;
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }

    public Class getImplementationClazz() {
        return implementationClazz;
    }
}
