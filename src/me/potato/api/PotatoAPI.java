package me.potato.api;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketPlayOutChat;
import net.minecraft.server.v1_8_R3.PacketPlayOutTitle;
import net.minecraft.server.v1_8_R3.PlayerConnection;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
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

    public void clear() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            for(int i=0; i < 100; i++) {
                player.sendMessage("");
            }
        }
    }

    // Action bar

    public void sendActionBar(Player p, String message) {
        IChatBaseComponent cbc = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + message + "\"}");
        PacketPlayOutChat ppac = new PacketPlayOutChat(cbc, (byte) 2);
        ((CraftPlayer) p).getHandle().playerConnection.sendPacket(ppac);
    }

    // Title

    public void sendTitle(Player p, String title, String subTitle, int fadeIn, int stay, int fadeOut) {
        CraftPlayer craftPlayer = (CraftPlayer) p;
        PlayerConnection connection = craftPlayer.getHandle().playerConnection;
        IChatBaseComponent titleJSON = IChatBaseComponent.ChatSerializer.a("{'text':  '" + title + "'}");
        IChatBaseComponent subtitleJSON = IChatBaseComponent.ChatSerializer.a("{'text':  '" + subTitle + "'}");
        PacketPlayOutTitle titlePacket = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.TITLE, titleJSON, fadeIn, stay, fadeOut);
        PacketPlayOutTitle subtitlePacket = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.SUBTITLE, subtitleJSON, fadeIn, stay, fadeOut);
        connection.sendPacket(titlePacket);
        connection.sendPacket(subtitlePacket);
    }

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
