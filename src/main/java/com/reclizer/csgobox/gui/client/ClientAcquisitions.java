package com.reclizer.csgobox.gui.client;

import com.reclizer.csgobox.config.AcquisitionSummary;
import java.util.*;

/** Server-backed client cache. A JEI listener is installed only when that optional plugin loads. */
public final class ClientAcquisitions {
    private static final Map<String, AcquisitionSummary> summaries = new LinkedHashMap<>();
    private static Set<String> expected = Set.of();
    private static Runnable listener = () -> {};
    private ClientAcquisitions() {}
    public static Collection<AcquisitionSummary> entries() { return List.copyOf(summaries.values()); }
    public static void setListener(Runnable next) { listener = next; }
    public static void reset(Collection<String> ids) {
        expected = Set.copyOf(ids); summaries.clear(); listener.run();
    }
    public static void accept(AcquisitionSummary summary) {
        if (!expected.contains(summary.id())) return;
        summaries.put(summary.id(), summary);
        if (summaries.size() == expected.size()) listener.run();
    }
}
