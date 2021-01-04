package com.github.archturion64.procstatparser;

import com.github.archturion64.procstatparser.model.CpuLoad;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * Retrieves the current CPU load on a Linux based system by reading /proc/stat.
 * No dependencies required besides JRE and the Linux kernel.
 *
 * This class uses the https://www.idnt.net/en-US/kb/941772 parsing approach.
 *
 * An example for a /proc/stat output which is to be parsed:
 *
 * cpu  85280 100 17875 1565564 1138 0 242 0 0 0
 * cpu0 22526 33 4667 389829 183 0 48 0 0 0
 * cpu1 21741 18 3971 391716 323 0 133 0 0 0
 * cpu2 19902 17 4160 393243 228 0 16 0 0 0
 * cpu3 21109 31 5076 390774 403 0 44 0 0 0
 * intr 2287490 6 11 0 0 0 0 0 0 1 465 0 0 355019 0 0 0 321 0 0 668 0 0 0 31 0 0 0 0 0 78245 77613 19 140945 551 0 0 ...
 * ctxt 5484946
 * btime 1609569892
 * processes 11151
 * procs_running 1
 * procs_blocked 0
 * softirq 2394697 108 882289 177 1484 75406 0 1137 816499 534 617063
 *
 */
public class ProcStatParser {

    private static final int PROBING_INTERVAL_MS = 1000;
    private static final int PROBE_MAX_SIZE = 2;
    private static Stack<List<CpuLoad>> probeResults = new Stack<>();

    private static long[][] parseProcStat() throws NumberFormatException, IOException, SecurityException {
        try (Stream<String> lines = Files.lines(Path.of("/proc/stat"))) {

            return lines.filter((line) -> line.startsWith("cpu"))
                    .map(line -> Stream.of(line.split("\\W+"))
                            .skip(1) // ignore the cpuX - String
                            .map(Long::parseLong)
                            .mapToLong(Long::longValue)
                            .toArray()
                    )
                    .toArray(long[][]::new);
        }
    }

    private static List <CpuLoad> calcUsageIdlePairs(long[][] parsedFileContents) throws IndexOutOfBoundsException {

        return Arrays.stream(parsedFileContents)
                .map(cpuInfo -> new CpuLoad(LongStream.of(cpuInfo).sum(), cpuInfo[3]))
                .collect(Collectors.toList());
    }

    private static List<Short> calcCpuLoad(List <CpuLoad> initial, List <CpuLoad> subsequent)
    {
        return IntStream.range(0, Math.min(initial.size(), subsequent.size()))
                .mapToObj(i -> calcPercentageChange(initial.get(i), subsequent.get(i)))
                .collect(Collectors.toList());
    }

    private static short calcPercentageChange(CpuLoad before, CpuLoad after)
    {
        final long totalLoadDiff = after.getCpuLoadTotal() - before.getCpuLoadTotal();
        final long idleDiff = after.getCpuIdleValue() - before.getCpuIdleValue();
        final long cpuUsed = totalLoadDiff - idleDiff;
        return  (short)(100 * cpuUsed / totalLoadDiff);
    }

    /**
     * Triggers a procedure that parses the /proc/stat file of a Linux distribution
     * sleeps for 1 second and parses it again. Than calculates the CPU load in percent
     * using the passed Idle vs Load times.
     * @return a list containing the total CPU load in percent (as a first element)
     * followed by the separate individual CPU core loads (as subsequent List elements).
     */
    public static List<Short> readCpuLoad() {

        try {
            List <CpuLoad> initialUsage = calcUsageIdlePairs(parseProcStat());
            Thread.sleep(PROBING_INTERVAL_MS);
            List <CpuLoad> currentUsage = calcUsageIdlePairs(parseProcStat());

            return calcCpuLoad(initialUsage, currentUsage);
        } catch (NumberFormatException nfe) {
            System.out.println("Unexpected format: " + nfe.toString());
        } catch (IOException ioe) {
            System.out.println("File not accessible: " + ioe.toString());
        } catch (SecurityException se) {
            System.out.println("Permissions error: " + se.toString());
        } catch (IndexOutOfBoundsException oob) {
            System.out.println("Read data does not make sense: " + oob.toString());
        }catch (ArithmeticException ae) {
            System.out.println("File contents did not change between measurements: " + ae.toString());
        } catch (InterruptedException ie) {
            System.out.println("Sleep interrupt detected: " + ie.toString());
        }
        return Collections.emptyList();
    }

    /**
     * Call ReadCpuLoad non-blocking;
     * @return a list containing the total CPU load in percent (as a first element)
     * followed by the separate individual CPU core loads (as subsequent List elements).
     */
    public static Future<List<Short>> readCpuLoadAsync() {
        return CompletableFuture.supplyAsync(ProcStatParser::readCpuLoad)
                .orTimeout(PROBING_INTERVAL_MS * 3, TimeUnit.MILLISECONDS);
    }

    /**
     * Subscribe for CPU load changes
     * @return a Publisher des sends CPU load changes
     */
    public static Flow.Publisher<List<Short>> monitorCpuLoad()
    {
        return subscriber -> subscriber.onSubscribe(
                new CpuLoadSubscription(subscriber)
        );
    }
}
