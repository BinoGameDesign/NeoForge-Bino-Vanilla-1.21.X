package com.bino.binosmovement;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

// Bino's Vanilla+ Movement: o motor de animacao do jogador. Funciona sozinho; o Bino's Vanilla+ Combat precisa dele.
//  - animacoes do Blockbench (client/anim/BbAnims) e a vida procedural no corpo (client/anim/PlayerLife)
//  - camera de primeira pessoa que acompanha o corpo (client/HeadCam) e o proprio corpo visivel (client/FirstPersonBody)
//  - camera no ombro (client/ShoulderCam)
// Outros mods entram pelos ganchos de client/MovementHooks (o Combat registra os golpes e o rolamento la).
@Mod(BinosMovement.MOD_ID)
public class BinosMovement {
    public static final String MOD_ID = "binosmovement";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BinosMovement(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, MovementConfig.SPEC);
    }
}
