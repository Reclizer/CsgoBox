package com.reclizer.csgobox.gametest;

import com.reclizer.csgobox.CsgoBox;
import com.reclizer.csgobox.config.*;
import com.reclizer.csgobox.gui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import java.lang.reflect.*;
import java.util.*;

/** Exercises real editor widgets in memory without taking over the user's screen or menu. */
public final class EntityPickerClientChecks {
    private static Object get(Object object,String name) throws Exception {
        for (Class<?> type=object.getClass();type!=null;type=type.getSuperclass()) {
            try { Field f=type.getDeclaredField(name); f.setAccessible(true); return f.get(object); }
            catch (NoSuchFieldException ignored) {}
        }
        throw new NoSuchFieldException(name);
    }
    private static void set(Object object,String name,Object value) throws Exception {
        Field f=object.getClass().getDeclaredField(name); f.setAccessible(true); f.set(object,value);
    }
    private static Object call(Object object,String name) throws Exception {
        Method m=object.getClass().getDeclaredMethod(name); m.setAccessible(true); return m.invoke(object);
    }
    private static void check(boolean condition,String error) { if (!condition) throw new AssertionError(error); }
    private static void click(CsgoBoxCraftScreen screen,int x,int y) throws Exception {
        float scale=(float)get(screen,"uiScale");
        double mx=((int)get(screen,"leftPos")+x)*scale,my=((int)get(screen,"topPos")+y)*scale;
        screen.mouseClicked(mx,my,0); screen.mouseReleased(mx,my,0);
    }
    private static void query(CsgoBoxCraftScreen screen,String value) throws Exception {
        ((EditBox)((List<?>)get(screen,"fields")).get(0)).setValue(value);
    }
    private static void choose(CsgoBoxCraftScreen screen,String id) throws Exception {
        query(screen,id);
        List<?> choices=(List<?>)get(screen,"filteredEntities");
        int row=-1;
        for (int i=0;i<choices.size();i++) if (call(choices.get(i),"id").equals(id)) { row=i; break; }
        check(row>=0 && row<9,"Missing searchable living entity: "+id);
        click(screen,240,130+row*24);
    }
    public static void run() throws Exception {
        var mc=Minecraft.getInstance();
        var screen=new CsgoBoxCraftScreen(new CsgoBoxCraftMenu(99,mc.player.getInventory(),null),mc.player.getInventory(),Component.empty());
        screen.init(mc,960,540);
        set(screen,"page",1); set(screen,"acquisitionPage",1); call(screen,"rebuild");
        Object box=get(screen,"boxAcquisition"),key=get(screen,"keyAcquisition");
        click(screen,550,248);
        check((boolean)get(screen,"entityPicker"),"Selector button did not open modal");
        check(EntityPickerFixture.creature!=null && EntityDropTargets.types().contains(EntityPickerFixture.creature),"Forge mod attribute fixture missing from picker");
        choose(screen,"minecraft:zombie"); choose(screen,"minecraft:villager"); choose(screen,"selector_fixture:creature");
        check(((Set<?>)get(screen,"selectedEntities")).size()==3 && get(box,"entities").equals(""),"Pending selection leaked into editor draft");
        query(screen,""); click(screen,450,98);
        check(((List<?>)get(screen,"filteredEntities")).size()==3,"Selected-only filter failed");
        screen.init(mc,480,270);
        check(((Set<?>)get(screen,"selectedEntities")).size()==3 && (boolean)get(screen,"selectedEntitiesOnly"),"Resize lost modal state");
        click(screen,480,388);
        check(!(boolean)get(screen,"entityPicker") && get(box,"entities").toString().contains("selector_fixture:creature")
                && get(key,"entities").equals(""),"Apply changed the wrong acquisition target");
        var saved=(AcquisitionDefinition)call(box,"definition");
        check(saved.drop().entities().size()==3,"Draft serialization lost selected entities");
        saved.validate(value->true);
        String before=get(box,"entities").toString();
        click(screen,550,248); click(screen,130,388); screen.keyPressed(256,0,0);
        check(get(box,"entities").equals(before) && !(boolean)get(screen,"entityPicker"),"Esc committed clear or failed to close modal");
        click(screen,550,248); query(screen,""); screen.mouseScrolled(0,0,-1);
        check((int)get(screen,"entityPage")==1,"Scroll did not advance entity page");
        query(screen,"no_such_creature_1130");
        check(((List<?>)get(screen,"filteredEntities")).isEmpty() && (int)get(screen,"entityPage")==0,"Empty search did not reset pagination");
        click(screen,350,388);
        check(get(box,"entities").equals(before),"Cancel changed draft");
        set(screen,"key","minecraft:tripwire_hook"); set(screen,"acquireKey",true); call(screen,"rebuild");
        click(screen,550,248); choose(screen,"minecraft:iron_golem"); click(screen,480,388);
        check(get(key,"entities").equals("minecraft:iron_golem") && get(box,"entities").equals(before),"Key selection overwrote box drop targets");
        CsgoBox.LOGGER.info("ENTITY PICKER CLIENT CHECKS PASSED: registered mod creature, search, multiselect, pagination, resize, apply, cancel, independent box/key drafts and serialization");
    }
}
