package dev.minime;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PathfinderMob;

/** Регистрирует тип сущности «чибик». Если что-то пойдёт не так, мод продолжит работать на модели «player». */
public class MiniMeMain implements ModInitializer {
    public static EntityType<MiniChibiEntity> CHIBI = null;

    @Override
    public void onInitialize() {
        try {
            ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("minime", "chibi"));
            CHIBI = Registry.register(BuiltInRegistries.ENTITY_TYPE, key,
                    EntityType.Builder.<MiniChibiEntity>of(MiniChibiEntity::new, MobCategory.MISC).sized(0.5f, 1.4f).build(key));
            FabricDefaultAttributeRegistry.register(CHIBI, PathfinderMob.createMobAttributes());
        } catch (Throwable t) {
            CHIBI = null;
            t.printStackTrace();
        }
    }
}
