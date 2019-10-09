package com.company;

import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

public class TestParallelTask {
    public final static int NO_NODES = 10000;
    public final static int NO_THREADS = 24;
    public final static int NO_TASKS = 10000;
    ForkJoinPool workPool;
    LazyConcurrentSkipList<Integer> workList = new LazyConcurrentSkipList<>();
    Random random = new Random();

    public static void main (String [] args){
        //dynamic environment
        TestParallelTask test = new TestParallelTask();

        test.workPool = new ForkJoinPool(NO_THREADS-1);//1+parallelism=threads
        test.createPopulation(NO_NODES);
        test.workPool.awaitQuiescence(10, TimeUnit.SECONDS);

        while(true) {
            System.out.println("Every input must be a float");
            Scanner in = new Scanner(System.in);
            System.out.println("Proportion add");
            float proportionAdd = in.nextFloat();
            System.out.println("Proportion delete");
            float proportionDel = in.nextFloat();
            System.out.println("Proportion contain");
            float proportionCon = in.nextFloat();

            System.out.println("Testing to expose workList with " + proportionAdd + " add " + proportionCon + " contain " + proportionDel + " delete");
            //1 == add, 2 == delete, 3 == contain
            System.out.println("Nanotime: " + test.startTest(proportionAdd, proportionDel+proportionAdd, proportionCon+proportionAdd+proportionDel));
        }
    }

    void createPopulation(int amount){
        for (int i = 0; i < amount; i++) {
            workPool.execute(new Runnable() {
                @Override
                public void run() {
                    workList.add(random.nextInt(amount));
                }
            });
        }
    }

    public long startTest(float add, float remove, float contain){
        ListTask[] tasks = new ListTask[NO_TASKS];

        for (int i = 0; i < NO_TASKS; i++){
            ListTask toAdd = new ListTask();
            float percentage = random.nextFloat();
            if(percentage<add){
                toAdd.task_type = 1;
            }else if(percentage-add<remove){
                toAdd.task_type = 2;
            }else if(percentage-add-remove<contain){
                toAdd.task_type = 3;
            }
            toAdd.key = random.nextInt(NO_NODES);
            toAdd.list = workList;
            toAdd.pool = workPool;
            tasks[i] = toAdd;
        }

        long start = System.nanoTime();
        Arrays.stream(tasks).forEach(ListTask::addToPool);
        workPool.awaitQuiescence(10, TimeUnit.SECONDS);
        long end = System.nanoTime();
        return end-start;
    }

    private class ListTask implements Runnable{

        int task_type;
        int key;
        ForkJoinPool pool;
        LazyConcurrentSkipList list;

        void addToPool(){
            pool.execute(this);
        }

        @Override
        public void run() {
            if(task_type==1){
                list.add(key);
            }else if(task_type==2){
                list.remove(key);
            }else{
                list.contains(key);
            }
        }
    }
}
