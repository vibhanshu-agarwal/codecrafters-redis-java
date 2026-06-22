package redis.command;

import org.junit.jupiter.api.Test;
import redis.protocol.RespResponse;
import redis.storage.RedisString;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IncrCommandTest {

    @Test
    void testExecuteWithExistingNumericValue() {
        IncrCommand command = new IncrCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        String key = "foo";
        storage.put(key, new RedisString("41".getBytes(StandardCharsets.UTF_8)));

        List<byte[]> args = new ArrayList<>();
        args.add(key.getBytes(StandardCharsets.UTF_8));

        byte[] result = command.execute(args, storage);

        assertEquals(":42\r\n", new String(result, StandardCharsets.UTF_8));
        assertEquals("42", storage.get(key).toString());
    }

    @Test
    void testExecuteWithNonExistingKey() {
        IncrCommand command = new IncrCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        String key = "bar";

        List<byte[]> args = new ArrayList<>();
        args.add(key.getBytes(StandardCharsets.UTF_8));

        byte[] result = command.execute(args, storage);

        assertEquals(":1\r\n", new String(result, StandardCharsets.UTF_8));
        assertEquals("1", storage.get(key).toString());
    }

    @Test
    void testExecuteWithInvalidValue() {
        IncrCommand command = new IncrCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        String key = "baz";
        storage.put(key, new RedisString("abc".getBytes(StandardCharsets.UTF_8)));

        List<byte[]> args = new ArrayList<>();
        args.add(key.getBytes(StandardCharsets.UTF_8));

        byte[] result = command.execute(args, storage);

        String response = new String(result, StandardCharsets.UTF_8);
        assertTrue(response.startsWith("-ERR"), "Expected error response for non-numeric value");
    }

    @Test
    void testExecuteWithWrongType() {
        // This might be for later stages, but good to have
        IncrCommand command = new IncrCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        String key = "list";
        // Assuming some other StoredValue type exists, e.g., RedisList
        // But I'll just check if it returns RespResponse.wrongType()
        storage.put(key, new StoredValue(StoredValue.NO_EXPIRY) {
            @Override
            public byte[] getType() {
                return new byte[0];
            }
        });

        List<byte[]> args = new ArrayList<>();
        args.add(key.getBytes(StandardCharsets.UTF_8));

        byte[] result = command.execute(args, storage);
        assertEquals(new String(RespResponse.wrongType(), StandardCharsets.UTF_8), new String(result, StandardCharsets.UTF_8));
    }
}
