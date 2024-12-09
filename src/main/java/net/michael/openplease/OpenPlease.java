package net.michael.openplease;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(OpenPlease.MOD_ID)
public class OpenPlease {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "openplease";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public OpenPlease(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {

    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {

    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {

        }
    }


        public static final float DOOR_DISTANCE = 1;

        public OpenPlease() {
            // Register event handlers
            MinecraftForge.EVENT_BUS.register(this);
        }

        @SubscribeEvent
        public void onServerTick(TickEvent.LevelTickEvent event) {
            // Ensure we are on the server side and in the END phase of the tick
            if (event.level instanceof ServerLevel world && event.phase == TickEvent.Phase.END) {
                for (Player player : world.players()) {
                    BlockPos playerPos = player.blockPosition();

                    // Check surrounding blocks within 4 blocks
                    for (int x = -4; x <= 4; x++) {
                        for (int y = -4; y <= 4; y++) {
                            for (int z = -4; z <= 4; z++) {
                                BlockPos pos = playerPos.offset(x, y, z);
                                if (isDoor(world, pos)) {
                                    handleDoor(world, pos, playerPos);
                                }
                                if (isTrapdoor(world, pos)) {
                                    handleTrapdoor(world, pos, playerPos);
                                }
                                if (isFenceGate(world, pos)) {
                                    handleFenceGate(world, pos, playerPos);
                                }
                            }
                        }
                    }
                }
            }
        }

        private boolean isDoor(Level world, BlockPos pos) {
            Block block = world.getBlockState(pos).getBlock();
            return block instanceof DoorBlock;
        }

        private boolean isTrapdoor(Level world, BlockPos pos) {
            Block block = world.getBlockState(pos).getBlock();
            return block instanceof TrapDoorBlock;
        }

        private boolean isFenceGate(Level world, BlockPos pos) {
            Block block = world.getBlockState(pos).getBlock();
            return block instanceof FenceGateBlock;
        }

        private void handleDoor(Level world, BlockPos doorPos, BlockPos playerPos) {
            double distance = playerPos.distSqr(new Vec3i(doorPos.getX(), doorPos.getY(), doorPos.getZ()));

            boolean isOpen = world.getBlockState(doorPos).getValue(DoorBlock.OPEN);
            if (distance <= DOOR_DISTANCE * DOOR_DISTANCE && !isOpen) {
                world.setBlock(doorPos, world.getBlockState(doorPos).setValue(DoorBlock.OPEN, true), 3);
            } else if (distance > DOOR_DISTANCE * DOOR_DISTANCE && isOpen) {
                world.setBlock(doorPos, world.getBlockState(doorPos).setValue(DoorBlock.OPEN, false), 3);
            }
        }

        private void handleTrapdoor(Level world, BlockPos trapdoorPos, BlockPos playerPos) {
            double distance = playerPos.distSqr(new Vec3i(trapdoorPos.getX(), trapdoorPos.getY(), trapdoorPos.getZ()));

            boolean isOpen = world.getBlockState(trapdoorPos).getValue(TrapDoorBlock.OPEN);
            if (distance <= DOOR_DISTANCE * DOOR_DISTANCE && !isOpen) {
                world.setBlock(trapdoorPos, world.getBlockState(trapdoorPos).setValue(TrapDoorBlock.OPEN, true), 3);
            } else if (distance > DOOR_DISTANCE * DOOR_DISTANCE && isOpen) {
                world.setBlock(trapdoorPos, world.getBlockState(trapdoorPos).setValue(TrapDoorBlock.OPEN, false), 3);
            }
        }

        private void handleFenceGate(Level world, BlockPos fenceGatePos, BlockPos playerPos) {
            double distance = playerPos.distSqr(new Vec3i(fenceGatePos.getX(), fenceGatePos.getY(), fenceGatePos.getZ()));

            boolean isOpen = world.getBlockState(fenceGatePos).getValue(FenceGateBlock.OPEN);
            if (distance <= DOOR_DISTANCE * DOOR_DISTANCE && !isOpen) {
                world.setBlock(fenceGatePos, world.getBlockState(fenceGatePos).setValue(FenceGateBlock.OPEN, true), 3);
            } else if (distance > DOOR_DISTANCE * DOOR_DISTANCE && isOpen) {
                world.setBlock(fenceGatePos, world.getBlockState(fenceGatePos).setValue(FenceGateBlock.OPEN, false), 3);
            }
        }
}