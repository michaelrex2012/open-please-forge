package net.michael.openplease;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(OpenPlease.MOD_ID)
public class OpenPlease {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "openplease";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    public static KeyMapping toggle;

    private boolean previousKeyState = false; // Track key state for toggling
    public static boolean doorToggle = true; // Toggle for auto-open feature
    public static final float DOOR_DISTANCE = 2f; // Distance for door interaction

    public OpenPlease() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register key mappings
        modEventBus.addListener(this::registerKeyMappings);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register the creative tab listener
        modEventBus.addListener(this::addCreative);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Common setup logic (currently empty)
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        // Add items or blocks to creative mode tabs (currently empty)
    }

    @SubscribeEvent
    public void registerKeyMappings(RegisterKeyMappingsEvent event) {
        toggle = new KeyMapping(
                "key.openplease.toggle", // Translation key
                GLFW.GLFW_KEY_R,         // Default key
                "key.categories.openplease" // Category
        );

        event.register(toggle);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("OpenPlease: Server is starting...");
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.LevelTickEvent event) {
        // Handle key toggling on the client
        if (event.level.isClientSide) {
            boolean currentKeyState = toggle.isDown();
            if (currentKeyState && !previousKeyState) {
                doorToggle = !doorToggle;

                Minecraft.getInstance().gui.setOverlayMessage(
                        Component.literal(doorToggle ? "Auto-Open Enabled!" : "Auto-Open Disabled!")
                                .withStyle(doorToggle ? ChatFormatting.GREEN : ChatFormatting.RED),
                        true
                );
            }
            previousKeyState = currentKeyState;
            return; // Skip further processing for the client
        }

        // Server-side logic for door handling
        if (event.level instanceof ServerLevel world && event.phase == TickEvent.Phase.END && doorToggle) {
            for (Player player : world.players()) {
                BlockPos playerPos = player.blockPosition();

                // Check surrounding blocks within 4 blocks
                for (int x = -4; x <= 4; x++) {
                    for (int y = -4; y <= 4; y++) {
                        for (int z = -4; z <= 4; z++) {
                            BlockPos pos = playerPos.offset(x, y, z);
                            Block block = world.getBlockState(pos).getBlock();

                            if (block instanceof DoorBlock) {
                                handleDoor(world, pos, playerPos);
                            } else if (block instanceof TrapDoorBlock) {
                                handleTrapdoor(world, pos, playerPos);
                            } else if (block instanceof FenceGateBlock) {
                                handleFenceGate(world, pos, playerPos);
                            }
                        }
                    }
                }
            }
        }
    }

    private void handleDoor(Level world, BlockPos doorPos, BlockPos playerPos) {
        double distance = playerPos.distSqr(doorPos);
        boolean isOpen = world.getBlockState(doorPos).getValue(DoorBlock.OPEN);
        boolean shouldBeOpen = distance <= DOOR_DISTANCE * DOOR_DISTANCE;

        if (isOpen != shouldBeOpen) {
            world.setBlock(doorPos, world.getBlockState(doorPos).setValue(DoorBlock.OPEN, shouldBeOpen), 3);
        }
    }

    private void handleTrapdoor(Level world, BlockPos trapdoorPos, BlockPos playerPos) {
        double distance = playerPos.distSqr(trapdoorPos);
        boolean isOpen = world.getBlockState(trapdoorPos).getValue(TrapDoorBlock.OPEN);
        boolean shouldBeOpen = distance <= DOOR_DISTANCE * DOOR_DISTANCE;

        if (isOpen != shouldBeOpen) {
            world.setBlock(trapdoorPos, world.getBlockState(trapdoorPos).setValue(TrapDoorBlock.OPEN, shouldBeOpen), 3);
        }
    }

    private void handleFenceGate(Level world, BlockPos fenceGatePos, BlockPos playerPos) {
        double distance = playerPos.distSqr(fenceGatePos);
        boolean isOpen = world.getBlockState(fenceGatePos).getValue(FenceGateBlock.OPEN);
        boolean shouldBeOpen = distance <= DOOR_DISTANCE * DOOR_DISTANCE;

        if (isOpen != shouldBeOpen) {
            world.setBlock(fenceGatePos, world.getBlockState(fenceGatePos).setValue(FenceGateBlock.OPEN, shouldBeOpen), 3);
        }
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("OpenPlease: Client setup complete.");
        }
    }
}
