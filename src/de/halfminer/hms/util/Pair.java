package de.halfminer.hms.util;

public class Pair<L, R> {

    private final L left;
    private final R right;

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
}
