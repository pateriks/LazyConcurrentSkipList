package com.company;

import java.util.Random;

public class TestInsertion {

    public static final int NO_NODES = 100;

    public static void main(String[] args) {

        Random r = new Random();
        LazyConcurrentSkipList<Integer> list = new LazyConcurrentSkipList<>();

        for(int i = 0; i < TestInsertion.NO_NODES; i++){
            int toAdd = r.nextInt(NO_NODES);
            list.add(toAdd);
            if(i%10==0){
                System.out.println("\n");
            }
            System.out.print(toAdd + " ");
        }

        System.out.println();
        /*
        Scanner in = new Scanner(System.in);
        boolean run = true;
        while(run){
            switch (in.next().toLowerCase()) {
                case "get":
                    int key = in.nextInt();
                    System.out.println(list.contains(key));
                    break;
                case "break": run = false; break;
            }
        }
        */

    }
}
