package redis.acl;

public class ConnectionAuth {
  private boolean authenticated;

  public ConnectionAuth() {
    authenticated = AclUserStore.getInstance().hasNopass();
  }

  public boolean isAuthenticated() {
    return authenticated;
  }

  public void authenticate() {
    authenticated = true;
  }
}
