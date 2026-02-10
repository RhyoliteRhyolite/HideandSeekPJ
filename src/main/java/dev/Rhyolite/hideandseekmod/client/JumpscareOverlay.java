package dev.Rhyolite.hideandseekmod.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.Rhyolite.hideandseekmod.HideandSeekMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

// 클라이언트 사이드에서만 동작하도록 설정
@EventBusSubscriber(modid = HideandSeekMod.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class JumpscareOverlay {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(HideandSeekMod.MODID, "textures/gui/jumpscare.png");
    private static int displayTicks = 0;

    // 점프스캐어 시작 메서드
    public static void trigger() {
        displayTicks = 40; // 2초 (20틱 = 1초)
    }

    // 매 프레임마다 화면을 그릴 때 호출됨
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (displayTicks > 0) {
            displayTicks--;

            GuiGraphics guiGraphics = event.getGuiGraphics();
            int width = guiGraphics.guiWidth();
            int height = guiGraphics.guiHeight();

            // 렌더링 설정 (기존과 동일)
            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            // 화면 전체에 이미지 그리기
            guiGraphics.blit(TEXTURE, 0, 0, 0, 0, width, height, width, height);

            RenderSystem.disableBlend();
            RenderSystem.enableDepthTest();
        }
    }
}