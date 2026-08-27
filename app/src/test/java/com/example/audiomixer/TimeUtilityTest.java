package com.example.audiomixer;

import static org.junit.Assert.assertEquals;
import com.example.audiomixer.utils.TimeUtility;
import org.junit.Test;

public class TimeUtilityTest {

    @Test
    public void testGetFormattedDuration_SecondsOnly() {
        assertEquals("0:05", TimeUtility.getFormattedDuration(5000));
    }

    @Test
    public void testGetFormattedDuration_MinutesAndSeconds() {
        assertEquals("1:05", TimeUtility.getFormattedDuration(65000));
    }

    @Test
    public void testGetFormattedDuration_LongDuration() {
        // 70 minutes (4200000ms)
        assertEquals("70:00", TimeUtility.getFormattedDuration(4200000));
    }

    @Test
    public void testGetFormattedDuration_Zero() {
        assertEquals("0:00", TimeUtility.getFormattedDuration(0));
    }

    @Test
    public void testNegativeDuration_DefaultToZero() {
        assertEquals("0:00", TimeUtility.getFormattedDuration(-1000));
    }
}
