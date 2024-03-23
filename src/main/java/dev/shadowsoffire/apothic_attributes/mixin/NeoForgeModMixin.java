package dev.shadowsoffire.apothic_attributes.mixin;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import dev.shadowsoffire.apothic_attributes.impl.BooleanAttribute;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mixin(NeoForgeMod.class)
public class NeoForgeModMixin {

    @WrapOperation(method = { "<clinit>" }, at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/registries/DeferredRegister;register(Ljava/lang/String;Ljava/util/function/Supplier;)Lnet/neoforged/neoforge/registries/DeferredHolder;"), require = 1)
    private static DeferredHolder<Attribute, Attribute> apoth_makeBoolAttr(DeferredRegister<Attribute> defReg, String name, Supplier<Attribute> supplier, Operation<DeferredHolder<Attribute, Attribute>> original) {
        if ("creative_flight".equals(name)) {
            return original.call(defReg, name, (Supplier<Attribute>) () -> new BooleanAttribute("neoforge.creative_flight", false));
        }
        System.out.println(name);
        return original.call(defReg, name, supplier);
    }
}
