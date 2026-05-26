package onlyajar.router;

import com.google.auto.service.AutoService;

@AutoService(value = Runnable.class, key = "RouterToPay2", priority = 2)
public class RouterToPay2 implements Runnable{
    @Override
    public void run() {
        System.out.println("RouterToPay2");
    }
}
