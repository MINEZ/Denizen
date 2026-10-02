package com.denizenscript.denizen.paper.utilities;

import com.denizenscript.denizen.Denizen;
import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.EnumSet;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 让生物主动把符合匹配器的实体当作攻击目标的索敌目标，行为仿照原版的“寻找最近的可攻击目标”。
 *
 * <p>经 Paper 的 Mob Goals API 挂到生物的目标选择器上，与原版的索敌目标按优先级并存：
 * 优先级数值更小的目标可以打断它，它也可以打断数值更大的目标。
 * 目标不随实体存盘，实体重新加载后需要重新添加。
 */
public class MatcherTargetGoal implements Goal<Mob> {

    /** 目标选择器每 2 刻评估一次尚未运行的目标，此处再按 1/5 的概率放行，平均每 10 刻搜索一次，与原版一致。 */
    public static final int SEARCH_CHANCE = 5;

    /** 运行期间每评估这么多次（约 10 刻）复核一次视线与匹配器。 */
    public static final int RECHECK_INTERVAL = 5;

    /** 连续这么多次复核都看不见目标（约 60 刻）后放弃，与原版的遗忘时间一致。 */
    public static final int MAX_UNSEEN_RECHECKS = 6;

    public final Mob mob;
    public final String id;
    public final String matcher;
    public final int priority;
    public final double range;
    public final boolean mustSee;
    public final GoalKey<Mob> key;

    private LivingEntity target;
    private int recheckCounter;
    private int unseenRechecks;
    private final Location mobLocation = new Location(null, 0, 0, 0);
    private final Location otherLocation = new Location(null, 0, 0, 0);

    public MatcherTargetGoal(Mob mob, String id, String matcher, int priority, double range, boolean mustSee) {
        this.mob = mob;
        this.id = id;
        this.matcher = matcher;
        this.priority = priority;
        this.range = range;
        this.mustSee = mustSee;
        this.key = GoalKey.of(Mob.class, new NamespacedKey(Denizen.getInstance(), "target_goal_" + id));
    }

    /** 当前正在追击的目标；未运行时为 null。 */
    public LivingEntity getTarget() {
        return target;
    }

    @Override
    public boolean shouldActivate() {
        if (ThreadLocalRandom.current().nextInt(SEARCH_CHANCE) != 0) {
            return false;
        }
        // 生物持有有效目标、却没有任何索敌目标在运行，说明该目标是由脚本或其他插件直接指定的，不去抢。
        // 有索敌目标在运行时无需避让：能走到这里，说明目标选择器已判定本目标的优先级足以打断它。
        LivingEntity current = mob.getTarget();
        if (current != null && current.isValid() && !current.isDead()
                && Bukkit.getMobGoals().getRunningGoals(mob, GoalType.TARGET).isEmpty()) {
            return false;
        }
        target = findTarget();
        return target != null;
    }

    @Override
    public boolean shouldStayActive() {
        if (target == null || !target.equals(mob.getTarget())) {
            return false;
        }
        if (!isAttackable(target) || !isWithinRange(target)) {
            return false;
        }
        if (++recheckCounter >= RECHECK_INTERVAL) {
            recheckCounter = 0;
            if (!matches(target)) {
                return false;
            }
            if (mustSee) {
                if (mob.hasLineOfSight(target)) {
                    unseenRechecks = 0;
                }
                else if (++unseenRechecks > MAX_UNSEEN_RECHECKS) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void start() {
        recheckCounter = 0;
        unseenRechecks = 0;
        mob.setTarget(target);
    }

    @Override
    public void stop() {
        // 只清除自己设置的目标：目标已被别处改掉时不去动它。
        if (target != null && target.equals(mob.getTarget())) {
            mob.setTarget(null);
        }
        target = null;
    }

    @Override
    public GoalKey<Mob> getKey() {
        return key;
    }

    @Override
    public EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.TARGET);
    }

    private LivingEntity findTarget() {
        mob.getLocation(mobLocation);
        LivingEntity best = null;
        double bestDistance = range * range;
        for (Entity entity : mob.getNearbyEntities(range, range, range)) {
            if (!(entity instanceof LivingEntity living) || !isAttackable(living)) {
                continue;
            }
            double distance = entity.getLocation(otherLocation).distanceSquared(mobLocation);
            if (distance > bestDistance) {
                continue;
            }
            // 开销由低到高依次判断：匹配器在前，视线检测放在最后。
            if (!matches(living) || (mustSee && !mob.hasLineOfSight(living))) {
                continue;
            }
            best = living;
            bestDistance = distance;
        }
        return best;
    }

    private boolean matches(LivingEntity entity) {
        return new EntityTag(entity).tryAdvancedMatcher(matcher, CoreUtilities.noDebugContext);
    }

    private boolean isAttackable(LivingEntity entity) {
        if (entity.equals(mob) || !entity.isValid() || entity.isDead() || entity.isInvulnerable()) {
            return false;
        }
        if (entity instanceof Player player) {
            GameMode mode = player.getGameMode();
            return mode == GameMode.SURVIVAL || mode == GameMode.ADVENTURE;
        }
        return true;
    }

    private boolean isWithinRange(LivingEntity entity) {
        if (!entity.getWorld().equals(mob.getWorld())) {
            return false;
        }
        return entity.getLocation(otherLocation).distanceSquared(mob.getLocation(mobLocation)) <= range * range;
    }
}
