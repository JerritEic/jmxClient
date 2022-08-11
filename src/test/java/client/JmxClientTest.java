package client;

import static org.junit.jupiter.api.Assertions.*;

class JmxClientTest {

    @org.junit.jupiter.api.Test
    void findNew() {

        assertArrayEquals(new long[] {30000005,30000006,30000007,30000008,30000009}, JmxClient.findNew(
                new long[] {40000000,40000001,40000002,40000003,40000004,40000005,40000006,40000007,40000008,40000009},
                new long[] {40000000,40000001,40000002,40000003,40000004,30000005,30000006,30000007,30000008,30000009}));

        assertArrayEquals(new long[] {11,12,13}, JmxClient.findNew(
                new long[] {1,2,3,4,5,6,7,8,9,10},
                new long[] {1,2,3,11,12,13,7,8,9,10}));

        assertArrayEquals(new long[] {11,12,13}, JmxClient.findNew(
                new long[] {1,2,3,4,5,6,7,8,9,10},
                new long[] {11,12,13,4,5,6,7,8,9,10}));

        assertArrayEquals(new long[] {11,12,13,14}, JmxClient.findNew(
                new long[] {1,2,3,4,5,6,7,8,9,10},
                new long[] {13,14,3,4,5,6,7,8,11,12}));

        assertArrayEquals(new long[] {11,12,5,13,14}, JmxClient.findNew(
                new long[] {1,2,3,4,5,6,7,8,9,10},
                new long[] {1,2,11,12,5,13,14,8,9,10}));

        assertArrayEquals(new long[] {11,12,1,13,14}, JmxClient.findNew(
                new long[] {1,2,3,4,5,6,7,8,9,10},
                new long[] {1,13,14,4,5,6,7,8,11,12}));

        assertArrayEquals(new long[] {11,2,13,4,15}, JmxClient.findNew(
                new long[] {1,2,3,4,5,6,7,8,9,10},
                new long[] {11,2,13,4,15,6,7,8,9,10}));
    }

    @org.junit.jupiter.api.Test
    void listDiff() {
        long prev[] = {40000000,40000001,30000002,30000003,30000004,30000005,30000006,40000007,40000008,40000009};
        long curr[] = {40000000,40000001,40000002,40000003,40000004,40000005,40000006,40000007,40000008,40000009};

        long exp[] = {0,0,40000002,40000003,40000004,40000005,40000006,0,0,0};
        assertArrayEquals(exp, JmxClient.listDiff(prev, curr));
    }

    @org.junit.jupiter.api.Test
    void getNonZero() {
        long array[] = {0,0,40000002,40000003,40000004,40000005,40000006,0,0,0};
        int start = 2;
        int max = 5;

        long exp[] = {40000002,40000003,40000004,40000005,40000006};
        assertArrayEquals(exp, JmxClient.getNonZero(array, start, max));
    }
}