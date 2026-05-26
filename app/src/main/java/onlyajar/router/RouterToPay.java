package onlyajar.router;

import com.google.auto.service.AutoService;

@AutoService(value = Runnable.class, key = "RouterToPay", priority = 2)
public class RouterToPay implements Runnable{
    @Override
    public void run() {
        System.out.println("RouterToPay");
    }
}
