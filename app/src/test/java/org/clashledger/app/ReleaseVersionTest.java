package org.clashledger.app;
import org.junit.Test;
import static org.junit.Assert.*;
public class ReleaseVersionTest {
    @Test public void numericVersionsAndPrefix(){assertTrue(ReleaseVersion.compare("v1.10.0","1.9.9")>0);assertEquals(0,ReleaseVersion.compare("v1.3.0","1.3.0"));assertTrue(ReleaseVersion.compare("1.2.1","1.3.0")<0);assertTrue(ReleaseVersion.compare("2.0.0","1.99.99")>0);}
    @Test public void unsupportedTagsCannotBeOfferedAsStableUpdates(){for(String v:new String[]{"", "latest", "1.3.0-beta", "1.3", "1.99999999.0", "https://evil.test"})try{ReleaseVersion.compare(v,"1.3.0");fail(v);}catch(IllegalArgumentException expected){}}
}
