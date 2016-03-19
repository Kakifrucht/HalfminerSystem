package de.halfminer.hms.util;

public class Pair<L, R> {

    private L left;
    private R right;

    /**
     * Create a new pair
     * @param left left object
     * @param right right object
     */
    public Pair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    /**
     * @return specified object on the left
     */
    public L getLeft() {
        return left;
    }

    /**
     * @return specified object on the left
     */
    public R getRight() {
        return right;
    }

    /**
     * Update value of left node
     * @param setTo value to set
     */
    public void setLeft(L setTo) {
        left = setTo;
    }

    /**
     * Update value of right node
     * @param setTo value to set
     */
    public void setRight(R setTo) {
        right = setTo;
    }
}
