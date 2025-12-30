# router

[![](https://www.jitpack.io/v/onlyajar/router.svg)](https://www.jitpack.io/#onlyajar/router)

### 快速配置 

#### gradle
```groovy
implementation 'com.github.onlyajar:router:0.0.1'
annotationProcessor 'com.google.auto.service:auto-service:1.1.1'
```
#### gradle.kts
```groovy
implementation("com.github.onlyajar:router:0.0.1")
kapt("com.google.auto.service:auto-service:1.1.1")
```

### 使用说明

```java
import com.google.auto.service.AutoService;
@AutoService(value = Runnable.class, key = "pay")
public class RouterToPay implements Runnable{
    @Override
    public void run() {
        System.out.println("RouterToPay");
    }
}
List<Class<Runnable>> runs = Router.getAllServiceClasses(Runnable.class);

Class<Runnable> payClass = Router.getServiceClass(Runnable.class, "pay");

Runnable payRun = Router.getService(Runnable.class, "pay");

```


