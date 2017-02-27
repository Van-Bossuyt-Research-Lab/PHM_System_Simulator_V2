package com.csm.rover.simulator.environments;

import com.csm.rover.simulator.environments.annotations.Environment;
import com.csm.rover.simulator.objects.util.ParamMap;
import com.csm.rover.simulator.test.objects.maps.UnknownCaveMap;
import com.csm.rover.simulator.test.objects.maps.UnlabeledSkyMap;
import com.csm.rover.simulator.test.objects.populators.UnknownPop;
import com.csm.rover.simulator.test.objects.populators.UnlabeledKelpPop;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class EnvironmentPopulatorTest {

    private double TOLERANCE = 0.000001;

    private EnvironmentPopulator pop;

    @Before
    public void setup(){
        pop = new UnknownPop();
        pop.build(new UnknownCaveMap(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRejectsBadType(){;
        pop.build(new UnlabeledSkyMap(), ParamMap.emptyParamMap());
    }

    @Test
    public void testCallsDoBuild(){
        pop = spy(new UnknownPop());
        EnvironmentMap map = new UnknownCaveMap();
        Map<String, Double> params = ParamMap.newParamMap().addParameter("hi", -1).build();
        pop.build(map, params);
        verify(pop).doBuild(map, params);
    }

    @Test
    public void testGetValue(){
        assertEquals(1, pop.getValue(1, 1), TOLERANCE);
    }

    @Test
    public void testUsesFloor(){
        assertEquals(1, pop.getValue(1.7, 1.2), TOLERANCE);
    }

    @Test
    public void testDefaultOnOutOfBounds(){
        assertEquals(-1, pop.getValue(10, 10), TOLERANCE);

        assertEquals(-1, pop.getValue(-10, -10), TOLERANCE);
    }

    @Test
    public void testDefaultOnUnfilled(){
        assertEquals(-1, pop.getValue(0, 2), TOLERANCE);
    }

    @Test
    public void testDefaultOnWrongCoordinates(){
        assertEquals(-1, pop.getValue(1), TOLERANCE);

        assertEquals(-1, pop.getValue(1, 4, 6), TOLERANCE);
    }

    @Test
    public void testDefaultOnNotBuilt(){
        EnvironmentPopulator pop = new UnknownPop();
        assertEquals(-1, pop.getValue(1, 1), TOLERANCE);
    }

}