package com.savelyich.trials.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.savelyich.trials.TrialsMod;
import com.savelyich.trials.entity.Bosses1;
import com.savelyich.trials.registry.ModEntities;
import com.savelyich.trials.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Рендереры всех существ и свойства моделей предметов (натяжение лука, заряд арбалета, блок щита). */
@Mod.EventBusSubscriber(modid = TrialsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BossRenderers {
    private BossRenderers() {}

    /** Гуманоидное существо с собственной текстурой, масштабом и (опционально) полупрозрачностью. */
    public static class Humanoid<T extends Mob> extends HumanoidMobRenderer<T, HumanoidModel<T>> {
        private final ResourceLocation tex;
        private final float scale;
        private final boolean translucent;

        public Humanoid(EntityRendererProvider.Context ctx, String id, float scale, float shadow, boolean translucent) {
            super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), shadow);
            this.tex = new ResourceLocation(TrialsMod.MODID, "textures/entity/" + id + ".png");
            this.scale = scale;
            this.translucent = translucent;
        }

        @Override public ResourceLocation getTextureLocation(T e) { return tex; }

        @Override
        protected void scale(T e, PoseStack ps, float pt) { ps.scale(scale, scale, scale); }

        @Override
        protected RenderType getRenderType(T e, boolean visible, boolean visibleToPlayer, boolean glowing) {
            if (translucent && visible) return RenderType.entityTranslucent(tex);
            return super.getRenderType(e, visible, visibleToPlayer, glowing);
        }
    }

    /** «Овца» Каменного Пастуха — вращающийся булыжник. */
    public static class HerdStoneRenderer extends EntityRenderer<Bosses1.HerdStone> {
        public HerdStoneRenderer(EntityRendererProvider.Context ctx) { super(ctx); this.shadowRadius = 0.3f; }

        @Override
        public void render(Bosses1.HerdStone e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
            ps.pushPose();
            ps.translate(0, 0.45, 0);
            ps.mulPose(Axis.YP.rotationDegrees((e.tickCount + pt) * 4f));
            ps.mulPose(Axis.XP.rotationDegrees((e.tickCount + pt) * 2f));
            ps.scale(0.85f, 0.85f, 0.85f);
            ps.translate(-0.5, -0.5, -0.5);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.COBBLESTONE.defaultBlockState(), ps, buf, light, OverlayTexture.NO_OVERLAY);
            ps.popPose();
            super.render(e, yaw, pt, ps, buf, light);
        }

        @Override public ResourceLocation getTextureLocation(Bosses1.HerdStone e) { return InventoryMenu.BLOCK_ATLAS; }
    }

    private static <T extends Mob> void h(EntityRenderersEvent.RegisterRenderers e, EntityType<? extends T> type, String id,
                                          float scale, float shadow, boolean translucent) {
        e.registerEntityRenderer(type, ctx -> new Humanoid<T>(ctx, id, scale, shadow, translucent));
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        h(e, ModEntities.ECHO_KEEPER.get(), "echo_keeper", 1.05f, 0.5f, false);
        h(e, ModEntities.WAILER.get(), "wailer", 1.1f, 0.4f, true);
        h(e, ModEntities.STONE_SHEPHERD.get(), "stone_shepherd", 1.5f, 0.9f, false);
        h(e, ModEntities.FIREFLY_SWARM.get(), "firefly_swarm", 1.2f, 0.2f, true);
        h(e, ModEntities.MUSHROOM_DOUBLE.get(), "mushroom_double", 1.0f, 0.5f, false);
        h(e, ModEntities.SAND_SWIMMER.get(), "sand_swimmer", 1.0f, 0.5f, false);
        h(e, ModEntities.ICE_PILGRIM.get(), "ice_pilgrim", 1.1f, 0.5f, false);
        h(e, ModEntities.RUSTY_MECHANIC.get(), "rusty_mechanic", 1.25f, 0.7f, false);
        h(e, ModEntities.DREAMER.get(), "dreamer", 1.0f, 0.6f, true);       // у настоящего есть тень
        h(e, ModEntities.DREAMER_CLONE.get(), "dreamer", 1.0f, 0.0f, true); // у копий — нет
        h(e, ModEntities.ROOT_WANDERER.get(), "root_wanderer", 1.4f, 0.8f, false);
        h(e, ModEntities.GLASS_WATCHER.get(), "glass_watcher", 0.9f, 0.0f, true);
        h(e, ModEntities.BOG_WHISPER.get(), "bog_whisper", 1.05f, 0.2f, true);
        h(e, ModEntities.GHOST_SMITH.get(), "ghost_smith", 1.1f, 0.3f, true);
        h(e, ModEntities.LIGHT_DEVOURER.get(), "light_devourer", 1.3f, 0.5f, false);
        h(e, ModEntities.MOON_DOUBLE.get(), "moon_double", 1.0f, 0.5f, true);
        h(e, ModEntities.SAVELYICH.get(), "savelyich", 1.0f, 0.5f, false);
        e.registerEntityRenderer(ModEntities.HERD_STONE.get(), HerdStoneRenderer::new);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent e) {
        e.enqueueWork(() -> {
            ResourceLocation pull = new ResourceLocation("pull"), pulling = new ResourceLocation("pulling");
            Item bow = ModItems.GHOST_BOW.get();
            ItemProperties.register(bow, pull, (s, l, en, seed) ->
                    en == null || en.getUseItem() != s ? 0.0f : (s.getUseDuration() - en.getUseItemRemainingTicks()) / 20.0f);
            ItemProperties.register(bow, pulling, (s, l, en, seed) ->
                    en != null && en.isUsingItem() && en.getUseItem() == s ? 1.0f : 0.0f);

            Item xb = ModItems.SPRING_CROSSBOW.get();
            ItemProperties.register(xb, pull, (s, l, en, seed) -> en == null || CrossbowItem.isCharged(s) ? 0.0f
                    : (float) (s.getUseDuration() - en.getUseItemRemainingTicks()) / (float) CrossbowItem.getChargeDuration(s));
            ItemProperties.register(xb, pulling, (s, l, en, seed) ->
                    en != null && en.isUsingItem() && en.getUseItem() == s && !CrossbowItem.isCharged(s) ? 1.0f : 0.0f);
            ItemProperties.register(xb, new ResourceLocation("charged"), (s, l, en, seed) -> CrossbowItem.isCharged(s) ? 1.0f : 0.0f);
            ItemProperties.register(xb, new ResourceLocation("firework"), (s, l, en, seed) ->
                    CrossbowItem.isCharged(s) && CrossbowItem.containsChargedProjectile(s, Items.FIREWORK_ROCKET) ? 1.0f : 0.0f);

            ItemProperties.register(ModItems.ECHO_TRIDENT.get(), new ResourceLocation("throwing"), (s, l, en, seed) ->
                    en != null && en.isUsingItem() && en.getUseItem() == s ? 1.0f : 0.0f);
            ItemProperties.register(ModItems.KINETIC_BLADE_SHIELD.get(), new ResourceLocation("blocking"), (s, l, en, seed) ->
                    en != null && en.isUsingItem() && en.getUseItem() == s ? 1.0f : 0.0f);
        });
    }
}
