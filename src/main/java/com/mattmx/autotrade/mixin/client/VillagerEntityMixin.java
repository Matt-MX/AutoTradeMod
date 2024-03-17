package com.mattmx.autotrade.mixin.client;

import com.mattmx.autotrade.client.AutoTradeClient;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VillagerEntity.class)
public class VillagerEntityMixin {

    @Inject(
            method = "interactMob",
            at = @At(
                    target = "Lnet/minecraft/entity/passive/VillagerEntity;beginTradeWith(Lnet/minecraft/entity/player/PlayerEntity;)V",
                    value = "HEAD"
            )
    )
    public void interactMob(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        // Runs after UI opens
        AutoTradeClient.INSTANCE.setLastVillager((VillagerEntity) (Object) this);
    }


}
