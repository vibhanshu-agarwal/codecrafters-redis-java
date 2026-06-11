package redis.command;

import java.util.List;
import java.util.Map;

public interface Command {
    byte[] execute(List<byte[]> args, Map<String, byte[]> keyValuePairs);
}
