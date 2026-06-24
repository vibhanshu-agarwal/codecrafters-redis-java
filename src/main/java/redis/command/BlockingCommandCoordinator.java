package redis.command;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import redis.protocol.RespResponse;

/**
 * Coordinates commands that need to block until a key changes.
 *
 * <p>The command implementations keep the Redis-specific behavior, such as what it means for a
 * command to be ready. This class owns only the concurrency behavior: waiters are queued in arrival
 * order per key, key changes wake the oldest waiter, and timeout accounting is handled consistently
 * for all future blocking commands.
 */
public final class BlockingCommandCoordinator {
  private static final long NANOS_PER_SECOND = 1_000_000_000L;

  /*
   * The per-key waiter queue is the source of truth for FIFO ordering. A fair lock makes lock
   * reacquisition more predictable when many client threads are blocked or waking at the same time.
   */
  private static final ReentrantLock LOCK = new ReentrantLock(true);
  private static final Map<String, Deque<Condition>> WAITERS_BY_KEY = new HashMap<>();

  private BlockingCommandCoordinator() {}

  /**
   * Exposes the shared lock so commands that mutate watched values can make their mutation and
   * notification atomic with respect to blocked commands.
   */
  public static ReentrantLock lock() {
    return LOCK;
  }

  /**
   * Waits until {@code responseSupplier} can produce a response for {@code key}.
   *
   * <p>The supplier is invoked while holding the coordinator lock. That is deliberate: the final
   * readiness check and the state mutation that completes a blocking command must happen as one
   * protected operation. For BLPOP, checking the list and removing the popped element cannot be
   * separated.
   *
   * @param key the key whose changes can unblock this command
   * @param timeoutSeconds timeout in seconds; 0 means wait indefinitely
   * @param responseSupplier returns a command response when ready, or {@code null} when the command
   *     must keep waiting
   * @return the command response, or a RESP null array when the timeout expires
   */
  public static byte[] await(String key, double timeoutSeconds, Supplier<byte[]> responseSupplier)
      throws InterruptedException {
    return await(java.util.List.of(key), timeoutSeconds, responseSupplier);
  }

  /**
   * Waits until {@code responseSupplier} can produce a response for any of the {@code keys}.
   *
   * @param keys the keys whose changes can unblock this command
   * @param timeoutSeconds timeout in seconds; 0 means wait indefinitely
   * @param responseSupplier returns a command response when ready, or {@code null} when the command
   *     must keep waiting
   * @return the command response, or a RESP null array when the timeout expires
   */
  public static byte[] await(
      Collection<String> keys, double timeoutSeconds, Supplier<byte[]> responseSupplier)
      throws InterruptedException {
    LOCK.lock();
    try {
      /*
       * Fast path: a command may complete immediately only when there is no older waiter for
       * ALL the keys.
       */
      boolean canTryImmediately = true;
      for (String key : keys) {
        Deque<Condition> waiters = WAITERS_BY_KEY.get(key);
        if (waiters != null && !waiters.isEmpty()) {
          canTryImmediately = false;
          break;
        }
      }

      if (canTryImmediately) {
        byte[] response = responseSupplier.get();
        if (response != null) {
          return response;
        }
      }

      Condition waiter = LOCK.newCondition();
      for (String key : keys) {
        WAITERS_BY_KEY.computeIfAbsent(key, ignored -> new ArrayDeque<>()).addLast(waiter);
      }

      long remainingNanos = timeoutToNanos(timeoutSeconds);

      while (true) {
        /*
         * Only the oldest waiter gets to test readiness. For multiple keys, if we are the oldest
         * on ANY of them, we check readiness.
         */
        boolean isFirstAny = false;
        for (String key : keys) {
          Deque<Condition> waiters = WAITERS_BY_KEY.get(key);
          if (waiters != null && waiters.peekFirst() == waiter) {
            isFirstAny = true;
            break;
          }
        }

        if (isFirstAny) {
          byte[] response = responseSupplier.get();
          if (response != null) {
            for (String key : keys) {
              Deque<Condition> waiters = WAITERS_BY_KEY.get(key);
              if (waiters != null) {
                boolean wasFirst = (waiters.peekFirst() == waiter);
                waiters.remove(waiter);
                cleanupKeyIfIdle(key, waiters);
                if (wasFirst) {
                  signalNext(key);
                }
              }
            }
            return response;
          }
        }

        if (timeoutSeconds == 0) {
          waiter.await();
        } else {
          if (remainingNanos <= 0) {
            for (String key : keys) {
              Deque<Condition> waiters = WAITERS_BY_KEY.get(key);
              if (waiters != null) {
                boolean wasFirst = (waiters.peekFirst() == waiter);
                waiters.remove(waiter);
                cleanupKeyIfIdle(key, waiters);
                if (wasFirst) {
                  signalNext(key);
                }
              }
            }
            return RespResponse.nullArray();
          }

          remainingNanos = waiter.awaitNanos(remainingNanos);
        }
      }
    } finally {
      LOCK.unlock();
    }
  }

  /**
   * Signals that a key changed, allowing the oldest blocked command for that key to re-check
   * readiness.
   */
  public static void signalKeyChanged(String key) {
    LOCK.lock();
    try {
      redis.storage.KeyModificationTracker.notifyModified(key);
      signalNext(key);
    } finally {
      LOCK.unlock();
    }
  }

  /**
   * Test-only cleanup so waiter state from one unit test cannot leak into another.
   */
  static void resetForTests() {
    LOCK.lock();
    try {
      WAITERS_BY_KEY.clear();
    } finally {
      LOCK.unlock();
    }
  }

  private static void signalNext(String key) {
    Deque<Condition> waiters = WAITERS_BY_KEY.get(key);
    if (waiters != null && !waiters.isEmpty()) {
      waiters.peekFirst().signal();
    }
  }

  private static void cleanupKeyIfIdle(String key, Deque<Condition> waiters) {
    if (waiters.isEmpty()) {
      WAITERS_BY_KEY.remove(key);
    }
  }

  private static long timeoutToNanos(double timeoutSeconds) {
    if (timeoutSeconds == 0) {
      return 0;
    }

    double nanos = Math.ceil(timeoutSeconds * NANOS_PER_SECOND);
    if (nanos >= Long.MAX_VALUE) {
      return Long.MAX_VALUE;
    }

    return Math.max(1, (long) nanos);
  }
}
