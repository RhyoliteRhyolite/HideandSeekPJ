package dev.Rhyolite.hideandseekmod;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.TickTask;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ITEMS {
    // 1. 여기서 직접 ITEMS 등록기를 만듭니다. (HideandSeekMod에서 가져오지 않음)
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HideandSeekMod.MODID);

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

                    // 2. 예약 실행: 10초(200틱) 뒤에 실행될 코드
                    if (!level.isClientSide) {
                        level.getServer().tell(new TickTask(level.getServer().getTickCount() + 200, () -> {
                            // 플레이어가 살아있는지 확인 (죽었으면 효과를 줄 필요 없음)
                            if (player.isAlive()) {
                                // 이동 속도 I (Amplifier 0) / 5초 (100틱) 부여
                                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 0));

                                // 알림 메시지 (선택 사항)
                                player.displayClientMessage(Component.literal("§6에너지 드링크의 효능이 끝났습니다."), true);
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

    // 덫
    public static final DeferredHolder<Item, Item> TRAP = ITEMS.register("trap",
            () -> new Item(new Item.Properties().stacksTo(1)) {
                @Override
                public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
                    ItemStack stack = player.getItemInHand(hand);
                    level.getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(3.0D)).forEach(target -> {
                        if (target != player) {
                            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 5));
                            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                                    SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.5F, 1.0F);
                        }
                    });
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
                }
            }
    );
}