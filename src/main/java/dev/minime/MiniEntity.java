package dev.minime;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.entity.player.PlayerSkin;

/** Мини-человечек: клиентская сущность-игрок, которую рисует сам Minecraft. */
public class MiniEntity extends RemotePlayer {
    public MiniEntity(ClientLevel level, GameProfile profile) { super(level, profile); }

    @Override
    public PlayerSkin getSkin() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return super.getSkin();
        return SkinManager.skinFor(mc, MiniMeClient.config.skin);
    }

    @Override public boolean isPushable() { return false; }

    // показываем второй слой скина (шапка, куртка, рукава, штаны)
    @Override public boolean isModelPartShown(PlayerModelPart part) { return true; }

    // --- без ника над головой ---
    @Override public boolean isVehicle() { return true; }                // рендер не рисует ник у "транспорта"
    @Override public Component getDisplayName() { return Component.empty(); }
}
