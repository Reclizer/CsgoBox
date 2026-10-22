package com.reclizer.csgobox.config;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/** The on-disk schema. Weight order remains gold, red, pink, purple, blue. */
public record BoxDefinition(String id, String name, String key, float drop, List<Integer> random,
                            List<String> entity, List<String> grade1, List<String> grade2,
                            List<String> grade3, List<String> grade4, List<String> grade5, Boolean luckEnabled,
                            List<Integer> displayOrder, AcquisitionDefinition acquisition, AcquisitionDefinition keyAcquisition,
                            String appearance, String consumption) {
    public static final int RECOMMENDED_REGULAR_LIMIT = 20;
    public BoxDefinition {
        // Missing in legacy JSON: retain the previous luck-sensitive behavior.
        luckEnabled = luckEnabled == null ? true : luckEnabled;
        appearance = appearance == null ? BoxAppearance.CLASSIC.id() : appearance;
        consumption = consumption == null ? key == null || key.isBlank() ? "box" : "both" : consumption;
        random = copy(random);
        entity = copy(entity);
        grade1 = copy(grade1);
        grade2 = copy(grade2);
        grade3 = copy(grade3);
        grade4 = copy(grade4);
        grade5 = copy(grade5);
        if (displayOrder == null) {
            var order = new java.util.ArrayList<Integer>();
            var groups = List.of(grade1, grade2, grade3, grade4);
            for (int g = 1; g <= 4; g++) for (int i = 0; i < groups.get(g - 1).size(); i++) order.add(g);
            displayOrder = List.copyOf(order);
        } else displayOrder = List.copyOf(displayOrder);
        acquisition = acquisition == null ? AcquisitionDefinition.legacy(drop, entity) : acquisition;
        keyAcquisition = keyAcquisition == null ? AcquisitionDefinition.disabled() : keyAcquisition;
        if (key != null && key.isBlank()) { key = ""; keyAcquisition = AcquisitionDefinition.disabled(); }
        // Retain the old JSON fields as mirrors for existing configs/tools; acquisition is authoritative.
        drop = acquisition.drop().chance();
        entity = acquisition.drop().entities();
    }

    public BoxDefinition(String id, String name, String key, float drop, List<Integer> random, List<String> entity,
                         List<String> grade1, List<String> grade2, List<String> grade3, List<String> grade4,
                         List<String> grade5, Boolean luckEnabled, List<Integer> displayOrder,
                         AcquisitionDefinition acquisition, AcquisitionDefinition keyAcquisition, String appearance) {
        this(id, name, key, drop, random, entity, grade1, grade2, grade3, grade4, grade5, luckEnabled,
                displayOrder, acquisition, keyAcquisition, appearance, null);
    }

    public BoxDefinition(String id, String name, String key, float drop, List<Integer> random, List<String> entity,
                         List<String> grade1, List<String> grade2, List<String> grade3, List<String> grade4,
                         List<String> grade5, Boolean luckEnabled, List<Integer> displayOrder,
                         AcquisitionDefinition acquisition, AcquisitionDefinition keyAcquisition) {
        this(id, name, key, drop, random, entity, grade1, grade2, grade3, grade4, grade5, luckEnabled,
                displayOrder, acquisition, keyAcquisition, null);
    }

    public BoxDefinition(String id, String name, String key, float drop, List<Integer> random, List<String> entity,
                         List<String> grade1, List<String> grade2, List<String> grade3,
                         List<String> grade4, List<String> grade5, Boolean luckEnabled) {
        this(id, name, key, drop, random, entity, grade1, grade2, grade3, grade4, grade5, luckEnabled, null, null, null);
    }

    public BoxDefinition(String id, String name, String key, float drop, List<Integer> random, List<String> entity,
                         List<String> grade1, List<String> grade2, List<String> grade3,
                         List<String> grade4, List<String> grade5) {
        this(id, name, key, drop, random, entity, grade1, grade2, grade3, grade4, grade5, true);
    }

    public float effectiveLuck(float playerLuck) { return luckEnabled ? playerLuck : 0; }

    public BoxDefinition withLuckEnabled(boolean enabled) {
        return new BoxDefinition(id, name, key, drop, random, entity, grade1, grade2, grade3, grade4, grade5, enabled,
                displayOrder, acquisition, keyAcquisition, appearance, consumption);
    }

    public BoxDefinition withName(String displayName) {
        return new BoxDefinition(id, displayName, key, drop, random, entity, grade1, grade2, grade3, grade4, grade5, luckEnabled,
                displayOrder, acquisition, keyAcquisition, appearance, consumption);
    }

    public BoxDefinition withAppearance(String value) {
        return new BoxDefinition(id, name, key, drop, random, entity, grade1, grade2, grade3, grade4, grade5, luckEnabled,
                displayOrder, acquisition, keyAcquisition, value, consumption);
    }

    public ConsumptionMode mode() { return ConsumptionMode.parse(consumption); }
    public BoxDefinition withConsumption(String value) {
        return new BoxDefinition(id, name, key, drop, random, entity, grade1, grade2, grade3, grade4, grade5, luckEnabled,
                displayOrder, acquisition, keyAcquisition, appearance, value);
    }

    public static int suggestedCount(int grade) {
        return switch (grade) { case 1 -> 7; case 2 -> 5; case 3 -> 3; case 4 -> 2; default -> 0; };
    }

    public List<RewardEntry> regularRewardsInOrder() {
        int[] offsets = new int[4];
        var result = new java.util.ArrayList<RewardEntry>();
        for (int grade : displayOrder) result.add(new RewardEntry(items(grade).get(offsets[grade - 1]++), grade));
        return List.copyOf(result);
    }

    public List<RewardEntry> previewRewards() {
        return regularRewardsInOrder();
    }

    public static String newId() { return "csgobox:" + java.util.UUID.randomUUID(); }

    public static boolean validId(String id) {
        return id != null && id.length() <= 128 && id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+");
    }

    private static <T> List<T> copy(List<T> list) {
        return list == null ? List.of() : List.copyOf(list);
    }

    public List<String> items(int grade) {
        return switch (grade) {
            case 1 -> grade1;
            case 2 -> grade2;
            case 3 -> grade3;
            case 4 -> grade4;
            case 5 -> grade5;
            default -> throw new IllegalArgumentException("Invalid grade: " + grade);
        };
    }

    public void validate(Predicate<String> validItem) {
        if (!ConsumptionMode.valid(consumption)) throw new IllegalArgumentException("editor.csgobox.bad_consumption");
        if (mode().requiresKey() && (key == null || key.isBlank())) throw new IllegalArgumentException("editor.csgobox.required_item_missing");
        if (!BoxAppearance.valid(appearance)) throw new IllegalArgumentException("editor.csgobox.bad_appearance");
        if (!validId(id)) throw new IllegalArgumentException("Pool requires a stable namespaced id, for example csgobox:weapons_supply");
        if (name == null || name.isBlank() || !name.equals(name.trim()) || name.length() > 128)
            throw new IllegalArgumentException("Box name must contain 1-128 characters without outer spaces");
        if (key == null || key.length() > 8192 || !key.isEmpty() && !validItem.test(key))
            throw new IllegalArgumentException(name + ": invalid key item");
        if (!Float.isFinite(drop) || drop < 0 || drop > 1)
            throw new IllegalArgumentException(name + ": drop must be between 0 and 1");
        if (random.size() != 5 || random.stream().anyMatch(w -> w < 0))
            throw new IllegalArgumentException(name + ": random must contain five nonnegative weights");
        if (entity.stream().anyMatch(id -> !id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")))
            throw new IllegalArgumentException(name + ": invalid entity ID");
        int count = 0;
        int textSize = 0;
        long total = 0;
        for (int grade = 1; grade <= 5; grade++) {
            List<String> items = items(grade);
            count += items.size();
            for (String item : items) {
                textSize += item.length();
                if (item.isBlank() || item.length() > 8192 || !validItem.test(item))
                    throw new IllegalArgumentException(name + ": invalid reward in grade " + grade + ": " + item);
            }
            if (!items.isEmpty()) total += random.get(5 - grade);
        }
        if (count > 256 || textSize > 65536 || total == 0)
            throw new IllegalArgumentException(name + ": requires 1-256 rewards and a positive weight on a nonempty grade");
        int[] shown = new int[4];
        if (displayOrder.size() > 256) throw new IllegalArgumentException("Invalid reward display order");
        for (int grade : displayOrder) {
            if (grade < 1 || grade > 4) throw new IllegalArgumentException("Invalid reward display order");
            shown[grade - 1]++;
        }
        for (int grade = 1; grade <= 4; grade++)
            if (shown[grade - 1] != items(grade).size()) throw new IllegalArgumentException("Invalid reward display order");
        acquisition.validate(validItem);
        keyAcquisition.validate(validItem);
    }

    public BoxDefinition withRewards(List<RewardEntry> rewards) {
        Objects.requireNonNull(rewards);
        var ordered = new java.util.ArrayList<>(rewards.stream().filter(e -> e.grade() != 5).toList());
        var nextOrder = new java.util.ArrayList<Integer>();
        for (var old : regularRewardsInOrder()) {
            if (ordered.remove(old)) nextOrder.add(old.grade());
        }
        ordered.forEach(e -> nextOrder.add(e.grade()));
        return new BoxDefinition(id, name, key, drop, random, entity,
                rewardsAt(rewards, 1), rewardsAt(rewards, 2), rewardsAt(rewards, 3),
                rewardsAt(rewards, 4), rewardsAt(rewards, 5), luckEnabled, nextOrder, acquisition, keyAcquisition, appearance, consumption);
    }

    private static List<String> rewardsAt(List<RewardEntry> entries, int grade) {
        return entries.stream().filter(e -> e.grade() == grade).map(RewardEntry::item).toList();
    }

    public static BoxDefinition create(String id, String name, List<RewardEntry> rewards) {
        return new BoxDefinition(id, name, "csgobox:csgo_key0", 0, List.of(2, 5, 6, 20, 625),
                List.of("minecraft:zombie"), null, null, null, null, null).withRewards(rewards);
    }
}
