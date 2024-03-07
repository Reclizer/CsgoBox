package com.reclizer.csgobox.capability.csbox;


import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;

public class CsboxCap  implements ICsboxCap{


    private final LivingEntity livingEntity;

    private long playSeed;

    private int mode;

    private String item;

    private int grade;
    public CsboxCap(@Nullable final LivingEntity entity) {
        this.livingEntity = entity;
    }

    @Override
    public long playerSeed() {
        return this.playSeed;
    }

    @Override
    public long setSeed(long seed) {
        this.playSeed=seed;
        return this.playSeed;
    }

    @Override
    public int mode() {
        return this.mode;
    }

    @Override
    public int setMode(int mode) {
        if(mode>-2&&mode<2){
            this.mode=mode;
        }
        return mode();
    }

    @Override
    public String setItem(String item) {
        this.item=item;
        return this.item;
    }

    @Override
    public String getItem() {
        if(this.item==null){
            return "";
        }
        return this.item;
    }

    @Override
    public int getGrade() {
        return this.grade;
    }

    @Override
    public int setGrade(int grade) {
        if(grade>0&&grade<6){
            this.grade=grade;
        }
        return this.grade;
    }


    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("csrandom", playerSeed());
        tag.putInt("csmode",mode());
        tag.putString("csitem",getItem());

        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        setSeed(nbt.getLong("csrandom"));
        setMode(nbt.getInt("csmode"));
        setItem(nbt.getString("csitem"));
    }


}
