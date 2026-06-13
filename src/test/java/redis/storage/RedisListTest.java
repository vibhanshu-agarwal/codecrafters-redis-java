package redis.storage;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RedisListTest {

    @Test
    void testRPushAndGetElements() {
        RedisList list = new RedisList();
        byte[] val1 = "item1".getBytes(StandardCharsets.UTF_8);
        byte[] val2 = "item2".getBytes(StandardCharsets.UTF_8);
        
        list.rpush(val1);
        list.rpush(val2);
        
        List<byte[]> elements = list.getElements();
        assertEquals(2, elements.size());
        assertArrayEquals(val1, elements.get(0));
        assertArrayEquals(val2, elements.get(1));
    }

    @Test
    void testRedisListNoExpirationByDefault() {
        RedisList list = new RedisList();
        assertFalse(list.isExpired());
        assertEquals(StoredValue.NO_EXPIRY, list.getExpiryTime());
    }
}
