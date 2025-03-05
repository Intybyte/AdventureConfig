package me.vaan.adventureconfig;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.title.Title;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class Message {
    private static final Duration TICK = Duration.ofMillis(50);
    private final String keyPrefix;
    private final Component beforeMessage;

    public Message() {
        this.keyPrefix = "";
        this.beforeMessage = Component.empty();
    }

    public Message(String keyPrefix) {
        this.keyPrefix = keyPrefix + ".";
        this.beforeMessage = Component.empty();
    }

    public Message(String keyPrefix, Component beforeMessage) {
        this.keyPrefix = keyPrefix + ".";
        this.beforeMessage = beforeMessage;
    }

    /**
     * Send a simple message to the user without any arguments to process.
     *
     * @param audience The target that must see the message
     * @param key Combined with the prefix key to obtain data from Config
     */
    public void sendMessage(Audience audience, String key) {
        Config cs = Config.getInstance();
        audience.sendMessage(beforeMessage.append(cs.getComponent(keyPrefix + key)));
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
        Config cs = Config.getInstance();

        TagResolver[] resolvers = new TagResolver[arg.length + 1];
        resolvers[arg.length] = TagResolver.standard();
        for (int i = 0; i < arg.length; i++) {
            resolvers[i] = TagResolver.resolver("arg" + i, Tag.selfClosingInserting(Component.text(arg[i])));
        }

        TagResolver total = TagResolver.resolver(resolvers);
        MiniMessage mm = MiniMessage.builder().tags(total).build();

        Component componentMessage = mm.deserialize(cs.getString(keyPrefix + key));
        audience.sendMessage(beforeMessage.append(componentMessage));
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
        Config cs = Config.getInstance();
        MiniMessage mm = MiniMessage.miniMessage();

        List<Component> cmp = new ArrayList<>();
        for (String line : cs.getSList(keyPrefix + key)) {
            cmp.add(mm.deserialize(line));
        }

        if (cmp.isEmpty()) {
            return;
        }

        int fadeIn = cs.getInt("titles.fade_in");
        int stayDuration = cs.getInt("titles.stay_duration");
        int fadeOut = cs.getInt("titles.fade_out");

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
