package com.reclizer.csgobox.block;

import com.reclizer.csgobox.config.*;
import com.reclizer.csgobox.item.*;
import com.reclizer.csgobox.packet.DisplayReward;
import com.reclizer.csgobox.server.BoxOpening;
import com.reclizer.csgobox.sounds.ModSounds;
import com.reclizer.csgobox.utils.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import java.util.*;

/** Ready -> fixed-result animation -> empty case, or a timed close/reset for key-only cases. */
public final class PlacedBoxEntity extends BlockEntity {
    private String poolId="";
    private boolean empty, opening, resetting, lidOpen, emitting;
    private ConsumptionMode consumption=ConsumptionMode.BOTH;
    private float lidFrom;
    private long lidChanged, openingStarted, visualSeed, rewardDroppedAt;
    private ItemStack pending=ItemStack.EMPTY;
    private int pendingGrade=1, lastSound=-1;
    private UUID opener;
    private List<DisplayReward> samples=List.of();
    private ReelMotion motion;
    public PlacedBoxEntity(BlockPos pos,BlockState state) { super(ModBlocks.BOX_ENTITY.get(),pos,state); }
    public String poolId() { return poolId; }
    public boolean isEmpty() { return empty; }
    public boolean isOpening() { return opening; }
    public boolean isBusy() { return opening || resetting; }
    public boolean lidOpen() { return lidOpen; }
    public long visualSeed() { return visualSeed; }
    public List<DisplayReward> samples() { return samples; }
    public DisplayReward winner() { return new DisplayReward(pending,pendingGrade); }
    public float elapsed(float partial) { return level==null?0:Math.max(0,level.getGameTime()-openingStarted+partial); }
    public float spinTicks(float partial) { return elapsed(partial)-GroundAnimation.LID_TICKS; }
    public float lidProgress(float partial) {
        return GroundAnimation.lid(lidFrom,lidOpen,level==null?0:level.getGameTime()-lidChanged+partial);
    }
    private void sync() {
        setChanged();
        if (level!=null) level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
    }
    public void setPoolId(String id) {
        if (isBusy() || empty) return;
        poolId=BoxDefinition.validId(id)?id:""; refreshAppearance(); sync();
    }
    public void setEmpty() {
        poolId=""; empty=true; opening=false; resetting=false; pending=ItemStack.EMPTY; samples=List.of(); opener=null; motion=null; sync();
    }
    private void setLid(boolean open) {
        lidFrom=lidProgress(0); lidOpen=open; lidChanged=level==null?0:level.getGameTime();
    }
    public void toggleEmpty() {
        if (!empty || isBusy() || level==null || level.isClientSide) return;
        setLid(!lidOpen);
        level.playSound(null,worldPosition,lidOpen?SoundEvents.CHEST_OPEN:SoundEvents.CHEST_CLOSE,SoundSource.BLOCKS,.65F,1.05F);
        level.gameEvent(null,lidOpen?net.minecraft.world.level.gameevent.GameEvent.BLOCK_OPEN:net.minecraft.world.level.gameevent.GameEvent.BLOCK_CLOSE,worldPosition);
        sync();
    }
    public boolean begin(BoxDefinition definition,BoxOpening.Result result,UUID player,long seed) {
        if (isBusy() || empty || isRemoved() || level==null || level.isClientSide || result.reward().isEmpty()) return false;
        var visuals=new ArrayList<DisplayReward>(); var regular=definition.previewRewards();
        int count=Math.min(15,regular.size());
        for (int i=0;i<count;i++) {
            var entry=regular.get(i*regular.size()/count);
            visuals.add(new DisplayReward(ItemNBT.parseStrict(entry.item()),entry.grade()));
        }
        if (!definition.grade5().isEmpty()) visuals.add(new DisplayReward(new ItemStack(ModItems.ITEM_CSGO_KEY0.get()),5));
        if (visuals.isEmpty()) visuals.add(new DisplayReward(result.reward().copy(),result.grade()));
        pending=result.reward().copy(); pendingGrade=result.grade(); opener=player; visualSeed=seed;
        consumption=definition.mode(); rewardDroppedAt=0;
        samples=List.copyOf(visuals); motion=GroundAnimation.motion(seed,samples.size());
        openingStarted=level.getGameTime(); opening=true; lastSound=motion.underPointer(0); setLid(true); sync(); return true;
    }
    public void refreshAppearance() {
        if (level==null || level.isClientSide || isRemoved() || isBusy() || empty) return;
        var definition=CsgoBoxManage.find(poolId); var state=getBlockState();
        if (definition==null || !state.is(ModBlocks.BOX.get())) return;
        var appearance=BoxAppearance.byId(definition.appearance());
        if (state.getValue(PlacedBoxBlock.APPEARANCE)!=appearance)
            level.setBlock(worldPosition,state.setValue(PlacedBoxBlock.APPEARANCE,appearance),3);
    }
    public static void serverTick(Level level,BlockPos pos,BlockState state,PlacedBoxEntity box) {
        if (box.resetting) {
            if (level.getGameTime()-box.rewardDroppedAt>=GroundAnimation.REUSE_DELAY_TICKS) box.resetForReuse();
            return;
        }
        if (box.opening) {
            float elapsed=box.spinTicks(0);
            if (elapsed>=0 && elapsed<ReelMotion.DURATION && box.motion!=null) {
                int index=box.motion.underPointer(elapsed);
                if (index!=box.lastSound) level.playSound(null,pos,ModSounds.CS_DITA.get(),SoundSource.BLOCKS,.35F,1);
                box.lastSound=index;
            }
            if (box.elapsed(0)>=GroundAnimation.TOTAL_TICKS && box.emitReward()) {
                if (box.isRemoved() || !level.getBlockState(pos).is(ModBlocks.BOX.get())) return;
                if (box.consumption==ConsumptionMode.KEY) {
                    box.opening=false; box.resetting=true; box.rewardDroppedAt=level.getGameTime();
                    box.samples=List.of(); box.opener=null; box.motion=null; box.sync();
                } else box.setEmpty();
                level.playSound(null,pos,SoundEvents.ITEM_PICKUP,SoundSource.BLOCKS,.45F,1.25F);
                if (!state.canSurvive(level,pos)) level.destroyBlock(pos,true);
            }
        } else {
            if (!box.empty && !box.lidOpen && box.lidFrom!=0 && level.getGameTime()-box.lidChanged>=GroundAnimation.LID_TICKS) {
                box.lidFrom=0; box.lidChanged=0; box.sync();
            }
            if ((level.getGameTime()+pos.asLong())%20==0) box.refreshAppearance();
        }
    }
    private void resetForReuse() {
        setLid(false); resetting=false; openingStarted=0; rewardDroppedAt=0; visualSeed=0; lastSound=-1;
        pending=ItemStack.EMPTY; samples=List.of(); opener=null; motion=null;
        level.playSound(null,worldPosition,SoundEvents.CHEST_CLOSE,SoundSource.BLOCKS,.65F,1.05F);
        level.gameEvent(null,net.minecraft.world.level.gameevent.GameEvent.BLOCK_CLOSE,worldPosition);
        refreshAppearance(); sync();
    }
    /** Canceled spawns keep the fixed result pending; ordinary ticks retry without drawing again. */
    private boolean emitReward() {
        if (level==null || level.isClientSide || pending.isEmpty() || emitting) return false;
        var item=new ItemEntity(level,worldPosition.getX()+.5,
                worldPosition.getY()+getBlockState().getValue(PlacedBoxBlock.APPEARANCE).height()/16+.25,
                worldPosition.getZ()+.5,pending.copy());
        item.setDeltaMovement(0,.2,0); item.setDefaultPickUpDelay(); if (opener!=null) item.setThrower(opener);
        emitting=true;
        try {
            if (!level.addFreshEntity(item)) return false;
            pending=ItemStack.EMPTY; return true;
        } finally { emitting=false; }
    }
    /** Settle the fixed result once; only modes that consumed the case leave an empty shell. */
    public void onRemoved() {
        if (isBusy() && level!=null && !level.isClientSide) {
            if (opening) emitReward();
            opening=false; resetting=false; empty=consumption.consumesBox();
            if (empty) poolId="";
            pending=ItemStack.EMPTY; samples=List.of(); opener=null; motion=null; setChanged();
        }
    }
    public ItemStack asItem() {
        if (empty || isBusy() && consumption.consumesBox()) return EmptyBoxItem.create(getBlockState().getValue(PlacedBoxBlock.APPEARANCE));
        return BoxDefinition.validId(poolId)?ItemCsgoBox.setBoxId(poolId,new ItemStack(ModItems.ITEM_CSGOBOX.get())):ItemStack.EMPTY;
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!empty) tag.putString(ItemCsgoBox.POOL_ID_TAG,poolId);
        if (empty || isBusy() || lidOpen || lidFrom!=0) {
            if (empty || isBusy()) tag.putString("Phase",opening?"opening":resetting?"resetting":"empty");
            tag.putBoolean("LidOpen",lidOpen);
            tag.putFloat("LidFrom",lidFrom); tag.putLong("LidChanged",lidChanged);
        }
        if (isBusy()) tag.putString("Consumption",consumption.id());
        if (resetting) tag.putLong("RewardDroppedAt",rewardDroppedAt);
        if (opening) {
            tag.putLong("OpeningStarted",openingStarted); tag.putLong("VisualSeed",visualSeed);
            tag.put("PendingReward",pending.save(new CompoundTag())); tag.putInt("Grade",pendingGrade);
            if (opener!=null) tag.putUUID("Opener",opener);
            var list=new ListTag();
            for (var sample:samples) {
                var entry=new CompoundTag(); entry.put("Item",sample.stack().save(new CompoundTag())); entry.putInt("Grade",sample.grade()); list.add(entry);
            }
            tag.put("ReelSamples",list);
        }
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        poolId=BoxDefinition.validId(tag.getString(ItemCsgoBox.POOL_ID_TAG))?tag.getString(ItemCsgoBox.POOL_ID_TAG):"";
        empty="empty".equals(tag.getString("Phase")); opening="opening".equals(tag.getString("Phase")); resetting="resetting".equals(tag.getString("Phase"));
        consumption=ConsumptionMode.valid(tag.getString("Consumption"))?ConsumptionMode.parse(tag.getString("Consumption")):ConsumptionMode.BOTH;
        rewardDroppedAt=tag.getLong("RewardDroppedAt");
        lidOpen=tag.getBoolean("LidOpen"); lidFrom=Math.max(0,Math.min(1,tag.getFloat("LidFrom"))); lidChanged=tag.getLong("LidChanged");
        openingStarted=tag.getLong("OpeningStarted"); visualSeed=tag.getLong("VisualSeed");
        pending=opening?ItemStack.of(tag.getCompound("PendingReward")):ItemStack.EMPTY;
        pendingGrade=Math.max(1,Math.min(5,tag.getInt("Grade"))); opener=tag.hasUUID("Opener")?tag.getUUID("Opener"):null;
        var list=tag.getList("ReelSamples",Tag.TAG_COMPOUND); var visuals=new ArrayList<DisplayReward>();
        for (int i=0;i<Math.min(16,list.size());i++) {
            var entry=list.getCompound(i); var stack=ItemStack.of(entry.getCompound("Item"));
            if (!stack.isEmpty()) visuals.add(new DisplayReward(stack,Math.max(1,Math.min(5,entry.getInt("Grade")))));
        }
        samples=List.copyOf(visuals);
        if (opening && pending.isEmpty()) { opening=false; empty=true; poolId=""; }
        if (opening && samples.isEmpty()) samples=List.of(new DisplayReward(pending,pendingGrade));
        motion=opening?GroundAnimation.motion(visualSeed,samples.size()):null; lastSound=-1;
    }
    @Override public AABB getRenderBoundingBox() { return new AABB(worldPosition).inflate(.25).expandTowards(0,1,0); }
    @Override public CompoundTag getUpdateTag() { var tag=saveWithoutMetadata(); tag.remove("Opener"); return tag; }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
