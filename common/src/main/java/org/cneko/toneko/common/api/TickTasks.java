package org.cneko.toneko.common.api;

import org.cneko.toneko.common.mod.util.ITickable;

import java.util.LinkedList;
import java.util.List;

public class TickTasks {
    public static final List<ITickable> CLIENT_TASKS = new LinkedList<>();
    public static final List<ITickable> TASKS = new LinkedList<>();

    public static void add(ITickable task) {
        TASKS.add(task);
    }

    public static void addClient(ITickable task) {
        CLIENT_TASKS.add(task);
    }

    public static void tick() {
        TASKS.forEach(task -> task.addTick(1));
    }

    public static void tickClient() {
        CLIENT_TASKS.forEach(task -> task.addTick(1));
    }

    public static void tick(int ticks) {
        TASKS.forEach(task -> task.addTick(ticks));
    }

    public static void tickClient(int ticks) {
        CLIENT_TASKS.forEach(task -> task.addTick(ticks));
    }

    /**
     * 检查队列中的任务是否有已经无效的，并移除
     */
    public static void checkAndRemove() {
        TASKS.removeIf(ITickable::isRemoved);
    }

    public static void checkAndRemoveClient() {
        CLIENT_TASKS.removeIf(ITickable::isRemoved);
    }

    public static void executeDefault() {
        tick();
        checkAndRemove();
    }

    public static void executeDefaultClient() {
        tickClient();
        checkAndRemoveClient();
    }
}