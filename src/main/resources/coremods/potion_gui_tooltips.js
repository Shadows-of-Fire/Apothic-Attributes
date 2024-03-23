function initializeCoreMod() {
    return {
        'apothic_attributes_potion_gui_tooltips': {
            'target': {
                'type': 'METHOD',
                'class': 'net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen',
                'methodName': 'renderEffects',
                'methodDesc': '(Lnet/minecraft/client/gui/GuiGraphics;II)V'
            },
            'transformer': function(method) {
                var owner = "dev/shadowsoffire/apothic_attributes/asm/ALHooks";
                var name = "getEffectTooltip";
                var desc = "(Lnet/minecraft/client/gui/screens/inventory/EffectRenderingInventoryScreen;Lnet/minecraft/world/effect/MobEffectInstance;Ljava/util/List;)Ljava/util/List;";
                var instr = method.instructions;

                var ASMAPI = Java.type('net.neoforged.coremod.api.ASMAPI');
                var Opcodes = Java.type('org.objectweb.asm.Opcodes');
                var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
                var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode');
                var InsnList = Java.type('org.objectweb.asm.tree.InsnList');
				ASMAPI.log('INFO', 'Patching EffectRenderingInventoryScreen#renderEffects');

                var insertion = null;
                var i;
                for (i = instr.size() - 1; i > 0; i--) {
                    var n = instr.get(i);
                    // The list is stored in local variable 13, so ALOAD 13 is loading the list.
                    // That's the list we want to override, so we just change the value before it loads.
                    if (n.getOpcode() == Opcodes.ASTORE && n.var == 13) {
                        insertion = n;
                        break;
                    }
                }

                var insns = new InsnList();
                insns.add(new VarInsnNode(Opcodes.ALOAD, 0));  // EffectRenderingInventoryScreen
                insns.add(new VarInsnNode(Opcodes.ALOAD, 12)); // MobEffectInstance
                insns.add(new VarInsnNode(Opcodes.ALOAD, 13)); // List<Component>
                insns.add(ASMAPI.buildMethodCall(
                    owner,
                    name,
                    desc,
                    ASMAPI.MethodType.STATIC));
                insns.add(new VarInsnNode(Opcodes.ASTORE, 13));
                instr.insert(insertion, insns);

                return method;
            }
        }
    }
}