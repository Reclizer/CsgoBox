package com.reclizer.csgobox.config;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/** The on-disk schema. Weight order remains gold, red, pink, purple, blue. */
public record BoxDefinition(String name, String key, float drop, List<Integer> random,
                            List<String> entity, List<String> grade1, List<String> grade2,
                            List<String> grade3, List<String> grade4, List<String> grade5, Boolean luckEnabled) {
    public BoxDefinition {
        // Missing in legacy JSON: retain the previous luck-sensitive behavior.
        luckEnabled = luckEnabled == null ? true : luckEnabled;
        random = copy(random);
        entity = copy(entity);
        grade1 = copy(grade1);
        grade2 = copy(grade2);
        grade3 = copy(grade3);
        grade4 = copy(grade4);
        grade5 = copy(grade5);
    }

    public BoxDefinition(String name, String key, float drop, List<Integer> random, List<String> entity,
                         List<String> grade1, List<String> grade2, List<String> grade3,
                         List<String> grade4, List<String> grade5) {
        this(name, key, drop, random, entity, grade1, grade2, grade3, grade4, grade5, true);
    }

    public float effectiveLuck(float playerLuck) { return luckEnabled ? playerLuck : 0; }

    public BoxDefinition withLuckEnabled(boolean enabled) {
        return new BoxDefinition(name, key, drop, random, entity, grade1, grade2, grade3, grade4, grade5, enabled);
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
        if (name == null || name.isBlank() || !name.equals(name.trim()) || name.length() > 128)
            throw new IllegalArgumentException("Box name must contain 1-128 characters without outer spaces");
        if (key == null || !key.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") || !validItem.test(key))
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
    }

    public BoxDefinition withRewards(List<RewardEntry> rewards) {
        Objects.requireNonNull(rewards);
        return new BoxDefinition(name, key, drop, random, entity,
                rewardsAt(rewards, 1), rewardsAt(rewards, 2), rewardsAt(rewards, 3),
                rewardsAt(rewards, 4), rewardsAt(rewards, 5), luckEnabled);
    }

    private static List<String> rewardsAt(List<RewardEntry> entries, int grade) {
        return entries.stream().filter(e -> e.grade() == grade).map(RewardEntry::item).toList();
    }

    public static BoxDefinition create(String name, List<RewardEntry> rewards) {
        return new BoxDefinition(name, "csgobox:csgo_key0", 0, List.of(2, 5, 6, 20, 625),
                List.of("minecraft:zombie"), null, null, null, null, null).withRewards(rewards);
    }
}
