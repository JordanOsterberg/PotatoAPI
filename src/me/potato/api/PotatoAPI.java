package me.potato.api;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.Socket;

public class PotatoAPI extends JavaPlugin {

    private static PotatoAPI instance;
    public static PotatoAPI getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
    }

    public void connectServer(Player player, String server) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(server);
        player.sendPluginMessage(this, "BungeeCord", out.toByteArray());
    }

    public ItemStack createItem(Material material, String displayName) {
        ItemStack stack = new ItemStack(material);
        ItemMeta stackMeta = stack.getItemMeta();
        stackMeta.setDisplayName(displayName);
        return stack;
    }

    public String format(String message) {
        ChatColor.translateAlternateColorCodes('&', message);
        return message;
    }

    // Clear chat

    public void clearChat() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            for(int i=0; i < 100; i++) {
                player.sendMessage("");
            }
        }
    }

    // Action bar

    public void sendActionBar(Player p, String message) {
        message = format(message);

        try {
            Object iChatBaseComponent = getNMSClass("IChatBaseComponent").getDeclaredClasses()[0].getMethod("a", String.class).invoke(null, "{\"text\": \"" + message + "\"}");
            Constructor<?> packetConstructor = getNMSClass("PacketPlayOutChat").getConstructor(getNMSClass("IChatBaseComponent"), byte.class);
            Object packet = packetConstructor.newInstance(iChatBaseComponent, (byte) 2);
            sendPacket(p, packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Title
    public void sendTitle(Player p, String title, String subTitle, int fadeIn, int stay, int fadeOut) {
        title = format(title);
        subTitle = format(subTitle);

        try {
            Object enumTitle = getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0].getField("TITLE").get(null);
            Object enumSubTitle = getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0].getField("SUBTITLE").get(null);

            Object titleChatComponent = getNMSClass("IChatBaseComponent").getDeclaredClasses()[0].getMethod("a", String.class).invoke(null, "{\"text\":\""  + title + "\"}");
            Object subTitleChatComponent = getNMSClass("IChatBaseComponent").getDeclaredClasses()[0].getMethod("a", String.class).invoke(null, "{\"text\":\""  + subTitle + "\"}");

            Constructor<?> titleConstructor = getNMSClass("PacketPlayOutTitle").getConstructor(getNMSClass("PacketPlayOutTitle").getDeclaredClasses()[0], getNMSClass("IChatBaseComponent"), int.class, int.class, int.class);

            Object titlePacket = titleConstructor.newInstance(enumTitle, titleChatComponent, fadeIn, stay, fadeOut);
            sendPacket(p, titlePacket);

            Object subTitlePacket = titleConstructor.newInstance(enumSubTitle, subTitleChatComponent, fadeIn, stay, fadeOut);
            sendPacket(p, subTitlePacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // NMS send packet
    private void sendPacket(Player player, Object packet) {
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object playerConnection = handle.getClass().getField("playerConnection").get(handle);
            playerConnection.getClass().getMethod("sendPacket", getNMSClass("Packet")).invoke(playerConnection, packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // NMS class fetcher
    public Class<?> getNMSClass(String name) {
        String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        try {
            return Class.forName("net.minecraft.server." + version + "." + name);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Socket MOTD fetching
    public String getMOTD(String ip, int port) {
        try (Socket sock = new Socket(ip, port);
             DataOutputStream out = new DataOutputStream(sock.getOutputStream());
             DataInputStream in = new DataInputStream(sock.getInputStream())) {

            out.write(0xFE);
            int b;
            StringBuffer str = new StringBuffer();
            while ((b = in.read()) != -1) {
                if (b != 0 && b > 16 && b != 255 && b != 23 && b != 24) str.append((char) b);
            }

            return str.toString().split("§")[0];
        } catch (IOException ex) {
            return null;
        }
    }
}
