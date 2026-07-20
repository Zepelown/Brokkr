package org.brokkr.enhancement.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.brokkr.enhancement.EnhancementAttemptService;
import org.brokkr.enhancement.EnhancementData;
import org.brokkr.enhancement.EnhancementMessages;
import org.brokkr.enhancement.EnhancementResult;
import org.brokkr.enhancement.item.ModItems;
import org.brokkr.enhancement.profile.WeaponEnhancementProfiles;

public class EnhancementMenu extends AbstractContainerMenu {
    public static final int WEAPON_SLOT = 0;
    public static final int STONE_SLOT = 1;
    public static final int PLAYER_INVENTORY_START = 2;
    public static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    public static final int HOTBAR_START = PLAYER_INVENTORY_END;
    public static final int HOTBAR_END = HOTBAR_START + 9;

    public static final int BUTTON_ENHANCE = 0;
    public static final int BUTTON_RESET = 1;

    public static final int DATA_STATE = 0;
    public static final int DATA_RESULT_TYPE = 1;
    public static final int DATA_PREVIOUS_LEVEL = 2;
    public static final int DATA_NEW_LEVEL = 3;
    public static final int DATA_SUCCESS_CHANCE = 4;
    public static final int DATA_ATTEMPT_SEQUENCE = 5;
    private static final int DATA_COUNT = 6;

    public static final int STATE_IDLE = 0;
    public static final int STATE_PROCESSING_LOCKED = 1;

    private final Container inputSlots;
    private final ContainerData data;

    public EnhancementMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(2), new SimpleContainerData(DATA_COUNT));
    }

    public EnhancementMenu(int containerId, Inventory playerInventory, net.minecraft.network.RegistryFriendlyByteBuf ignored) {
        this(containerId, playerInventory);
    }

    public EnhancementMenu(int containerId, Inventory playerInventory, Container inputSlots, ContainerData data) {
        super(ModMenus.ENHANCEMENT_MENU.get(), containerId);
        this.inputSlots = inputSlots;
        this.data = data;

        addSlot(new EnhancementSlot(inputSlots, WEAPON_SLOT, 44, 35));
        addSlot(new EnhancementStoneSlot(inputSlots, STONE_SLOT, 116, 35));

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 142));
        }

        addDataSlots(data);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_ENHANCE) {
            tryEnhance(player);
            return true;
        }
        if (id == BUTTON_RESET) {
            data.set(DATA_STATE, STATE_IDLE);
            broadcastChanges();
            return true;
        }
        return false;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (isLocked()) {
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (isLocked()) {
            return ItemStack.EMPTY;
        }

        Slot sourceSlot = slots.get(index);
        if (!sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copy = sourceStack.copy();

        if (index == WEAPON_SLOT || index == STONE_SLOT) {
            if (!moveItemStackTo(sourceStack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= PLAYER_INVENTORY_START && index < HOTBAR_END) {
            if (WeaponEnhancementProfiles.isSupported(sourceStack)) {
                if (!moveItemStackTo(sourceStack, WEAPON_SLOT, WEAPON_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (sourceStack.is(ModItems.ENHANCEMENT_STONE.get())) {
                if (!moveItemStackTo(sourceStack, STONE_SLOT, STONE_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < PLAYER_INVENTORY_END) {
                if (!moveItemStackTo(sourceStack, HOTBAR_START, HOTBAR_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(sourceStack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (sourceStack.isEmpty()) {
            sourceSlot.setByPlayer(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }

        return copy;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            clearContainer(player, inputSlots);
        }
    }

    public boolean canEnhance() {
        ItemStack weapon = getWeapon();
        ItemStack stone = getStone();
        return !isLocked()
                && WeaponEnhancementProfiles.isSupported(weapon)
                && stone.is(ModItems.ENHANCEMENT_STONE.get())
                && EnhancementData.getLevel(weapon) < EnhancementData.MAX_LEVEL;
    }

    public boolean isLocked() {
        return data.get(DATA_STATE) == STATE_PROCESSING_LOCKED;
    }

    public ItemStack getWeapon() {
        return inputSlots.getItem(WEAPON_SLOT);
    }

    public ItemStack getStone() {
        return inputSlots.getItem(STONE_SLOT);
    }

    public int getResultTypeId() {
        return data.get(DATA_RESULT_TYPE);
    }

    public int getPreviousLevel() {
        return data.get(DATA_PREVIOUS_LEVEL);
    }

    public int getNewLevel() {
        return data.get(DATA_NEW_LEVEL);
    }

    public int getSuccessChance() {
        return data.get(DATA_SUCCESS_CHANCE);
    }

    public int getAttemptSequence() {
        return data.get(DATA_ATTEMPT_SEQUENCE);
    }

    private void tryEnhance(Player player) {
        if (isLocked()) {
            return;
        }

        EnhancementResult result = EnhancementAttemptService.attempt(getWeapon(), getStone(), player.getRandom());
        data.set(DATA_RESULT_TYPE, result.type().ordinal());
        data.set(DATA_PREVIOUS_LEVEL, result.previousLevel());
        data.set(DATA_NEW_LEVEL, result.newLevel());
        data.set(DATA_SUCCESS_CHANCE, result.successChance());

        if (result.type() == EnhancementResult.Type.SUCCESS || result.type() == EnhancementResult.Type.FAILED_ROLL) {
            data.set(DATA_STATE, STATE_PROCESSING_LOCKED);
            data.set(DATA_ATTEMPT_SEQUENCE, data.get(DATA_ATTEMPT_SEQUENCE) + 1);
        }

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(EnhancementMessages.forResult(result));
        }
        broadcastChanges();
    }
}
