package net.michael.openplease;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
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
    public static KeyMapping toggleOpen; // Keybinding for toggling auto-open
    public static KeyMapping toggleSound; // Keybinding for toggling sound
    private static KeyMapping getState; // Keybinding for getting states
    private boolean previousKeyStateOpen = false; // Track key state for toggling
    private boolean previousKeyStateSound = false; // Track key state for sound
    private boolean previousKeyGetSate = false; // Track key state for getting states
    public static boolean doorToggle = true; // Toggle for auto-open feature
    public static boolean soundToggle = true; // Toggle for sound feature
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
        toggleOpen = new KeyMapping(
                "key.openplease.toggle", // Translation key
                GLFW.GLFW_KEY_R,         // Default key
                "key.categories.openplease" // Category
        );
        toggleSound = new KeyMapping(
                "key.openplease.sound", // Translation key
                GLFW.GLFW_KEY_Y,         // Default key
                "key.categories.openplease" // Category
        );
        getState = new KeyMapping(
                "key.openplease.getstate", // Translation key
                GLFW.GLFW_KEY_U,         // Default key
                "key.categories.openplease" // Category
        );

        event.register(toggleOpen);
        event.register(toggleSound);
        event.register(getState);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("OpenPlease: Server is starting...");
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.LevelTickEvent event) {
        // Handle key toggling on the client
        if (event.level.isClientSide) {
            boolean currentKeyStateOpen = toggleOpen.isDown();
            boolean currentKeyStateSound = toggleSound.isDown();
            boolean currentKeyGetState = getState.isDown();
            if (currentKeyStateOpen && !previousKeyStateOpen) {
                doorToggle = !doorToggle;

                Minecraft.getInstance().gui.setOverlayMessage(
                        Component.literal(doorToggle ? "Auto-Open Enabled!" : "Auto-Open Disabled!")
                                .withStyle(doorToggle ? ChatFormatting.GREEN : ChatFormatting.RED),
                        true
                );
            }
            if (currentKeyStateSound && !previousKeyStateSound) {
                soundToggle = !soundToggle;

                Minecraft.getInstance().gui.setOverlayMessage(
                        Component.literal(soundToggle ? "Sound Enabled!" : "Sound Disabled!")
                                .withStyle(soundToggle ? ChatFormatting.GREEN : ChatFormatting.RED),
                        true
                );
            }
            if (currentKeyGetState && !previousKeyGetSate) {
                Minecraft.getInstance().gui.setOverlayMessage(
                        Component.literal("Auto-Open: ").withStyle(ChatFormatting.WHITE)
                                .append(Component.literal(doorToggle ? "Enabled" : "Disabled")
                                        .withStyle(doorToggle ? ChatFormatting.GREEN : ChatFormatting.RED))
                                .append(Component.literal(", Sound: ").withStyle(ChatFormatting.WHITE))
                                .append(Component.literal(soundToggle ? "Enabled" : "Disabled")
                                        .withStyle(soundToggle ? ChatFormatting.GREEN : ChatFormatting.RED)),
                        true
                );
            }

            previousKeyStateOpen = currentKeyStateOpen;
            previousKeyStateSound = currentKeyStateSound;
            previousKeyGetSate = currentKeyGetState;
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

                            if (block instanceof DoorBlock && block != Blocks.IRON_DOOR) {
                                boolean oldStateDoor = world.getBlockState(pos).getValue(DoorBlock.OPEN);
                                handleDoor(world, pos, playerPos);
                                if (oldStateDoor && !world.getBlockState(pos).getValue(DoorBlock.OPEN) && soundToggle) {
                                    world.playSound(null, pos, SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.BLOCKS, 1.0f, 1.0f);
                                }
                                if (!oldStateDoor && world.getBlockState(pos).getValue(DoorBlock.OPEN) && soundToggle) {
                                    world.playSound(null, pos, SoundEvents.WOODEN_DOOR_OPEN, SoundSource.BLOCKS, 1.0f, 1.0f);
                                }
                            }
                            if (block instanceof TrapDoorBlock && block != Blocks.IRON_TRAPDOOR) {
                                boolean oldStateTrapDoor = world.getBlockState(pos).getValue(TrapDoorBlock.OPEN);
                                handleTrapdoor(world, pos, playerPos);
                                if (oldStateTrapDoor && !world.getBlockState(pos).getValue(TrapDoorBlock.OPEN) && soundToggle) {
                                    world.playSound(null, pos, SoundEvents.WOODEN_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1.0f, 1.0f);
                                }
                                if (!oldStateTrapDoor && world.getBlockState(pos).getValue(TrapDoorBlock.OPEN) && soundToggle) {
                                    world.playSound(null, pos, SoundEvents.WOODEN_TRAPDOOR_OPEN, SoundSource.BLOCKS, 1.0f, 1.0f);
                                }
                            }
                            if (block instanceof FenceGateBlock) {
                                boolean oldStateFenceGate = world.getBlockState(pos).getValue(FenceGateBlock.OPEN);
                                handleFenceGate(world, pos, playerPos);
                                if (oldStateFenceGate && !world.getBlockState(pos).getValue(FenceGateBlock.OPEN) && soundToggle) {
                                    world.playSound(null, pos, SoundEvents.FENCE_GATE_CLOSE, SoundSource.BLOCKS, 1.0f, 1.0f);
                                }
                                if (!oldStateFenceGate && world.getBlockState(pos).getValue(FenceGateBlock.OPEN) && soundToggle) {
                                    world.playSound(null, pos, SoundEvents.FENCE_GATE_OPEN, SoundSource.BLOCKS, 1.0f, 1.0f);
                                }
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
        boolean shouldBeOpen = distance <= DOOR_DISTANCE + 2 * DOOR_DISTANCE + 2;

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
