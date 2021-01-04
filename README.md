# ProcStatParser
A Linux CPU load monitor that taps directly into /proc/stat

A successfull call returns a List containig the total CPU usage followed by the utilization of the seperate HW-hhreads.

To add dependancy in maven:
```xml
    <dependencies>
        <dependency>
            <groupId>com.github.archturion64</groupId>
            <artifactId>ProcStatParser</artifactId>
            <version>1.0.4</version>
        </dependency>
    </dependencies>
...
    <repositories>
        <repository>
            <id>ProcStatParser</id>
            <url>https://github.com/archturion64/ProcStatParser/raw/repository/</url>
            <releases>
                <enabled>true</enabled>
                <updatePolicy>always</updatePolicy>
            </releases>
            <snapshots>
                <enabled>true</enabled>
                <updatePolicy>always</updatePolicy>
            </snapshots>
        </repository>
    </repositories>

```

Usage:
```java
import com.github.archturion64.procstatparser.ProcStatParser;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

public class Main {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        // sync
        System.out.println(ProcStatParser.readCpuLoad());
        // async
        System.out.println(ProcStatParser.readCpuLoadAsync().get());
        // reactive
        ProcStatParser.monitorCpuLoad().subscribe(new CpuLoadSubscriber());

        Scanner s = new Scanner(System.in);
        System.out.println("Press enter to exit.....");
        s.nextLine();
    }
}
```

Clients that want to subscribe have to provide their own Flow.Subscriber implementation e.g.:
```java
import java.util.List;
import java.util.concurrent.Flow;

public class CpuLoadSubscriber implements Flow.Subscriber<List<Short>> {
    private Flow.Subscription subscription;

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        subscription.request(1);
    }

    @Override
    public void onNext(List<Short> item) {
        System.out.println(item);
        subscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {
        System.err.println(throwable.getMessage());
    }

    @Override
    public void onComplete() {
    }
}
```


Posible output on a system with 4 HW Threads:
```bash
[61, 47, 50, 57, 93]
[5, 8, 4, 4, 4]
Press enter to exit.....
[2, 0, 1, 6, 3]
[2, 2, 2, 4, 1]
[2, 3, 1, 1, 1]
[1, 4, 1, 1, 1]

```

