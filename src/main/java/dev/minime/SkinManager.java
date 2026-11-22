package dev.minime;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Скины для мини: свой, steve, alex, встроенный женский или PNG из config/minime/skins. */
public class SkinManager {
    private static final Map<String, Identifier> FILES = new HashMap<>();
    private static final Map<String, PlayerSkin> SKINS = new HashMap<>();

    public static PlayerSkin skinFor(Minecraft mc, String skinName) {
        PlayerSkin own = mc.player.getSkin();
        if (skinName == null || skinName.equals("own")) return own;
        switch (skinName) {
            case "girl" -> { return SKINS.computeIfAbsent("girl", k -> make(
                    Identifier.fromNamespaceAndPath("minime", "skin/girl"),
                    Identifier.fromNamespaceAndPath("minime", "textures/skin/girl.png"), PlayerModelType.WIDE)); }
            case "steve" -> { return SKINS.computeIfAbsent("steve", k -> make(
                    Identifier.fromNamespaceAndPath("minecraft", "entity/player/wide/steve"),
                    Identifier.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png"), PlayerModelType.WIDE)); }
            case "alex" -> { return SKINS.computeIfAbsent("alex", k -> make(
                    Identifier.fromNamespaceAndPath("minecraft", "entity/player/slim/alex"),
                    Identifier.fromNamespaceAndPath("minecraft", "textures/entity/player/slim/alex.png"), PlayerModelType.SLIM)); }
            default -> { }
        }
        Identifier id = FILES.computeIfAbsent(skinName, SkinManager::load);
        if (id != null) return SKINS.computeIfAbsent("f:" + skinName, k -> make(id, id, own.model()));
        return own;
    }

    /** ВНИМАНИЕ: конструктор PlayerSkin / ClientAsset — самое вероятное место правок под новые версии. */
    private static PlayerSkin make(Identifier id, Identifier texturePath, PlayerModelType type) {
        return new PlayerSkin(new ClientAsset.ResourceTexture(id, texturePath), null, null, type, true);
    }

    public static void clear() {
        var tm = Minecraft.getInstance().getTextureManager();
        FILES.values().forEach(id -> { if (id != null) tm.release(id); });
        FILES.clear();
        SKINS.clear();
    }

    private static Identifier load(String name) {
        Path f = Config.DIR.resolve("skins").resolve(name + ".png");
        if (!Files.exists(f)) return null;
        try (InputStream in = Files.newInputStream(f)) {
            NativeImage img = NativeImage.read(in);
            Identifier id = Identifier.fromNamespaceAndPath("minime", "skin/" + name.toLowerCase().replaceAll("[^a-z0-9_./-]", "_"));
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "minime_skin_" + name, img));
            return id;
        } catch (Exception e) { e.printStackTrace(); return null; }
    }
}
