package dev.minime;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.util.Mth;

/**
 * Чибик: огромная голова, крошечное тельце. UV такая же, как у скина игрока (64x64),
 * поэтому подходят любые обычные скины. Для лиса те же части, но видны уши, морда и хвост.
 */
public class MiniChibiModel extends EntityModel<MiniChibiRenderState> {
    private final ModelPart head, rightArm, leftArm, rightLeg, leftLeg;
    private final ModelPart[] skinLayers;
    private final ModelPart[] foxParts;

    public MiniChibiModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        ModelPart body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.skinLayers = new ModelPart[]{
                head.getChild("hat"), body.getChild("jacket"),
                rightArm.getChild("right_sleeve"), leftArm.getChild("left_sleeve"),
                rightLeg.getChild("right_pants"), leftLeg.getChild("left_pants")};
        this.foxParts = new ModelPart[]{
                head.getChild("ear_r"), head.getChild("ear_l"), head.getChild("snout"), body.getChild("tail")};
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // голова 12x12x12 (из 8x8x8 с раздутием) + шапка-слой
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4f, -8f, -4f, 8f, 8f, 8f, new CubeDeformation(2.0f)),
                PartPose.offset(0f, 13f, 0f));
        head.addOrReplaceChild("hat",
                CubeListBuilder.create().texOffs(32, 0).addBox(-4f, -8f, -4f, 8f, 8f, 8f, new CubeDeformation(2.5f)),
                PartPose.ZERO);
        head.addOrReplaceChild("ear_r",
                CubeListBuilder.create().texOffs(32, 16).addBox(-1.5f, -3f, -0.5f, 3f, 3f, 1f),
                PartPose.offsetAndRotation(-3.5f, -9.5f, 0f, 0f, 0f, -0.2f));
        head.addOrReplaceChild("ear_l",
                CubeListBuilder.create().texOffs(32, 16).addBox(-1.5f, -3f, -0.5f, 3f, 3f, 1f),
                PartPose.offsetAndRotation(3.5f, -9.5f, 0f, 0f, 0f, 0.2f));
        head.addOrReplaceChild("snout",
                CubeListBuilder.create().texOffs(32, 0).addBox(-2f, -4f, -8f, 4f, 3f, 2f),
                PartPose.ZERO);

        // тельце 5x6x3
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16, 16).addBox(-4f, -3f, -2f, 8f, 12f, 4f, new CubeDeformation(-1.5f, -3f, -0.5f)),
                PartPose.offset(0f, 13f, 0f));
        body.addOrReplaceChild("jacket",
                CubeListBuilder.create().texOffs(16, 32).addBox(-4f, -3f, -2f, 8f, 12f, 4f, new CubeDeformation(-1.0f, -2.5f, 0.0f)),
                PartPose.ZERO);
        body.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(40, 32).addBox(-1.5f, -1.5f, 0f, 3f, 3f, 4f),
                PartPose.offsetAndRotation(0f, 4.5f, 1.5f, -0.9f, 0f, 0f));

        // ручки 2x5x2
        PartDefinition rArm = root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40, 16).addBox(-3f, -3f, -2f, 4f, 12f, 4f, new CubeDeformation(-1f, -3.5f, -1f)),
                PartPose.offset(-2.5f, 13f, 0f));
        rArm.addOrReplaceChild("right_sleeve",
                CubeListBuilder.create().texOffs(40, 32).addBox(-3f, -3f, -2f, 4f, 12f, 4f, new CubeDeformation(-0.5f, -3f, -0.5f)),
                PartPose.ZERO);
        PartDefinition lArm = root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(32, 48).addBox(-1f, -3f, -2f, 4f, 12f, 4f, new CubeDeformation(-1f, -3.5f, -1f)),
                PartPose.offset(2.5f, 13f, 0f));
        lArm.addOrReplaceChild("left_sleeve",
                CubeListBuilder.create().texOffs(48, 48).addBox(-1f, -3f, -2f, 4f, 12f, 4f, new CubeDeformation(-0.5f, -3f, -0.5f)),
                PartPose.ZERO);

        // ножки 3x5x3
        PartDefinition rLeg = root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 16).addBox(-2f, -3.5f, -2f, 4f, 12f, 4f, new CubeDeformation(-0.5f, -3.5f, -0.5f)),
                PartPose.offset(-1.5f, 19f, 0f));
        rLeg.addOrReplaceChild("right_pants",
                CubeListBuilder.create().texOffs(0, 32).addBox(-2f, -3.5f, -2f, 4f, 12f, 4f, new CubeDeformation(0f, -3f, 0f)),
                PartPose.ZERO);
        PartDefinition lLeg = root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(16, 48).addBox(-2f, -3.5f, -2f, 4f, 12f, 4f, new CubeDeformation(-0.5f, -3.5f, -0.5f)),
                PartPose.offset(1.5f, 19f, 0f));
        lLeg.addOrReplaceChild("left_pants",
                CubeListBuilder.create().texOffs(0, 48).addBox(-2f, -3.5f, -2f, 4f, 12f, 4f, new CubeDeformation(0f, -3f, 0f)),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(MiniChibiRenderState state) {
        super.setupAnim(state);
        for (ModelPart p : skinLayers) p.visible = !state.fox;
        for (ModelPart p : foxParts) p.visible = state.fox;

        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;

        float t = state.ageInTicks;
        float amp = state.walkAnimationSpeed;
        float pos = state.walkAnimationPos;
        this.rightLeg.xRot = Mth.cos(pos * 0.6662f) * 1.0f * amp + Mth.sin(t * 0.12f) * 0.12f;
        this.leftLeg.xRot = Mth.cos(pos * 0.6662f + Mth.PI) * 1.0f * amp - Mth.sin(t * 0.12f) * 0.12f;
        this.rightArm.xRot = Mth.cos(pos * 0.6662f + Mth.PI) * 1.0f * amp;
        this.leftArm.xRot = Mth.cos(pos * 0.6662f) * 1.0f * amp;
        // ручки «порхают»
        this.rightArm.zRot = 0.35f + Mth.sin(t * 0.25f) * 0.12f;
        this.leftArm.zRot = -0.35f - Mth.sin(t * 0.25f) * 0.12f;
    }
}
