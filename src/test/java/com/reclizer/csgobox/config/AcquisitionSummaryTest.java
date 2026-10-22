package com.reclizer.csgobox.config;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AcquisitionSummaryTest {
    @Test void onlyEnabledAcquisitionRulesArePublic() {
        var draft = new AcquisitionDefinition(new AcquisitionDefinition.Crafting(false,Collections.nCopies(9,"private:draft"),2),
                new AcquisitionDefinition.Drop(true,.25F,List.of("minecraft:zombie"),3),
                new AcquisitionDefinition.Trade(false,"private:profession",2,"private:cost","",1,12,5));
        var info = new AcquisitionSummary("csgobox:test","minecraft:tripwire_hook",draft,draft);
        assertEquals(AcquisitionDefinition.disabled().crafting(),info.boxRules().crafting());
        assertEquals(AcquisitionDefinition.disabled().trade(),info.boxRules().trade());
        assertEquals(draft.drop(),info.boxRules().drop());
        assertEquals(info.boxRules(),info.keyRules());
        assertEquals(AcquisitionDefinition.disabled(),new AcquisitionSummary("csgobox:test","",draft,draft).keyRules());
    }
    @Test void publicSummariesRetainEnabledRuleCountsButRejectInvalidBounds() {
        var grid = new ArrayList<>(Collections.nCopies(9,"")); grid.set(0,"minecraft:stone");
        var valid = new AcquisitionDefinition(new AcquisitionDefinition.Crafting(true,grid,2),null,null);
        assertEquals(valid,new AcquisitionSummary("csgobox:test","",valid,null).boxRules());
        var invalid = new AcquisitionDefinition(new AcquisitionDefinition.Crafting(true,grid,65),null,null);
        assertThrows(IllegalArgumentException.class,()->new AcquisitionSummary("csgobox:test","",invalid,null));
        assertThrows(IllegalArgumentException.class,()->new AcquisitionSummary("bad id","",valid,null));
    }
}
