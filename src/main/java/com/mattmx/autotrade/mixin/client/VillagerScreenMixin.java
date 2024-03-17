package com.mattmx.autotrade.mixin.client;

import com.mattmx.autotrade.InventoryHelper;
import com.mattmx.autotrade.client.AutoTradeClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.packet.c2s.play.SelectMerchantTradeC2SPacket;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.village.Merchant;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import java.util.Optional;
import java.util.UUID;

@Mixin(MerchantScreen.class)
public abstract class VillagerScreenMixin extends HandledScreen<MerchantScreenHandler> {
    private final MinecraftClient client = MinecraftClient.getInstance();
    @Shadow
    private int selectedIndex;

    public VillagerScreenMixin(MerchantScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(
            method = "render",
            at = @At("TAIL")
    )
    private void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        context.drawText(client.textRenderer,
                MutableText.of(Text.of("Press ").getContent())
                        .append(Text.translatable(AutoTradeClient.INSTANCE.getQuickTradeKey().getBinding().getBoundKeyTranslationKey()))
                        .append(" and click an offer to quick trade."),
                4, height - client.textRenderer.fontHeight * 2 - 4, Color.WHITE.getRGB(), false
        );
        context.drawText(client.textRenderer,
                MutableText.of(Text.of("Press ").getContent())
                        .append(Text.translatable(AutoTradeClient.INSTANCE.getQuickTradeKey().getBinding().getBoundKeyTranslationKey()))
                        .append(" + ")
                        .append(Text.translatable(AutoTradeClient.INSTANCE.getBindAutoTradeKey().getBinding().getBoundKeyTranslationKey()))
                        .append(" and click to bind a trade to auto-trade."),
                4, height - client.textRenderer.fontHeight - 4, Color.WHITE.getRGB(), false
        );
    }

    @Inject(
            method = "init",
            at = @At("HEAD")
    )
    private void init(CallbackInfo ci) {

        addDrawableChild(
                new ButtonWidget.Builder(Text.of("Reset Auto-Trade"), (action) -> {
                    getLastVillagerUuid().ifPresent((uuid -> AutoTradeClient.INSTANCE.getSavedTrades().remove(uuid.toString())));
                    client.player.sendMessage(Text.of("Reset auto-trade."));
                })
                        .position(5, 5)
                        .size(300, 20)
                        .build()
        );

//        if (!AutoTradeClient.INSTANCE.getQuickTradeKey().getBinding().isPressed()) {
        if (!Screen.hasControlDown()) {
            return;
        }

        this.client.setScreen(null);

        // Auto trade
        getLastVillagerUuid().flatMap(AutoTradeClient.INSTANCE::getSavedTrade).ifPresent((tradeIndex) -> {
            final int[] ticks = {0};

            AutoTradeClient.INSTANCE.runUntilFalse(() -> {
                TradeOfferList tradeOfferList = this.handler.getRecipes();
                ticks[0]++;

                if (!tradeOfferList.isEmpty()) {
                    TradeOffer selectedOffer = tradeOfferList.get(tradeIndex);

                    selectedIndex = tradeIndex;

                    client.player.sendMessage(Text.of("Attempting auto-trade."));

                    // todo stop if price is too high

                    tradeUntilDoneOrEmpty(selectedOffer, handler, this::close);
                    return false;
                }

                if (ticks[0] < 100) {
                    client.player.sendMessage(Text.of("Failed to auto-trade."));
                    return false;
                }
                return true;
            });
        });
    }

    @Inject(
            method = "syncRecipeIndex",
            at = @At("TAIL")
    )
    private void syncRecipeIndex(CallbackInfo ci) {
        // Called when a new trade is selected, server is notified at end of call

//        if (AutoTradeClient.INSTANCE.getQuickTradeKey().getBinding().isPressed()) {
        if (Screen.hasControlDown()) {
            TradeOfferList tradeOfferList = this.handler.getRecipes();
            if (tradeOfferList.size() < selectedIndex) return;

            TradeOffer selectedOffer = tradeOfferList.get(selectedIndex);
            MerchantScreenHandler handler = getScreenHandler();

//            if (AutoTradeClient.INSTANCE.getBindAutoTradeKey().getBinding().isPressed()) {
            if (Screen.hasAltDown()) {
                // mark selected trade
                AutoTradeClient.INSTANCE.saveTrade(getLastVillagerUuid(), selectedIndex);

                client.player.sendMessage(Text.of(String.format("Saved trade slot %d for auto-trade.", selectedIndex)));
                return;
            }
            tradeUntilDoneOrEmpty(selectedOffer, handler, () -> {});
        }
    }

    @Inject(
            method = "renderArrow",
            at = @At("TAIL")
    )
    private void renderArrow(DrawContext context, TradeOffer tradeOffer, int x, int y, CallbackInfo ci) {
        TradeOfferList tradeOfferList = this.handler.getRecipes();
        Entity lastVillager = AutoTradeClient.INSTANCE.getLastVillager();
        if (lastVillager == null) return;

        Optional<Integer> selectedIndex = AutoTradeClient.INSTANCE.getSavedTrade(lastVillager.getUuid());
        if (selectedIndex.isEmpty()) return;

        int index = tradeOfferList.indexOf(tradeOffer);

        if (selectedIndex.get() == index) {
            context.drawVerticalLine(x + 1, y - 1, y + 20, Color.GREEN.getRGB());
        }
    }

    private Optional<UUID> getLastVillagerUuid() {
        Entity entity = AutoTradeClient.INSTANCE.getLastVillager();
        return Optional.ofNullable(entity == null ? null : entity.getUuid());
    }

    private Merchant getMerchant() {
        return ((MerchantScreenHandlerAccessor) handler).getMerchant();
    }

    private void tradeUntilDoneOrEmpty(TradeOffer selectedOffer, MerchantScreenHandler handler, Runnable after) {
        assert client.interactionManager != null;
        Slot outputSlot = handler.getSlot(2);
        AutoTradeClient.INSTANCE.runUntilFalse(() -> {
            // If player is null or they changed screen
            if (client.player == null) {
                after.run();
                return false;
            }
            if (client.player.currentScreenHandler != handler) {
                after.run();
                return false;
            }

            // Refresh items
            handler.setRecipeIndex(selectedIndex);
            handler.switchTo(selectedIndex);
            client.getNetworkHandler().sendPacket(new SelectMerchantTradeC2SPacket(selectedIndex));

            // Out of materials OR trade is out of stock

            // todo auto-convert emerald blocks if we're out of them
            // todo auto-trade without clicking a trade (preset trades?)
            boolean shouldStopTrading = !selectedOffer.matchesBuyItems(handler.slots.get(0).getStack(), handler.slots.get(1).getStack()) ||
                    selectedOffer.isDisabled();

            if (shouldStopTrading) {
                after.run();
                return false;
            }

            if (InventoryHelper.INSTANCE.hasSpace(client.player.getInventory(), selectedOffer.getSellItem())) {
                client.interactionManager.clickSlot(handler.syncId, outputSlot.id, 0, SlotActionType.QUICK_MOVE, client.player);
            } else {
                client.interactionManager.clickSlot(handler.syncId, outputSlot.id, 0, SlotActionType.THROW, client.player);
            }

            return true;
        });
    }

}
