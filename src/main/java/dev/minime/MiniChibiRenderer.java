package dev.minime;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class MiniChibiRenderer extends MobRenderer<MiniChibiEntity, MiniChibiRenderState, MiniChibiModel> {
    private static final Identifier FOX = Identifier.fromNamespaceAndPath("minime", "textures/entity/fox.png");
    private static final Identifier STEVE = Identifier.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png");

    public MiniChibiRenderer(EntityRendererProvider.Context context) {
        super(context, new MiniChibiModel(MiniChibiModel.createLayer().bakeRoot()), 0.15f);
    }

    @Override
    public MiniChibiRenderState createRenderState() { return new MiniChibiRenderState(); }

    @Override
    public void extractRenderState(MiniChibiEntity entity, MiniChibiRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.fox = entity.fox;
        state.texture = entity.texture;
    }

    @Override
    public Identifier getTextureLocation(MiniChibiRenderState state) {
        if (state.fox) return FOX;
        return state.texture != null ? state.texture : STEVE;
    }
}
