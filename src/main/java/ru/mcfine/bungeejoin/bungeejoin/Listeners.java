package ru.mcfine.bungeejoin.bungeejoin;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.awt.*;
import java.io.IOException;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class Listeners implements Listener {

    public static WeakHashMap<ProxiedPlayer, Boolean> permCache = new WeakHashMap<>();
    private static final WeakHashMap<ProxiedPlayer, Boolean> annCache = new WeakHashMap<>();

    private static boolean checkRecievePerm(ProxiedPlayer proxiedPlayer) {
        if (permCache.containsKey(proxiedPlayer)) return permCache.get(proxiedPlayer);
        boolean perm = proxiedPlayer.hasPermission("mcfine.join-message-recieve");
        permCache.put(proxiedPlayer, perm);
        return perm;
    }

    @EventHandler
    public void switchEvent(ServerSwitchEvent event) {
        ProxiedPlayer player = event.getPlayer();
        if (BungeeJoin.plugin.getProxy().getPlayers().size() > 40) return;
        if (event.getFrom() == null) {
            BungeeJoin.plugin.getProxy().getScheduler().schedule(BungeeJoin.plugin, new Runnable() {
                @Override
                public void run() {
                    if (BungeeJoin.plugin.getProxy().getPlayers().contains(player)) {
                        boolean firstJoin = false;
                        if (!BungeeJoin.names.containsKey(player.getName())) {
                            firstJoin = true;
                            BungeeJoin.names.put(player.getName(), System.currentTimeMillis());
                            BungeeJoin.dirty = true;
                        }
                        if (player.hasPermission("mcfine.join-message") || (firstJoin && player.hasPermission("mcfine.first-join-message"))) {
                            BaseComponent[] components;
                            if (!firstJoin) {
                                try {
                                    sendEmbed(1, player);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                                components = new ComponentBuilder("● ").color(ChatColor.DARK_GREEN)
                                        .append("Игрок ").color(ChatColor.GRAY)
                                        .append(player.getDisplayName()).color(ChatColor.GREEN)
                                        .append(" присоединился к серверу").color(ChatColor.GRAY).create();
                            } else {
                                try {
                                    sendEmbed(0, player);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                                components = new ComponentBuilder("● ").color(ChatColor.GOLD)
                                        .append("Игрок ").color(ChatColor.GRAY)
                                        .append(player.getDisplayName()).color(ChatColor.YELLOW)
                                        .append(" впервые на сервере!").color(ChatColor.GRAY).create();
                            }
                            for (ProxiedPlayer proxiedPlayer : BungeeJoin.plugin.getProxy().getPlayers()) {
                                if (!proxiedPlayer.equals(player) && checkRecievePerm(proxiedPlayer)) {
                                    proxiedPlayer.sendMessage(components);
                                }
                            }
                        }
                        annCache.put(player, true);
                    }
                }
            }, 3L, TimeUnit.SECONDS);
        }
    }

    @EventHandler
    public void leaveEvent(PlayerDisconnectEvent event) {
        if (BungeeJoin.plugin.getProxy().getPlayers().size() > 40) return;

        try {
            ProxiedPlayer player = event.getPlayer();
            if (!annCache.containsKey(player)) return;
            if (player.hasPermission("mcfine.join-message")) {
                try {
                    sendEmbed(2, player);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                BaseComponent[] components = new ComponentBuilder("● ").color(ChatColor.DARK_RED)
                        .append("Игрок ").color(ChatColor.GRAY)
                        .append(player.getDisplayName()).color(ChatColor.RED)
                        .append(" покинул сервер").color(ChatColor.GRAY).create();
                for (ProxiedPlayer proxiedPlayer : BungeeJoin.plugin.getProxy().getPlayers()) {
                    if (checkRecievePerm(proxiedPlayer)) {
                        proxiedPlayer.sendMessage(components);
                    }
                }
            }
            annCache.remove(player);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendEmbed(int type, ProxiedPlayer player) throws IOException {


        CompletableFuture.supplyAsync(() -> {
            String title = "";
            Color color = Color.GRAY;
            if (type == 0) {
                color = Color.ORANGE;
                title = "Игрок " + player.getDisplayName() + " впервые на сервере!";
            } else if (type == 1) {
                color = Color.GREEN;
                title = "Игрок " + player.getDisplayName() + " присоединился к серверу!";
            } else if (type == 2) {
                color = Color.RED;
                title = "Игрок " + player.getDisplayName() + " покинул сервер!";
            }
            DiscordWebhook webhook = new DiscordWebhook(BungeeJoin.configuration.getString("token"));
            webhook.addEmbed(new DiscordWebhook.EmbedObject()
                    .setColor(color)
                    .setAuthor(title, "https://rus-crafting.ru", "https://cravatar.eu/helmavatar/" + player.getName() + "/128.png"));
            try {
                webhook.execute(); //Handle exception
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return null;
        });


    }

}
