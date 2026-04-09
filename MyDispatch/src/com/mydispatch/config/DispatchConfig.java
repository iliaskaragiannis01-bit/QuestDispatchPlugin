package com.mydispatch.config;

import eu.darkbot.api.config.annotations.Configuration;
import eu.darkbot.api.config.annotations.Number;
import eu.darkbot.api.config.annotations.Option;

@Configuration("mydispatch.config")
public class DispatchConfig {

    @Option("dispatch.activate")
    public boolean activate = true;

    @Option("dispatch.only_outside_gg")
    public boolean onlyOutsideGG = false;

    @Option("dispatch.check_interval_mins")
    @Number(min = 1, max = 120, step = 1)
    public int checkIntervalMins = 5;

    @Option("dispatch.collect_only")
    public boolean collectOnly = false;
}
