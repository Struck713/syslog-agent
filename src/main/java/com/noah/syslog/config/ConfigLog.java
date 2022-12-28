package com.noah.syslog.config;

import com.sun.jna.platform.win32.Advapi32Util;

import java.util.List;
import java.util.stream.Collectors;

public class ConfigLog {

    private List<String> types;
    private List<String> levels;

    // non-stored JSON values
    private transient List<Advapi32Util.EventLogType> logLevels;

    public List<String> getTypes() {
        return this.types;
    }

    public List<Advapi32Util.EventLogType> getLevels() {
        if (this.logLevels == null) this.logLevels = this.levels.stream().map(Advapi32Util.EventLogType::valueOf).collect(Collectors.toList());
        return this.logLevels;
    }
}
