package com.csm.rover.simulator.platforms;

import com.csm.rover.simulator.objects.util.DecimalPoint;

import java.util.Map;

public abstract class PlatformAutonomousCodeModel {

    protected final String platform_type;

    protected PlatformAutonomousCodeModel(String type){
        this.platform_type = type;
    }

    public final String getType(){
        return platform_type;
    }

    public abstract void constructParameters(Map<String, Double> params);

    public abstract String nextCommand(long milliTime, DecimalPoint location,
                                       double direction, Map<String, Double> parameters);

}
