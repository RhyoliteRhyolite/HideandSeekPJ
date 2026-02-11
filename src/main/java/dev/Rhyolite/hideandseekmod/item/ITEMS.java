package dev.Rhyolite.hideandseekmod.item;

import dev.Rhyolite.hideandseekmod.HideandSeekMod;
import dev.Rhyolite.hideandseekmod.block.TrapBlock;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ITEMS {
    // 1. 여기서 직접 ITEMS 등록기를 만듭니다. (HideandSeekMod에서 가져오지 않음)
// DeferredRegister.Items 대신 이 방식을 권장합니다.
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, HideandSeekMod.MODID);

    public static final DeferredHolder<Item, Item> TRAP_ITEM = ITEMS.register("trap",
            () -> new BlockItem(ModBlocks.TRAP_BLOCK.get(), new Item.Properties())); // ModBlocks에서 가져오기!

    // 에너지 드링크
    public static final DeferredHolder<Item, Item> ENERGY_DRINK = ITEMS.register("energy_drink",
            () -> new Item(new Item.Properties().stacksTo(1)) {
                @Override
                public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
                    ItemStack stack = player.getItemInHand(hand);

                    // 1. 즉시 적용: 이동 속도 II (Amplifier 1) / 10초 (200틱)
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1));

                    player.displayClientMessage(Component.literal("§e⚡ 에너지가 넘쳐흐릅니다!"), true);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.0F, 1.0F);
                    // 3. 서버에서 10초 뒤에 실행될 작업 예약 (핵심!)
                    if (!level.isClientSide && level.getServer() != null) {
                        // 현재 서버 틱 + 200틱(10초) 뒤에 실행
                        level.getServer().tell(new net.minecraft.server.TickTask(level.getServer().getTickCount() + 200, () -> {
                            if (player.isAlive()) {
                                // 이동 속도 I (Amplifier 0) 부여 (시간은 무제한급으로)
                                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 9999999, 0, false, false, true));
                            }
                        }));
                    }

                    if (!player.getAbilities().instabuild) stack.shrink(1);
                    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());


                }

            });

    // 칠판 지우개
    public static final DeferredHolder<Item, Item> CHALK_ERASER = ITEMS.register("chalk_eraser",
            () -> new Item(new Item.Properties().stacksTo(1)) {
                @Override
                public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
                    ItemStack stack = player.getItemInHand(hand);

                    player.displayClientMessage(Component.literal("§7🌫 칠판 지우개를 사용했습니다!"), true);

                    if (level.isClientSide) {
                        // 파티클 개수를 30개에서 300개로 대폭 늘림
                        for (int i = 0; i < 200; i++) {
                            // 생성 위치를 플레이어 발밑에서 머리 위까지 넓힘 (랜덤 범위 증가)
                            double xOffset = level.random.nextGaussian() * 1.7; // 가로 범위 (1.7배)
                            double yOffset = level.random.nextFloat() * 2.5;    // 높이 범위 (최대 2.5블록)
                            double zOffset = level.random.nextGaussian() * 1.7; // 세로 범위 (1.7배)

                            level.addParticle(
                                    ParticleTypes.CAMPFIRE_COSY_SMOKE, // 캠프파이어 연기 (크고 오래감)
                                    player.getX() + xOffset,
                                    player.getY() + yOffset,
                                    player.getZ() + zOffset,
                                    0, 0.01, 0 // 위로 천천히 올라가는 속도
                            );
                        }
                    }

                    if (!level.isClientSide) {
                        level.getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(5.0D)).forEach(target -> {
                            if (target != player) {
                                target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0));
                            }
                        });
                    }

                    // 소리도 조금 더 묵직하게 변경 (기존 양초 끄는 소리 -> 펑 소리 느낌)
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.PLAYERS, 1.0F, 0.7F);

                    if (!player.getAbilities().instabuild) stack.shrink(1);
                    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
                }
            }
    );
// 1. 등록기(Register) 생성
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, "hideandseekmod");

    // 2. 덫 블록 등록
    public static final DeferredHolder<Block, Block> TRAP_BLOCK = BLOCKS.register("trap",
            () -> new TrapBlock(BlockBehaviour.Properties.of()
                    .noCollission()
                    .instabreak()
                    .noOcclusion()
            ));
}
