package com.github.archturion64.procstatparser;

import com.github.archturion64.procstatparser.model.CpuLoad;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * Retrieves the current CPU load on a Linux based system by reading /proc/stat.
 * No dependencies required besides JRE and the Linux kernel.
 */
public class ProcStatParser {

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
    public static List<Short> ReadCpuLoad() {

        try {
            List <CpuLoad> initialUsage = calcUsageIdlePairs(parseProcStat());
            Thread.sleep(1000);
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
}
