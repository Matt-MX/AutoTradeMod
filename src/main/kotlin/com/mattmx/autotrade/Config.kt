package com.mattmx.autotrade

import me.shedaniel.autoconfig.ConfigData
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment

class Config : ConfigData {

    class VillagerConfig {
        @Comment(
            """
                Should we automatically convert emerald blocks into 
                emeralds so we can keep trading?
                
                (default: true)
            """
        )
        var useEmeraldBlocks = true
    }

}