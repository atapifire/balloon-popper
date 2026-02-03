package com.atapifire.balloonpopper;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("balloonpopper")
public interface BalloonPopperConfig extends Config {
    @ConfigItem(keyName = "balloonsSingle", name = "Single Balloons Burst", description = "Total number of single balloons burst", hidden = true)
    default int balloonsSingle() {
        return 0;
    }

    @ConfigItem(keyName = "balloonsDouble", name = "Double Balloons Burst", description = "Total number of double balloons burst", hidden = true)
    default int balloonsDouble() {
        return 0;
    }

    @ConfigItem(keyName = "balloonsTriple", name = "Triple Balloons Burst", description = "Total number of triple balloons burst", hidden = true)
    default int balloonsTriple() {
        return 0;
    }

    @ConfigItem(keyName = "balloonsYellow", name = "", description = "", hidden = true)
    default int balloonsYellow() {
        return 0;
    }

    @ConfigItem(keyName = "balloonsRed", name = "", description = "", hidden = true)
    default int balloonsRed() {
        return 0;
    }

    @ConfigItem(keyName = "balloonsBlue", name = "", description = "", hidden = true)
    default int balloonsBlue() {
        return 0;
    }

    @ConfigItem(keyName = "balloonsGreen", name = "", description = "", hidden = true)
    default int balloonsGreen() {
        return 0;
    }

    @ConfigItem(keyName = "balloonsPurple", name = "", description = "", hidden = true)
    default int balloonsPurple() {
        return 0;
    }

    @ConfigItem(keyName = "balloonsWhite", name = "", description = "", hidden = true)
    default int balloonsWhite() {
        return 0;
    }

    @ConfigItem(keyName = "collectionLogData", name = "", description = "", hidden = true)
    default String collectionLogData() {
        return "{}";
    }
}
