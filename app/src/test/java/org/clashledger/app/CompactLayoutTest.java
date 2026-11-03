package org.clashledger.app;
import org.junit.Test;
import static org.junit.Assert.*;
public class CompactLayoutTest {
    @Test public void smallWidgetReportsOverflowWithoutScrolling() {
        CompactLayout l=new CompactLayout(130,120);assertEquals(2,l.capacity);assertEquals(2,l.visible(13));assertEquals(11,l.overflow(13));
    }
    @Test public void tallerWidgetFitsMoreIcons() { assertTrue(new CompactLayout(320,350).capacity>new CompactLayout(320,170).capacity); }
    @Test public void boundsLimitRemoteViewBitmapPayload() {
        CompactLayout l=new CompactLayout(3000,3000);assertEquals(24,l.capacity);assertEquals(24,l.visible(300));assertEquals(276,l.overflow(300));
    }
    @Test public void zeroAndUnknownDimensionsAreSafe() {CompactLayout l=new CompactLayout(0,0);assertEquals(0,l.visible(0));assertEquals(0,l.overflow(0));assertTrue(l.capacity>0);}
}
