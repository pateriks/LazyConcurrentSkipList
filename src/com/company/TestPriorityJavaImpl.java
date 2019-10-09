package com.company;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TestPriorityJavaImpl {

    public final static int NO_NODES = 10000;
    public final static int NO_THREADS = 8;
    public final static int NO_TASKS = 10000;

    ForkJoinPool workPool;
    PriorityBlockingQueue<Integer> workList = new PriorityBlockingQueue<>();
    Random random = new Random();
    HashMap<Long, Long> times = new HashMap<>();

    public TestPriorityJavaImpl(){
        workPool = new ForkJoinPool(NO_THREADS-1);
    }

    public static void main (String[] args){
        TestPriorityJavaImpl test = new TestPriorityJavaImpl();
        test.createPopulation(NO_NODES);
        test.workPool.awaitQuiescence(10, TimeUnit.SECONDS);
        //testPriority.printPopulation();

        float proportionAdd = (float) 0.05;
        float proportionDel = (float) 0.05;
        float proportionCon = (float) 0.9;

        long time = test.startTest(proportionAdd, proportionDel + proportionAdd, proportionCon + proportionAdd + proportionDel);
        System.out.println("Total time: " + time);
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
        PriorityBlockingQueue<Integer> list;

        void addToPool() {
            pool.execute(this);
        }

        @Override
        public void run() {
            try {
                if (task_type == 1) {
                    list.put(key);
                } else if (task_type == 2) {
                    list.take();
                } else {
                    list.contains(key);
                }
            }catch (Exception e){

            }
        }
    }
}
