package com.company;

import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Modified code from Herlihy, M., & Shavit, N. (2012).
 * The Art of Multiprocessor Programming,
 * Revised Reprint : Revised Reprint.
 * San Francisco: Elsevier Science & Technology.
 *
 *
 * @param <T>
 */


public class LazyConcurrentSkipList <T> {
    Lock lock = new ReentrantLock();
    Random r = new Random();
    static final int MAXLEVEL = 10;
    final Node<T> head = new Node<T>(Integer.MIN_VALUE);
    final Node<T> tail = new Node<T>(Integer.MAX_VALUE);

    public LazyConcurrentSkipList(){
        for(int i = 0; i < head.next.length; i++){
            head.next[i] = tail;
        }
    }

    int find(T x, Node<T>[] preds, Node<T>[] succs){
        int key = x.hashCode();
        int lFound = -1;
        Node<T> pred = head;
        for (int level = MAXLEVEL; level >= 0; level--){
            Node<T> curr = pred.next[level];
            while(key > curr.key){
                pred = curr;
                curr = pred.next[level];
            }
            if(lFound == -1 && key == curr.key){
                lFound = level;
            }
            preds[level] = pred;
            succs[level] = curr;
        }
        return lFound;
    }

    boolean add (T x){
        Random r = new Random();
        int topLevel = r.nextInt(MAXLEVEL); //Todo: randomize
        Node<T>[] preds = (Node<T>[]) new Node[MAXLEVEL+1];
        Node<T>[] succs = (Node<T>[]) new Node[MAXLEVEL+1];
        while(true) {
            int lFound = find(x, preds, succs);
            if(lFound != -1){
                Node<T> nodeFound = succs[lFound];
                if(!nodeFound.marked) {
                    while(!nodeFound.fullyLinked){}
                    return false;
                }
                continue;
            }
            int highestLocked = -1;
            try {
                Node<T> pred, succ;
                boolean valid = true;
                for (int level = 0; valid && (level <= topLevel); level++){
                    pred = preds[level];
                    succ = succs[level];
                    pred.lock.lock();
                    highestLocked = level;
                    valid = !pred.marked && !succ.marked && pred.next[level]==succ;
                }
                if(!valid) continue;
                Node<T> newNode = new Node(x, topLevel);
                for(int level = 0; level <= topLevel; level++){
                    newNode.next[level] = succs[level];
                }
                for(int level = 0; level <= topLevel; level++){
                    preds[level].next[level] = newNode;
                }
                newNode.fullyLinked = true;
                return true;
            } finally {
                for(int level = 0; level <= highestLocked; level++){
                    preds[level].unLock();
                }
            }
        }
    }

    Tuple add (T x, long startTime){
        int topLevel = r.nextInt(MAXLEVEL); //Todo: randomize
        Node<T>[] preds = (Node<T>[]) new Node[MAXLEVEL+1];
        Node<T>[] succs = (Node<T>[]) new Node[MAXLEVEL+1];
        while(true) {
            int lFound = find(x, preds, succs);
            if(lFound != -1){
                long endTime = System.nanoTime();
                Node<T> nodeFound = succs[lFound];
                if(!nodeFound.marked) {
                    while(!nodeFound.fullyLinked){}
                    return new Tuple().setResult(false).setTime(endTime-startTime);
                }
                continue;
            }
            int highestLocked = -1;
            try {
                Node<T> pred, succ;
                boolean valid = true;
                for (int level = 0; valid && (level <= topLevel); level++){
                    pred = preds[level];
                    succ = succs[level];
                    pred.lock.lock();
                    highestLocked = level;
                    valid = !pred.marked && !succ.marked && pred.next[level]==succ;
                }
                if(!valid) continue;
                Node<T> newNode = new Node(x, topLevel);
                for(int level = 0; level <= topLevel; level++){
                    newNode.next[level] = succs[level];
                }
                for(int level = 0; level <= topLevel; level++){
                    preds[level].next[level] = newNode;
                }
                newNode.fullyLinked = true;
                long endTime = System.nanoTime();
                return new Tuple().setResult(true).setTime(endTime-startTime);
            } finally {
                for(int level = 0; level <= highestLocked; level++){
                    preds[level].unLock();
                }
            }
        }
    }

    boolean contains(T x){
        Node<T>[] preds = (Node<T>[]) new Node[MAXLEVEL+1];
        Node<T>[] succs = (Node<T>[]) new Node[MAXLEVEL+1];
        int lFound = find(x, preds, succs);
        return (lFound != -1
        && succs[lFound].fullyLinked
        && !succs[lFound].marked);
    }

    Tuple contains(T x, long startTime){
        Node<T>[] preds = (Node<T>[]) new Node[MAXLEVEL+1];
        Node<T>[] succs = (Node<T>[]) new Node[MAXLEVEL+1];
        int lFound = find(x, preds, succs);
        long endTime = System.nanoTime();
        boolean returnValue = (lFound != -1
                && succs[lFound].fullyLinked
                && !succs[lFound].marked);
        return new Tuple().setResult(returnValue).setTime(endTime-startTime);
    }

    boolean remove(T x){
        Node<T> victim = null; boolean isMarked = false; int topLevel = -1;
        Node<T>[] preds = (Node<T>[]) new Node[MAXLEVEL + 1];
        Node<T>[] succs = (Node<T>[]) new Node[MAXLEVEL + 1];
        while (true){
            int lFound = find(x, preds, succs);
            if(lFound != -1) victim = succs[lFound];
            if(isMarked || (lFound != -1 &&
                    (victim.fullyLinked &&
                            victim.topLevel == lFound
                    && !victim.marked))){
                if(!isMarked) {
                    topLevel = victim.topLevel;
                    victim.lock.lock();
                    if(victim.marked){
                        victim.lock.unlock();
                        return false;
                    }
                    victim.marked = true;
                    isMarked = true;
                }
                int highestLocked = -1;
                try{
                    Node<T> pred, succ; boolean valid = true;
                    for(int level = 0; valid && (level <= topLevel); level++){
                        pred = preds[level];
                        pred.lock.lock();
                        highestLocked = level;
                        valid = !pred.marked && pred.next[level]==victim;
                    }
                    if(!valid) continue;
                    for(int level = topLevel; level >= 0; level--){
                        preds[level].next[level] = victim.next[level];
                    }
                    victim.lock.unlock();
                    return true;
                } finally {
                    for(int i = 0; i <= highestLocked; i++){
                        preds[i].unLock();
                    }
                }
            }else return false;
        }
    }

    Tuple remove(T x, long startTime){
        Node<T> victim = null; boolean isMarked = false; int topLevel = -1;
        Node<T>[] preds = (Node<T>[]) new Node[MAXLEVEL + 1];
        Node<T>[] succs = (Node<T>[]) new Node[MAXLEVEL + 1];
        while (true){
            int lFound = find(x, preds, succs);
            if(lFound != -1) victim = succs[lFound];
            if(isMarked || (lFound != -1 &&
                    (victim.fullyLinked &&
                            victim.topLevel == lFound
                            && !victim.marked))){
                long endTime = System.nanoTime();
                if(!isMarked) {
                    topLevel = victim.topLevel;
                    victim.lock.lock();
                    if(victim.marked){
                        victim.lock.unlock();
                        return new Tuple().setResult(false).setTime(endTime-startTime);
                    }
                    victim.marked = true;
                    isMarked = true;
                }
                int highestLocked = -1;
                try{
                    Node<T> pred, succ; boolean valid = true;
                    for(int level = 0; valid && (level <= topLevel); level++){
                        pred = preds[level];
                        pred.lock.lock();
                        highestLocked = level;
                        valid = !pred.marked && pred.next[level]==victim;
                    }
                    if(!valid) continue;
                    for(int level = topLevel; level >= 0; level--){
                        preds[level].next[level] = victim.next[level];
                    }
                    victim.lock.unlock();
                    return new Tuple().setResult(true).setTime(endTime-startTime);
                } finally {
                    for(int i = 0; i <= highestLocked; i++){
                        preds[i].unLock();
                    }
                }
            }else return new Tuple().setResult(false).setTime(System.nanoTime()-startTime);
        }
    }

    private static final class Node <T> {

        final Lock lock = new ReentrantLock();
        final T item;
        final int key;
        final Node<T>[] next;
        volatile boolean marked = false;
        volatile boolean fullyLinked = false;
        private int topLevel;

        public Node(int key){
            this.item = null;
            this.key = key;
            next = new Node[MAXLEVEL + 1];
            topLevel = MAXLEVEL;
        }

        public Node(T x, int height){
            item = x;
            key = x.hashCode();
            next = new Node[height + 1];
            topLevel = height;
        }

        public void doLock(){
            lock.lock();
        }

        public void unLock(){
            lock.unlock();
        }
    }
}

