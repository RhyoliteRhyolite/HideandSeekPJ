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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

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
    //저주인형
    public static final DeferredHolder<Item, Item> VOODOO_DOLL = ITEMS.register("voodoo_doll",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)) {

                // 1. 아이템 사용 시간 설정 (3초 = 60틱)
                @Override
                public int getUseDuration(ItemStack stack, LivingEntity entity) {
                    return 60;
                }

                // 2. 사용 모션 설정 (활 당기는 모션 or 먹는 모션)
                @Override
                public UseAnim getUseAnimation(ItemStack stack) {
                    return UseAnim.BOW; // 인형을 손에 들고 집중하는 모션
                }

                // 3. 우클릭 시 사용 시작 (차징 시작)
                @Override
                public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
                    ItemStack stack = player.getItemInHand(hand);

                    // 술래는 사용 불가
                    if (player.getTags().contains("seeker")) {
                        if (!level.isClientSide) player.displayClientMessage(Component.literal("§c술래는 부두인형을 사용할 수 없습니다!"), true);
                        return InteractionResultHolder.fail(stack);
                    }

                    player.startUsingItem(hand); // 3초 카운트다운 시작
                    return InteractionResultHolder.consume(stack);
                }

                // 4. 3초 사용이 완료되었을 때 실행되는 로직
                @Override
                public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entityLiving) {
                    if (!level.isClientSide && entityLiving instanceof Player player) {

                        // [오류 해결] 명시적 형변환을 통해 List<Player>로 맞춤
                        List<Player> targets = level.players().stream()
                                .map(p -> (Player) p) // ? extends Player를 Player로 캐스팅
                                .filter(p -> p != player) // 나 제외
                                .filter(p -> !p.getTags().contains("seeker")) // 술래 제외
                                .collect(java.util.stream.Collectors.toList());

                        if (targets.isEmpty()) {
                            player.displayClientMessage(Component.literal("§7텔레포트할 대상이 없습니다."), true);
                            return stack;
                        }

                        // 랜덤 타겟 선정 및 이동
                        Player target = targets.get(level.random.nextInt(targets.size()));

                        // 이동 전 소리
                        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);

                        // 텔레포트!
                        player.teleportTo(target.getX(), target.getY(), target.getZ());

                        // 이동 후 소리
                        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);

                        // 메시지 출력
                        player.displayClientMessage(Component.literal("§d부두인형이 당신을 §f" + target.getScoreboardName() + "§d에게 인도했습니다!"), true);

                        // [추가됨] 실명 효과 부여 (이동 후 3초간 앞이 안 보임)
                        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));

                        // 아이템 소모
                        if (!player.getAbilities().instabuild) {
                            stack.shrink(1);
                        }
                    }
                    return stack;
                }
            });

    //표식
    public static final DeferredHolder<Item, Item> SEEKER_MARK = ITEMS.register("seeker_mark",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)) {
                @Override
                public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
                    ItemStack stack = player.getItemInHand(hand);

                    if (!level.isClientSide) {
                        // 1. 술래 체크 (도망자가 쓰면 실패)
                        if (!player.getTags().contains("seeker")) {
                            player.displayClientMessage(Component.literal("§c이 아이템은 술래 전용입니다."), true);
                            return InteractionResultHolder.fail(stack);
                        }

                        // 2. 도망자 목록 가져오기
                        List<Player> targets = level.players().stream()
                                .map(p -> (Player) p)
                                .filter(p -> !p.getTags().contains("seeker")) // 술래가 아닌 사람(도망자)만
                                .collect(java.util.stream.Collectors.toList());

                        if (targets.isEmpty()) {
                            player.displayClientMessage(Component.literal("§7표식을 남길 도망자가 없습니다."), true);
                            return InteractionResultHolder.fail(stack);
                        }

                        // 3. 랜덤 타겟 선정 (이미 코드가 있다면 이 부분부터 수정)
                        Player target = targets.get(level.random.nextInt(targets.size()));

                        // [중요] ServerPlayer로 형변환해야 connection을 사용할 수 있습니다.
                        if (target instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {

                            // 4. 효과 적용: 발광 (1초)
                            serverPlayer.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20, 0, false, false, false));

                            // 5. 타이틀 출력 (serverPlayer의 connection 사용)
                            serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(
                                    Component.literal("§c⚠ 표식되었습니다! ⚠")
                            ));
                            serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(
                                    Component.literal("§7술래가 당신의 위치를 파악했습니다.")
                            ));

                            // 6. 소리 재생
                            level.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                                    SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 1.0F, 1.0F);
                        }

                        // 술래에게 성공 메시지
                        player.displayClientMessage(Component.literal("§e" + target.getScoreboardName() + "§f에게 표식을 남겼습니다!"), true);

                        // 아이템 소모
                        if (!player.getAbilities().instabuild) {
                            stack.shrink(1);
                        }
                    }

                    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
                }
            });

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
