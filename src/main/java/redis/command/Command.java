package redis.command;

import redis.storage.StoredValue;

import java.util.List;
import java.util.Map;

public interface Command {
    byte[] execute(List<byte[]> args, Map<String, StoredValue> keyValuePairs);
}
