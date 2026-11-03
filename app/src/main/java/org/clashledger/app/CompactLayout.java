package org.clashledger.app;

/** Fits whole tiles; overflow is reported instead of creating a scrolling collection. */
public final class CompactLayout {
    public final int columns,rows,capacity;
    public CompactLayout(int widthDp,int heightDp) {
        columns=Math.max(2,Math.min(6,(widthDp-24)/60));
        rows=Math.max(1,Math.min(4,(heightDp-76)/68));
        capacity=columns*rows;
    }
    public int visible(int count) { return Math.min(Math.max(0,count),capacity); }
    public int overflow(int count) { return Math.max(0,count-capacity); }
}
