package com.camobile.camflake;

import mockit.Expectations;
import mockit.Mocked;
import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * {@link DefaultMachineId}に対する単体テスト
 */
public class DefaultMachineIdTest {

    @Test
    public void testGetId(@Mocked InetAddress inetAddress) {
        new Expectations() {
            {
                inetAddress.getAddress();
                result = new byte[]{127, 0, 0, 1};

                inetAddress.getHostAddress();
                result = "test_host";
            }
        };

        DefaultMachineId testee = new DefaultMachineId();
        assertThat(testee.getId(), is(1));
    }

    @Test(expected = CamflakeException.class)
    public void testGetIdThrowsExceptionWhenErrorOccurs() throws Exception {
        new Expectations(InetAddress.class) {
            {
                InetAddress.getLocalHost();
                result = new UnknownHostException();
            }
        };

        DefaultMachineId testee = new DefaultMachineId();
        testee.getId();
    }
}
