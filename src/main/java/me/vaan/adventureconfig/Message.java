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

    public static void sendMessage(Audience sender, String key) {
        Config cs = Config.getInstance();
        Component prefix = cs.getComponent("messages.prefix");

        sender.sendMessage(prefix.append(cs.getComponent("messages." + key)));
    }

    public static void sendMessage(Audience player, String key, String... arg) {
        Config cs = Config.getInstance();
        Component prefix = cs.getComponent("messages.prefix");

        TagResolver[] resolvers = new TagResolver[arg.length + 1];
        resolvers[arg.length] = TagResolver.standard();
        for (int i = 0; i < arg.length; i++) {
            resolvers[i] = TagResolver.resolver("arg" + i, Tag.selfClosingInserting(Component.text(arg[i])));
        }

        TagResolver total = TagResolver.resolver(resolvers);
        MiniMessage mm = MiniMessage.builder().tags(total).build();

        Component componentMessage = mm.deserialize(cs.getString("messages." + key));
        player.sendMessage(prefix.append(componentMessage));
    }

    public static void sendTitle(Audience player, String key) {
        Config cs = Config.getInstance();
        if (!cs.getBool("titles.enabled")) {
            return;
        }

        MiniMessage mm = MiniMessage.miniMessage();

        List<Component> cmp = new ArrayList<>();
        for (String line : cs.getSList("messages." + key)) {
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

        player.showTitle(title);
    }
}
