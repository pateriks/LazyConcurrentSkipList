package com.company;

/**
 * Modified code from Herlihy, M., & Shavit, N. (2012).
 * The Art of Multiprocessor Programming,
 * Revised Reprint : Revised Reprint.
 * San Francisco: Elsevier Science & Technology.
 *
 *
 * @param <T>
 */


public class SkipQueue<T> {
    PrioritySkipList<T> skipList;
    public SkipQueue(){
        skipList = new PrioritySkipList<T>();

    }
    public boolean add (T item, int score){
        PrioritySkipList.Node node = new PrioritySkipList.Node(item, score);
        return skipList.add(node);
    }
    public T removeMin(){
        PrioritySkipList.Node<T> node = skipList.findAndMarkMin();
        if(node != null){
            skipList.remove(node);
            return node.item;
        } else {
            return null;
        }
    }
    public boolean contains(T item, int score){
        return skipList.contains(new PrioritySkipList.Node<>(item, score));
    }
}
