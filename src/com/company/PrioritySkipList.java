package com.company;

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

public class PrioritySkipList <T> {
    final Node<T> head = new Node<T>(Integer.MIN_VALUE);
    final Node<T> tail = new Node<T>(Integer.MAX_VALUE);

    static final int MAXLEVEL = 10;

    public PrioritySkipList(){
        for(int i = 0; i < head.next.length; i++){
            head.next[i] = tail;
        }
    }

    public static final class Node<T> {

        final java.util.concurrent.locks.Lock lock = new ReentrantLock();
        final T item;
        final int score;
        final Node<T>[] next;
        int key;
        volatile boolean marked;
        volatile boolean fullyLinked;

        public Node(int myPriority) {
            this.score = myPriority;
            this.item = null;
            this.marked = false;

            next = new Node[MAXLEVEL + 1];
        }

        public Node(T x, int myPriority) {
            this.key = x.hashCode();
            this.item = (T) x;
            this.score = myPriority;
            this.marked = false;
            next = new Node [myPriority+1]; //myPririty same as height?
        }
    }

    boolean contains(Node<T> x){
        Node<T>[] preds = (Node<T>[]) new Node[MAXLEVEL+1];
        Node<T>[] succs = (Node<T>[]) new Node[MAXLEVEL+1];
        int lFound = find(x, preds, succs);
        return (lFound != -1
                && succs[lFound].fullyLinked
                && !succs[lFound].marked);
    }

    boolean add (Node<T> node){
        T x = node.item;
        int score = node.score;

        //int topLevel = MAXLEVEL; //Todo: randomize

        Node<T>[] preds = (Node<T>[]) new Node[MAXLEVEL+1];
        Node<T>[] succs = (Node<T>[]) new Node[MAXLEVEL+1];

        while(true) {
            int lFound = find(node, preds, succs);
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
                for (int level = 0; valid && (level <= score) && (level <= MAXLEVEL); level++){
                    pred = preds[level];
                    succ = succs[level];
                    pred.lock.lock();
                    highestLocked = level;
                    valid = !pred.marked && !succ.marked && pred.next[level]==succ;
                }
                if(!valid) continue;
                Node<T> newNode = new Node(x, score);
                for(int level = 0; level <= score && level <= MAXLEVEL; level++){
                    newNode.next[level] = succs[level];
                }
                for(int level = 0; level <= score && level <= MAXLEVEL; level++){
                    preds[level].next[level] = newNode;
                }
                newNode.fullyLinked = true;
                return true;
            } finally {
                for(int level = 0; level <= highestLocked; level++){
                    preds[level].lock.unlock();
                }
            }
        }
    }

    boolean remove (Node<T> node){
        return false;
    }

    public Node<T> findAndMarkMin(){
        Node<T> curr;
        curr = head.next[0];
        while(curr != tail){
            curr.lock.lock();
            if (!curr.marked) {
                if (!curr.marked) {
                    curr.marked = true;
                    return curr;
                } else {
                    curr.lock.unlock();
                    curr = curr.next[0];
                }
            }else {
                curr.lock.unlock();
                curr = curr.next[0];
            }
        }//no unmarked nodes
        return null;
    }

    int find(Node<T> x, Node<T>[] preds, Node<T>[] succs){
        int key = x.key;
        int lFound = -1;
        Node<T> pred = head;
        for (int level = MAXLEVEL; level >= 0; level--){
            Node<T> curr = pred.next[level];
            while(x.score >= curr.score){
                pred = curr;
                curr = pred.next[level];
            }
            if(lFound == -1 && curr != head && curr != tail && key == curr.item.hashCode()){
                lFound = level;
            }
            //System.out.println("Level " + lFound);
            preds[level] = pred;
            succs[level] = curr;
        }
        return lFound;
    }
}
