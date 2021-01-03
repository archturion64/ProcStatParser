# ProcStatParser
A Linux CPU load monitor that taps directly into /proc/stat

Usage:
```java
import com.github.archturion64.procstatparser.ProcStatParser;

public class Main {

    public static void main(String[] args) {
        System.out.println(ProcStatParser.ReadCpuLoad());
	// or
	System.out.println(ProcStatParser.ReadCpuLoadAsync().get());
    }
}
```

To add dependancy in maven:
```xml
    <dependencies>
        <dependency>
            <groupId>com.github.archturion64</groupId>
            <artifactId>ProcStatParser</artifactId>
            <version>1.0.1</version>
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

Posible output on a system with 4 HW Threads:
	[72, 70, 71, 55, 92]

