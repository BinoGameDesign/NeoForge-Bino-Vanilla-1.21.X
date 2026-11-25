package com.bino.binosmining.item.custom;

import com.bino.binosmining.util.ToolUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;

import java.util.List;

// "extends PickaxeItem" = é uma picareta normal, com extras
// "implements MultiBreakTool" = promete saber dizer quais blocos extras quebrar
public class RubiPickaxeItem extends PickaxeItem implements MultiBreakTool {

    private static final int MAX_BLOCKS = 10;   // limite do veio

    public RubiPickaxeItem(Tier tier, Item.Properties properties) {
        super(tier, properties);                // repassa tudo para a picareta normal
    }

    @Override
    public List<BlockPos> getExtraBlocks(ServerPlayer player, BlockPos origin, BlockState state) {
        // Só funciona em minérios (tag c:ores)
        if (!state.is(Tags.Blocks.ORES)) return List.of();

        // Só funciona se a picareta é a ferramenta correta (senão o bloco nem dropa)
        if (!player.getMainHandItem().isCorrectToolForDrops(state)) return List.of();

        String family = oreFamily(state);       // ex: "minecraft:iron_ore"

        // Acha minérios conectados da mesma família, até o limite
        return ToolUtil.findConnected(player.level(), origin,
                s -> s.is(Tags.Blocks.ORES) && oreFamily(s).equals(family),
                MAX_BLOCKS);
    }

    // Transforma o bloco em um "nome de família", ignorando a versão deepslate.
    // "minecraft:deepslate_iron_ore" e "minecraft:iron_ore" viram o mesmo nome,
    // e "binosmining:rubi_deepslate_ore" vira "binosmining:rubi_ore".
    // (guardado por bloco: antes remontava o texto para cada um dos 26 vizinhos de cada bloco do veio)
    private static final java.util.Map<net.minecraft.world.level.block.Block, String> FAMILIES = new java.util.concurrent.ConcurrentHashMap<>();

    private static String oreFamily(BlockState state) {
        return FAMILIES.computeIfAbsent(state.getBlock(), b -> {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(b);
            return id.getNamespace() + ":" + id.getPath().replace("deepslate_", "");
        });
    }
}