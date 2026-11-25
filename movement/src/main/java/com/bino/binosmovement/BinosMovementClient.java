package com.bino.binosmovement;

import com.bino.binosmovement.client.MovementHooks;
import com.bino.binosmovement.client.anim.Locomotion;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// so no cliente: tela de opcoes do mod (Mods > Bino's Vanilla+ Movement > Configurar)
@Mod(value = BinosMovement.MOD_ID, dist = Dist.CLIENT)
public class BinosMovementClient {
    public BinosMovementClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        // camera: animacoes do dia a dia por ultimo (golpes do Combat entram antes, na frente da lista)
        MovementHooks.BODY_SOURCES.add(Locomotion::bodyFrame);
    }
}
