package com.luixtech.uidgenerator.core.uid.impl;

import com.luixtech.uidgenerator.core.epochseconds.EpochSecondsService;
import com.luixtech.uidgenerator.core.worker.WorkerIdAssigner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class CachedUidGeneratorTest {
    private static final int     SIZE    = 7_000_000; // 700w
    private static final boolean VERBOSE = false;
    private static final int     THREADS = Runtime.getRuntime().availableProcessors() << 1;

    private static CachedUidGenerator uidGenerator;

    @BeforeAll
    public static void setup() {
        uidGenerator = new CachedUidGenerator();
        uidGenerator.setWorkerIdAssigner(getWorkerIdAssigner());
        uidGenerator.setEpochSecondsService(getEpochSecondsService());
        uidGenerator.setDeltaSecondsBits(29);
        uidGenerator.setWorkerBits(12);
        uidGenerator.setSequenceBits(22);
        uidGenerator.initialize();
    }

    private static WorkerIdAssigner getWorkerIdAssigner() {
        return () -> 1L;
    }

    private static EpochSecondsService getEpochSecondsService() {
        return () -> {
            Date now = new Date();
            // Truncate to zero clock
            return DateUtils.truncate(now, Calendar.DATE).getTime() / 1000;
        };
    }

    /**
     * Test for serially generate
     *
     * @throws IOException
     */
    @Test
    public void testSerialGenerate() throws IOException {
        // Generate UID serially
        Set<Long> uidSet = new HashSet<>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            doGenerate(uidSet, i);
        }

        // Check UIDs are all unique
        checkUniqueID(uidSet);
    }

    /**
     * Test for parallel generate
     *
     * @throws InterruptedException
     * @throws IOException
     */
    @Test
    public void testParallelGenerate() throws InterruptedException, IOException {
        AtomicInteger control = new AtomicInteger(-1);
        Set<Long> uidSet = new ConcurrentSkipListSet<>();

        // Initialize threads
        List<Thread> threadList = new ArrayList<>(THREADS);
        for (int i = 0; i < THREADS; i++) {
            Thread thread = new Thread(() -> workerRun(uidSet, control));
            thread.setName("UID-generator-" + i);

            threadList.add(thread);
            thread.start();
        }

        // Wait for worker done
        for (Thread thread : threadList) {
            thread.join();
        }

        // Check generate 700w times
        assertThat(control.get()).isEqualTo(SIZE);

        // Check UIDs are all unique
        checkUniqueID(uidSet);
    }

    /**
     * Worker run
     */
    private void workerRun(Set<Long> uidSet, AtomicInteger control) {
        for (; ; ) {
            int myPosition = control.updateAndGet(old -> (old == SIZE ? SIZE : old + 1));
            if (myPosition == SIZE) {
                return;
            }

            doGenerate(uidSet, myPosition);
        }
    }

    /**
     * Do generating
     */
    private void doGenerate(Set<Long> uidSet, int index) {
        long uid = uidGenerator.generateUid();
        String parsedInfo = uidGenerator.parseUid(uid);
        boolean existed = !uidSet.add(uid);
        if (existed) {
            System.out.println("Found duplicated UID " + uid);
        }

        // Check UID is positive, and can be parsed
        assertThat(uid).isPositive();
        assertThat(parsedInfo).isNotEmpty();

        if (VERBOSE) {
            System.out.println(Thread.currentThread().getName() + " No." + index + " >>> " + parsedInfo);
        }
    }

    /**
     * Check UIDs are all unique
     */
    private void checkUniqueID(Set<Long> uidSet) {
        log.info("Size: {}", uidSet.size());
        assertThat(uidSet.size()).isEqualTo(SIZE);
    }
}
