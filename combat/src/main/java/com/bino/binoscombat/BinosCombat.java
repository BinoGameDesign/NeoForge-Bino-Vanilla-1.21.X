package com.bino.binoscombat;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

// Bino's Vanilla+ Combat: hack and slash (combos, golpes do Blockbench, parry, postura, Issen, rajada, rolamento,
// numeros de dano, mira nova). Precisa do Bino's Vanilla+ Movement (motor de animacao e camera).
// Servidor: pacote combat/. Cliente: client/combat/ e client/combo/. Ganchos para o Vanilla+: CombatHooks.
@Mod(BinosCombat.MOD_ID)
public class BinosCombat {
    public static final String MOD_ID = "binoscombat";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BinosCombat(IEventBus modEventBus, ModContainer modContainer) {
        CombatSounds.register(modEventBus);
        CombatParticles.register(modEventBus);
    }
}
