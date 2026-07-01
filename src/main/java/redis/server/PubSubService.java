package redis.server;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PubSubService {
    public interface Subscriber {
        void sendMessage(String channel, byte[] message);
    }

    private final Map<String, Set<String>> channelSubscribers = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> clientSubscriptions = new ConcurrentHashMap<>();
    private final Map<String, Subscriber> clientSubscribers = new ConcurrentHashMap<>();

    public void subscribe(String clientId, String channel, Subscriber subscriber) {
        clientSubscribers.putIfAbsent(clientId, subscriber);
        channelSubscribers.computeIfAbsent(channel, k -> ConcurrentHashMap.newKeySet()).add(clientId);
        clientSubscriptions.computeIfAbsent(clientId, k -> ConcurrentHashMap.newKeySet()).add(channel);
    }

    public void unsubscribe(String clientId, String channel) {
        Set<String> subscribers = channelSubscribers.get(channel);
        if (subscribers != null) {
            subscribers.remove(clientId);
        }
        Set<String> subscriptions = clientSubscriptions.get(clientId);
        if (subscriptions != null) {
            subscriptions.remove(channel);
        }
    }

    public void unsubscribeAll(String clientId) {
        clientSubscribers.remove(clientId);
        Set<String> subscriptions = clientSubscriptions.remove(clientId);
        if (subscriptions != null) {
            for (String channel : subscriptions) {
                Set<String> subscribers = channelSubscribers.get(channel);
                if (subscribers != null) {
                    subscribers.remove(clientId);
                }
            }
        }
    }

    public int getSubscriberCount(String channel) {
        Set<String> subscribers = channelSubscribers.get(channel);
        return subscribers == null ? 0 : subscribers.size();
    }

    public int publish(String channel, byte[] message) {
        Set<String> clientIds = channelSubscribers.get(channel);
        if (clientIds == null || clientIds.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (String clientId : clientIds) {
            Subscriber subscriber = clientSubscribers.get(clientId);
            if (subscriber != null) {
                subscriber.sendMessage(channel, message);
                count++;
            }
        }
        return count;
    }
}
