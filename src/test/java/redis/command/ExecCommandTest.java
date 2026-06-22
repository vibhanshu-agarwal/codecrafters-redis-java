package redis.command;

import org.junit.jupiter.api.Test;
import redis.storage.StoredValue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExecCommandTest {

    @Test
    void testExecuteWithoutMulti() {
        ExecCommand command = new ExecCommand();
        Map<String, StoredValue> storage = new HashMap<>();
        List<byte[]> args = new ArrayList<>();

        byte[] result = command.execute(args, storage);

        assertEquals("-ERR EXEC without MULTI\r\n", new String(result, StandardCharsets.UTF_8));
    }
}
