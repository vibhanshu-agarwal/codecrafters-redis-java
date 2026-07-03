package redis.acl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AclUserStore {
  private static final AclUserStore INSTANCE = new AclUserStore();

  private final Set<String> flags = new LinkedHashSet<>();
  private final List<String> passwords = new ArrayList<>();

  private AclUserStore() {
    flags.add("nopass");
  }

  public static AclUserStore getInstance() {
    return INSTANCE;
  }

  public Set<String> getFlags() {
    return flags;
  }

  public List<String> getPasswords() {
    return passwords;
  }

  public void setPassword(String password) {
    flags.remove("nopass");
    passwords.add(sha256(password));
  }

  public void resetForTests() {
    flags.clear();
    flags.add("nopass");
    passwords.clear();
  }

  private static String sha256(String password) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder(hash.length * 2);
      for (byte b : hash) {
        hex.append(String.format("%02x", b));
      }
      return hex.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }
}
