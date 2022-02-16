package jmxClient;

import static org.junit.jupiter.api.Assertions.*;

class jmxClientTest {

    @org.junit.jupiter.api.Test
    void findNew() {
        long prev[] = {40000000,40000001,40000002,40000003,40000004,40000005,40000006,40000007,40000008,40000009};
        long curr[] = {40000000,40000001,40000002,40000003,40000004,30000005,30000006,30000007,30000008,30000009};

        long exp[] = {30000005,30000006,30000007,30000008,30000009};
        assertArrayEquals(exp, jmxClient.findNew(prev, curr, 5));
    }

    @org.junit.jupiter.api.Test
    void listDiff() {
        long prev[] = {40000000,40000001,30000002,30000003,30000004,30000005,30000006,40000007,40000008,40000009};
        long curr[] = {40000000,40000001,40000002,40000003,40000004,40000005,40000006,40000007,40000008,40000009};

        long exp[] = {0,0,40000002,40000003,40000004,40000005,40000006,0,0,0};
        assertArrayEquals(exp, jmxClient.listDiff(prev, curr));
    }

    @org.junit.jupiter.api.Test
    void getNonZero() {
        long array[] = {0,0,40000002,40000003,40000004,40000005,40000006,0,0,0};
        int start = 2;
        int max = 5;

        long exp[] = {40000002,40000003,40000004,40000005,40000006};
        assertArrayEquals(exp, jmxClient.getNonZero(array, start, max));
    }
}