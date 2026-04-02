package compare;

import llc.berserkr.cache.hash.CacheLocks;
import llc.berserkr.cache.hash.CacheLocksFactory;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class CacheLocksCompareTest {

    private static final Logger logger = LoggerFactory.getLogger(CacheLocksCompareTest.class);

    private static final int THREADS = 500;
    private static final int LOCK_COUNT = 200;
    private static final int OPS_PER_THREAD = 20000;
    private static final double WRITER_RATIO = 0.2;

    private static boolean nativeAvailable = false;

    static {
        try {
            System.loadLibrary("nativelib");
            nativeAvailable = true;
        } catch (UnsatisfiedLinkError e) {
            logger.warn("Native library not available: " + e.getMessage());
        }
    }

    @Test
    public void benchmarkCompare() throws InterruptedException {
        assumeTrue(nativeAvailable, "Native library not loaded");

        logger.info("=== CacheLocks Benchmark: Java vs Native ===");
        logger.info("Threads: " + THREADS + ", Locks: " + LOCK_COUNT +
                ", Ops/thread: " + OPS_PER_THREAD + ", Writer ratio: " + (int)(WRITER_RATIO * 100) + "%");
        logger.info("");

        // Warmup
        logger.info("Warming up...");
        runBenchmark("Java (warmup)", true, THREADS / 5, OPS_PER_THREAD / 10);
        runBenchmark("Native (warmup)", false, THREADS / 5, OPS_PER_THREAD / 10);
        logger.info("");

        // Actual runs
        BenchmarkResult javaResult = runBenchmark("Java", true, THREADS, OPS_PER_THREAD);
        BenchmarkResult nativeResult = runBenchmark("Native", false, THREADS, OPS_PER_THREAD);

        // Summary
        logger.info("");
        logger.info("╔══════════════════════════════════════════════════════════════════╗");
        logger.info("║                        BENCHMARK RESULTS                        ║");
        logger.info("╠══════════════════════════════════════════════════════════════════╣");
        logger.info(String.format("║  %-20s %18s %18s  ║", "", "Java", "Native"));
        logger.info("╠══════════════════════════════════════════════════════════════════╣");
        logger.info(String.format("║  %-20s %15d ms %15d ms  ║", "Total time", javaResult.totalTimeMs, nativeResult.totalTimeMs));
        logger.info(String.format("║  %-20s %15d    %15d     ║", "Total ops", javaResult.totalOps, nativeResult.totalOps));
        logger.info(String.format("║  %-20s %13.0f /s  %13.0f /s   ║", "Throughput", javaResult.throughput(), nativeResult.throughput()));
        logger.info(String.format("║  %-20s %15d ns %15d ns  ║", "Avg latency", javaResult.avgLatencyNs, nativeResult.avgLatencyNs));
        logger.info(String.format("║  %-20s %15d ns %15d ns  ║", "Max latency", javaResult.maxLatencyNs, nativeResult.maxLatencyNs));
        logger.info(String.format("║  %-20s %15d    %15d     ║", "Read ops", javaResult.readOps, nativeResult.readOps));
        logger.info(String.format("║  %-20s %15d    %15d     ║", "Write ops", javaResult.writeOps, nativeResult.writeOps));
        logger.info(String.format("║  %-20s %15d ns %15d ns  ║", "Avg read latency", javaResult.avgReadLatencyNs, nativeResult.avgReadLatencyNs));
        logger.info(String.format("║  %-20s %15d ns %15d ns  ║", "Avg write latency", javaResult.avgWriteLatencyNs, nativeResult.avgWriteLatencyNs));
        logger.info("╠══════════════════════════════════════════════════════════════════╣");

        double speedup = (double) javaResult.totalTimeMs / nativeResult.totalTimeMs;
        String winner = speedup > 1.0 ? "Native" : "Java";
        logger.info(String.format("║  Winner: %-10s  Speedup: %.2fx                              ║", winner, Math.max(speedup, 1.0 / speedup)));
        logger.info("╚══════════════════════════════════════════════════════════════════╝");
    }

    private BenchmarkResult runBenchmark(String name, boolean useJava, int threads, int opsPerThread)
            throws InterruptedException {

        CacheLocks[] locks = new CacheLocks[LOCK_COUNT];

        for (int i = 0; i < LOCK_COUNT; i++) {
            locks[i] = useJava
                    ? CacheLocksFactory.createJavaWithIgnoredWriteLocks()
                    : CacheLocksFactory.createNativeWithIgnoredWriteLocks();
        }

        ExecutorService pool = Executors.newFixedThreadPool(threads);

        AtomicLong totalLatencyNs = new AtomicLong(0);
        AtomicLong maxLatencyNs = new AtomicLong(0);
        AtomicLong readOps = new AtomicLong(0);
        AtomicLong writeOps = new AtomicLong(0);
        AtomicLong readLatencyNs = new AtomicLong(0);
        AtomicLong writeLatencyNs = new AtomicLong(0);

        long startTime = System.nanoTime();

        for (int t = 0; t < threads; t++) {
            pool.execute(() -> {
                ThreadLocalRandom rng = ThreadLocalRandom.current();

                for (int op = 0; op < opsPerThread; op++) {
                    int lockIdx = rng.nextInt(LOCK_COUNT);
                    boolean isWriter = rng.nextDouble() < WRITER_RATIO;
                    CacheLocks.LockType type = isWriter ? CacheLocks.LockType.WRITER : CacheLocks.LockType.READER;

                    long opStart = System.nanoTime();
                    try {
                        locks[lockIdx].getLock(type);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    locks[lockIdx].releaseLock(type);
                    long opEnd = System.nanoTime();

                    long elapsed = opEnd - opStart;
                    totalLatencyNs.addAndGet(elapsed);

                    long currentMax;
                    do {
                        currentMax = maxLatencyNs.get();
                    } while (elapsed > currentMax && !maxLatencyNs.compareAndSet(currentMax, elapsed));

                    if (isWriter) {
                        writeOps.incrementAndGet();
                        writeLatencyNs.addAndGet(elapsed);
                    } else {
                        readOps.incrementAndGet();
                        readLatencyNs.addAndGet(elapsed);
                    }
                }
            });
        }

        pool.shutdown();
        pool.awaitTermination(300, TimeUnit.SECONDS);

        long endTime = System.nanoTime();
        long totalOps = (long) threads * opsPerThread;
        long totalTimeMs = (endTime - startTime) / 1_000_000;
        long avgLatency = totalLatencyNs.get() / totalOps;
        long avgRead = readOps.get() > 0 ? readLatencyNs.get() / readOps.get() : 0;
        long avgWrite = writeOps.get() > 0 ? writeLatencyNs.get() / writeOps.get() : 0;

        BenchmarkResult result = new BenchmarkResult(
                totalTimeMs, totalOps, avgLatency, maxLatencyNs.get(),
                readOps.get(), writeOps.get(), avgRead, avgWrite
        );

        logger.info(String.format("  %-18s %6d ms | %,d ops | throughput: %,.0f ops/s | avg: %,d ns",
                name, totalTimeMs, totalOps, result.throughput(), avgLatency));

        return result;
    }

    private record BenchmarkResult(
            long totalTimeMs,
            long totalOps,
            long avgLatencyNs,
            long maxLatencyNs,
            long readOps,
            long writeOps,
            long avgReadLatencyNs,
            long avgWriteLatencyNs
    ) {
        double throughput() {
            return totalTimeMs > 0 ? (totalOps * 1000.0) / totalTimeMs : 0;
        }
    }
}
