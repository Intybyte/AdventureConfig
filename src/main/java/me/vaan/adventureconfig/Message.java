package me.vaan.adventureconfig;

import lombok.Getter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.title.Title;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Getter
public class Message {
    private static final Duration TICK = Duration.ofMillis(50);
    private final String keyPrefix;
    private final Component beforeMessage;
    private final Config config;

    public Message(Config config) {
        this.config = config;
        this.keyPrefix = "";
        this.beforeMessage = Component.empty();
    }

    public Message(Config config, String keyPrefix) {
        this.config = config;
        this.keyPrefix = keyPrefix + ".";
        this.beforeMessage = Component.empty();
    }

    public Message(Config config, String keyPrefix, Component beforeMessage) {
        this.config = config;
        this.keyPrefix = keyPrefix + ".";
        this.beforeMessage = beforeMessage;
    }

    public List<Component> getRaw(String key, MiniMessage mm) {
        String finalKey = keyPrefix + key;
        Object message = config.get(finalKey);
        if (message == null) {
            throw new RuntimeException("Key not found: " + finalKey);
        }

        if (message instanceof String) {
            String messageString = (String) message;
            List<Component> result = new ArrayList<>(1);
            result.add(beforeMessage.append(
                    mm.deserialize(messageString)
            ));
            return result;
        } else if (message instanceof List<?>) {
            List<?> messages = (List<?>) message;

            if (messages.isEmpty()) {
                throw new RuntimeException("Key format not valid for message: " + finalKey);
            }

            if (!(messages.get(0) instanceof String)) {
                throw new RuntimeException("Key format not valid for message: " + finalKey);
            }

            List<String> messageList = (List<String>) messages;
            List<Component> allMessage = new LinkedList<>();
            for (String entry : messageList) {
                allMessage.add(
                        beforeMessage.append(
                                mm.deserialize(entry)
                        )
                );
            }
            return allMessage;
        }

        throw new RuntimeException("Key format not valid for message: " + finalKey);
    }

    public List<Component> getRaw(String key) {
        return getRaw(key, MiniMessage.miniMessage());
    }

    public List<Component> getRaw(String key, String... arg) {
        TagResolver[] resolvers = new TagResolver[arg.length + 1];
        resolvers[arg.length] = TagResolver.standard();
        for (int i = 0; i < arg.length; i++) {
            resolvers[i] = TagResolver.resolver("arg" + i, Tag.selfClosingInserting(Component.text(arg[i])));
        }

        TagResolver total = TagResolver.resolver(resolvers);
        MiniMessage mm = MiniMessage.builder().tags(total).build();

        return getRaw(key, mm);
    }

    public List<Component> getRaw(String key, Map<String, Component> mapper) {
        ArrayList<TagResolver> resolvers = new ArrayList<>(mapper.size() + 1);
        resolvers.add(TagResolver.standard());
        for (Map.Entry<String, Component> entry : mapper.entrySet()) {
            resolvers.add( TagResolver.resolver(entry.getKey(), Tag.selfClosingInserting(entry.getValue())) );
        }

        TagResolver total = TagResolver.resolver(resolvers);
        MiniMessage mm = MiniMessage.builder().tags(total).build();

        return getRaw(key, mm);
    }

    /**
     * Send a simple message to the user without any arguments to process.
     *
     * @param audience The target that must see the message
     * @param key Combined with the prefix key to obtain data from Config
     */
    public void sendMessage(Audience audience, String key) {
        List<Component> componentMessage = getRaw(key);
        componentMessage.stream().map(beforeMessage::append).forEach(audience::sendMessage);
    }

    /**
     * This method processes the args given based on the position:
     * The first arg will replace any occurrences of &lt;arg1&gt;
     * The second arg will replace any occurrences of &lt;arg2&gt;
     * And so forth depending on the number of arguments passed.
     *
     * @param audience The target that must see the message
     * @param key Combined with the prefix key to obtain data from Config
     * @param arg List of arguments to process on the tag
     */
    public void sendMessage(Audience audience, String key, String... arg) {
        List<Component> componentMessage = getRaw(key, arg);
        componentMessage.stream().map(beforeMessage::append).forEach(audience::sendMessage);
    }


    public void sendMessage(Audience audience, String key, Map<String, Component> mapper) {
        List<Component> componentMessage = getRaw(key, mapper);
        componentMessage.stream().map(beforeMessage::append).forEach(audience::sendMessage);
    }

    /**
     * This method doesn't use beforeMessage defined before.
     * In the config file these tags must be defined in order to use in this method:
     * <br>
     * titles.fade_in
     * titles.stay_duration
     * titles.fade_out
     *
     * @param audience The target that must see the title
     * @param key Combined with the prefix key to obtain data from Config
     */
    public void sendTitle(Audience audience, String key) {
        MiniMessage mm = MiniMessage.miniMessage();

        List<Component> cmp = new ArrayList<>();
        for (String line : config.getSList(keyPrefix + key)) {
            cmp.add(mm.deserialize(line));
        }

        if (cmp.isEmpty()) {
            return;
        }

        int fadeIn = config.getInt("titles.fade_in");
        int stayDuration = config.getInt("titles.stay_duration");
        int fadeOut = config.getInt("titles.fade_out");

        Title.Times times = Title.Times.times(TICK.multipliedBy(fadeIn), TICK.multipliedBy(stayDuration), TICK.multipliedBy(fadeOut));
        Title title;
        if (cmp.size() == 1) {
            title = Title.title(cmp.get(0), Component.empty(), times);
        } else {
            title = Title.title(cmp.get(0), cmp.get(1), times);
        }

        audience.showTitle(title);
    }
}
