package com.company;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

public class TestLinearization {
    public final static int NO_NODES = 10000;
    public final static int NO_THREADS = 8;
    public final static int NO_TASKS = 10;
    ForkJoinPool workPool;
    LazyConcurrentSkipList<Integer> workList = new LazyConcurrentSkipList<>();
    Random random = new Random();
    HashMap<Long, Long> times = new HashMap<>();

    public static void main(String[] args) {
        TestLinearization test = new TestLinearization();
        test.workPool = new ForkJoinPool(NO_THREADS - 1);
        test.createPopulation(NO_NODES);
        test.workPool.awaitQuiescence(10, TimeUnit.SECONDS);

        while (true) {
            Scanner in = new Scanner(System.in);
            System.out.println("Proportion add");
            float proportionAdd = in.nextFloat();
            System.out.println("Proportion delete");
            float proportionDel = in.nextFloat();
            System.out.println("Proportion contain");
            float proportionCon = in.nextFloat();

            System.out.println("Testing to expose workList with " + proportionAdd + " add " + proportionCon + " contain " + proportionDel + " delete");
            //1 == add, 2 == delete, 3 == contain
            System.out.println("Totaltime ns: " + test.startTest(proportionAdd, proportionDel + proportionAdd, proportionCon + proportionAdd + proportionDel));
            System.out.println("Threadtime ns: " + test.times);
            test.times = new HashMap<>();

        }
    }

    void createPopulation(int amount) {
        for (int i = 0; i < amount; i++) {
            workPool.execute(new Runnable() {
                @Override
                public void run() {
                    workList.add(random.nextInt(amount));
                }
            });
        }
    }

    public long startTest(float add, float remove, float contain) {
        ListTask[] tasks = new ListTask[NO_TASKS];
        for (int i = 0; i < NO_TASKS; i++) {
            ListTask toAdd = new ListTask();
            float percentage = random.nextFloat();
            if (percentage < add) {
                toAdd.task_type = 1;
            } else if (percentage - add < remove) {
                toAdd.task_type = 2;
            } else if (percentage - add - remove < contain) {
                toAdd.task_type = 3;
            }
            toAdd.key = random.nextInt(NO_NODES);
            toAdd.list = this.workList;
            toAdd.pool = workPool;
            toAdd.times = this.times;
            tasks[i] = toAdd;
        }

        long start = System.nanoTime();
        Arrays.stream(tasks).parallel().forEach(ListTask::addToPool);
        workPool.awaitQuiescence(10, TimeUnit.SECONDS);
        long end = System.nanoTime();
        return end - start;
    }

    private class ListTask implements Runnable {

        HashMap<Long, Long> times;
        int task_type;
        int key;
        ForkJoinPool pool;
        LazyConcurrentSkipList list;

        void addToPool() {
            pool.execute(this);
        }

        @Override
        public void run() {
            if (task_type == 1) {
                Tuple tuple = list.add(key, 0);
                System.out.println(Thread.currentThread().getId() + " add @" + tuple.time);
                if (!times.containsKey(Thread.currentThread().getId())) {
                    times.put(Thread.currentThread().getId(), tuple.time);
                } else {
                    Long val = times.get(Thread.currentThread().getId());
                    times.put(Thread.currentThread().getId(), val + tuple.time);
                }
            } else if (task_type == 2) {
                Tuple tuple = list.remove(key, 0);
                System.out.println(Thread.currentThread().getId() + " remove @" + tuple.time);
                if (!times.containsKey(Thread.currentThread().getId())) {
                    times.put(Thread.currentThread().getId(), tuple.time);
                } else {
                    Long val = times.get(Thread.currentThread().getId());
                    times.put(Thread.currentThread().getId(), val + tuple.time);
                }
            } else {
                Tuple tuple = list.contains(key, 0);
                System.out.println(Thread.currentThread().getId() + " contains @" + tuple.time);
                if (!times.containsKey(Thread.currentThread().getId())) {
                    times.put(Thread.currentThread().getId(), tuple.time);
                } else {
                    Long val = times.get(Thread.currentThread().getId());
                    times.put(Thread.currentThread().getId(), val + tuple.time);
                }
            }
        }
    }
}
