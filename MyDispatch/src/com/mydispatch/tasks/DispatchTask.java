package com.mydispatch.tasks;

import com.mydispatch.config.DispatchConfig;
import eu.darkbot.api.config.ConfigSetting;
import eu.darkbot.api.extensions.Configurable;
import eu.darkbot.api.extensions.Feature;
import eu.darkbot.api.extensions.Task;
import eu.darkbot.api.managers.AuthAPI;
import eu.darkbot.api.managers.BotAPI;
import eu.darkbot.api.managers.DispatchAPI;
import eu.darkbot.api.managers.HeroAPI;
import eu.darkbot.api.managers.StatsAPI;

import java.util.List;

@Feature(name = "MyDispatch", description = "Auto collects and hires dispatches on a timer.")
public class DispatchTask implements Task, Configurable<DispatchConfig> {

    private final AuthAPI authAPI;
    private final HeroAPI heroAPI;
    private final BotAPI botAPI;
    private final StatsAPI statsAPI;
    private final DispatchAPI dispatchAPI;

    private DispatchConfig config;
    private long lastRunMs = 0;

    public DispatchTask(AuthAPI authAPI,
                        HeroAPI heroAPI,
                        BotAPI botAPI,
                        StatsAPI statsAPI,
                        DispatchAPI dispatchAPI) {
        this.authAPI     = authAPI;
        this.heroAPI     = heroAPI;
        this.botAPI      = botAPI;
        this.statsAPI    = statsAPI;
        this.dispatchAPI = dispatchAPI;
    }

    @Override
    public void setConfig(ConfigSetting<DispatchConfig> configSetting) {
        this.config = configSetting.getValue();
    }

    @Override
    public void onTickTask() {
        if (!config.activate) return;
        if (!authAPI.isAuthenticated()) return;
        if (config.onlyOutsideGG && isInGalaxyGate()) return;

        long nowMs = System.currentTimeMillis();
        long intervalMs = config.checkIntervalMins * 60_000L;
        if (nowMs - lastRunMs < intervalMs) return;

        runDispatch();
        lastRunMs = nowMs;
    }

    private void runDispatch() {
        // Hire new dispatches into available (empty) slots
        if (!config.collectOnly) {
            List<? extends DispatchAPI.Retriever> available = dispatchAPI.getAvailableRetrievers();
            if (available != null) {
                for (DispatchAPI.Retriever retriever : available) {
                    try {
                        tryHire(retriever);
                        sleep(500);
                    } catch (Exception ignored) {}
                }
            }
        }

        // Collect finished dispatches (in-progress ones that are done)
        List<? extends DispatchAPI.Retriever> inProgress = dispatchAPI.getInProgressRetrievers();
        if (inProgress != null) {
            for (DispatchAPI.Retriever retriever : inProgress) {
                try {
                    // isAvailable() on an in-progress retriever means it's ready to collect
                    if (retriever.isAvailable()) {
                        DispatchAPI.Cost instant = retriever.getInstantCost();
                        if (instant != null && instant.getAmount() == 0) {
                            // free instant collect
                            retriever.getInstantCost();
                        }
                        sleep(500);
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    private void tryHire(DispatchAPI.Retriever retriever) {
        List<? extends DispatchAPI.Cost> costs = retriever.getCostList();
        if (costs == null || costs.isEmpty()) return;

        // Pick the cheapest cost option (lowest amount)
        DispatchAPI.Cost best = null;
        for (DispatchAPI.Cost cost : costs) {
            if (best == null || cost.getAmount() < best.getAmount()) {
                best = cost;
            }
        }

        if (best != null) {
            // Just select the retriever — DarkBot handles the actual hire UI
            dispatchAPI.overrideSelectedRetriever(retriever);
            sleep(300);
        }
    }

    private boolean isInGalaxyGate() {
        try {
            return heroAPI.getMap() != null && heroAPI.getMap().getId() >= 100;
        } catch (Exception e) {
            return false;
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
