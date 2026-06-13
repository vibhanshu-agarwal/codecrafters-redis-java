package redis.storage;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class RedisStringTest {

    @Test
    void testRedisStringValue() {
        byte[] value = "hello".getBytes(StandardCharsets.UTF_8);
        RedisString rs = new RedisString(value);
        assertArrayEquals(value, rs.getValue());
        assertFalse(rs.isExpired());
    }

    @Test
    void testRedisStringExpiration() throws InterruptedException {
        byte[] value = "hello".getBytes(StandardCharsets.UTF_8);
        long expiry = System.currentTimeMillis() + 100; // 100ms from now
        RedisString rs = new RedisString(value, expiry);
        
        assertFalse(rs.isExpired());
        
        Thread.sleep(150);
        assertTrue(rs.isExpired());
    }

    @Test
    void testRedisStringNoExpiration() {
        byte[] value = "hello".getBytes(StandardCharsets.UTF_8);
        RedisString rs = new RedisString(value, -1);
        assertFalse(rs.isExpired());
    }
}
