package com.loohp.interactivechat.platform.packetevents;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.protocol.chat.ChatTypes;
import com.github.retrooper.packetevents.protocol.chat.message.ChatMessage;
import com.github.retrooper.packetevents.protocol.chat.message.ChatMessageLegacy;
import com.github.retrooper.packetevents.protocol.chat.message.ChatMessage_v1_16;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChatMessage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSystemChatMessage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTabComplete;
import com.loohp.interactivechat.InteractiveChat;
import com.loohp.interactivechat.bungeemessaging.packet.packetevents.PEServerPingListener;
import com.loohp.interactivechat.listeners.packet.packetevents.PEClientSettingsPacket;
import com.loohp.interactivechat.listeners.packet.packetevents.PEOutMessagePacket;
import com.loohp.interactivechat.listeners.packet.packetevents.PEOutTabCompletePacket;
import com.loohp.interactivechat.listeners.packet.packetevents.PERedispatchSignedPacket;
import com.loohp.interactivechat.objectholders.CustomTabCompletionAction;
import com.loohp.interactivechat.platform.ProtocolPlatform;
import com.loohp.interactivechat.utils.InteractiveChatComponentSerializer;
import com.loohp.interactivechat.utils.MCVersion;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.loohp.interactivechat.InteractiveChat.version;

public class PacketEventsPlatform implements ProtocolPlatform {

    @Override
    public void initialise() {
        PacketEvents.getAPI().getEventManager().registerListener(new PEOutMessagePacket(), PacketListenerPriority.MONITOR);
        PacketEvents.getAPI().getEventManager().registerListener(new PEClientSettingsPacket(), PacketListenerPriority.NORMAL);

        if (version.isNewerOrEqualTo(MCVersion.V1_19)) {
            PacketEvents.getAPI().getEventManager().registerListener(new PERedispatchSignedPacket(), PacketListenerPriority.HIGHEST);
        }

        if (!version.isLegacy()) {
            PacketEvents.getAPI().getEventManager().registerListener(new PEOutTabCompletePacket(), PacketListenerPriority.HIGH);
        }
    }

    @Override
    public void onBungeecordEnabled() {
        PacketEvents.getAPI().getEventManager().registerListener(new PEServerPingListener(), PacketListenerPriority.NORMAL);
    }

    @Override
    public void sendTabCompletionPacket(Player player, CustomTabCompletionAction action, List<String> list) {
        List<WrapperPlayServerTabComplete.CommandMatch> suggestions = new ArrayList<>();
        for (String cmd : list) {
            suggestions.add(new WrapperPlayServerTabComplete.CommandMatch(cmd, null));
        }

        WrapperPlayServerTabComplete packet = new WrapperPlayServerTabComplete(
                null,
                new WrapperPlayServerTabComplete.CommandRange(0, list.size()),
                suggestions
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }

    @Override
    public void sendUnprocessedChatMessage(CommandSender sender, UUID uuid, Component component) {
        if (sender instanceof Player) {
            PacketWrapper<?> packet;
            if (InteractiveChat.version.isNewerOrEqualTo(MCVersion.V1_19)) {
                packet = new WrapperPlayServerSystemChatMessage(false, component);
            } else {
                ChatMessage message;
                if (InteractiveChat.version.isNewerOrEqualTo(MCVersion.V1_16)) {
                    message = new ChatMessage_v1_16(component, ChatTypes.SYSTEM, uuid);
                } else {
                    message = new ChatMessageLegacy(component, ChatTypes.SYSTEM);
                }

                packet = new WrapperPlayServerChatMessage(message);
            }

            PacketEvents.getAPI().getPlayerManager().sendPacket(sender, packet);
        } else {
            String json = InteractiveChatComponentSerializer.gson().serialize(component);
            sender.spigot().sendMessage(ComponentSerializer.parse(json));
        }
    }

    @Override
    public boolean hasChatSigning() {
        return InteractiveChat.version.isNewerOrEqualTo(MCVersion.V1_19);
    }

    @Override
    public int getProtocolVersion(Player player) {
        return PacketEvents.getAPI().getProtocolManager().getClientVersion(player).getProtocolVersion();
    }
}
