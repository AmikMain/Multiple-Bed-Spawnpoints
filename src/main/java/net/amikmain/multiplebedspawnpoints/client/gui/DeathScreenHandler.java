package net.amikmain.multiplebedspawnpoints.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "multiplebedspawnpoints", value = Dist.CLIENT)
public class DeathScreenHandler {

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof DeathScreen deathScreen)) return;

        // Локализованный текст для кнопки Respawn (на текущем языке клиента)
        String respawnLocalized = Component.translatable("deathScreen.respawn").getString();

        for (GuiEventListener widget : event.getListenersList()) {
            if (!(widget instanceof Button button)) continue;

            // Сравниваем локализованные строки — надёжно для любых языков
            if (button.getMessage().getString().equals(respawnLocalized)) {
                // Удаляем стандартную кнопку
                event.removeListener(button);

                // Создаём точно такую же кнопку, но с нашим действием
                Button newButton = Button.builder(
                        Component.translatable("deathScreen.respawn"),
                        b -> Minecraft.getInstance().setScreen(new DeathSpawnScreen())
                ).bounds(button.getX(), button.getY(), button.getWidth(), button.getHeight()).build();

                event.addListener(newButton);
                // Как правило нужна только одна кнопка Respawn — можно выйти из цикла
                break;
            }
        }
    }
}