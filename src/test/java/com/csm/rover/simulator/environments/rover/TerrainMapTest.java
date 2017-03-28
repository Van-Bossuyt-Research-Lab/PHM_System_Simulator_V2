package com.csm.rover.simulator.environments.rover;

import com.csm.rover.simulator.objects.util.ArrayGrid;
import com.csm.rover.simulator.objects.util.DecimalPoint;
import com.csm.rover.simulator.objects.util.FloatArrayArrayGrid;
import com.csm.rover.simulator.test.objects.maps.LandMap;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class TerrainMapTest {

    private static final double TOLERANCE = 0.0000001;

    @Test(expected = IllegalArgumentException.class)
    public void testCheckSizeOnMake_Array(){
        new TerrainMap(3, 2, new Float[][] {
                new Float[] {4f, 5f},
                new Float[] {1.2f, 0.7f}
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckSizeOnMake_Grid(){
        ArrayGrid<Float> grid = new FloatArrayArrayGrid();
        grid.fillToSize(5, 7);
        new TerrainMap(2, 3, grid);
    }

    @Test
    public void testFindMin(){
        assertEquals(-2f, getAMap().getMinValue(), TOLERANCE);
    }

    @Test
    public void testFindMax(){
        assertEquals(6f, getAMap().getMaxValue(), TOLERANCE);
    }

    @Test
    public void testGetHeightEasy(){
        assertEquals(0f, getAMap().getHeightAt(new DecimalPoint(0, 0)), TOLERANCE);
    }

    @Test
    public void testGetHeight(){
        assertEquals(2.35f, getAMap().getHeightAt(new DecimalPoint(0.75, 0.75)), TOLERANCE);
    }

    @Test
    public void testEquals(){
        assertEquals(getAMap(), getAMap());
    }

    @Test
    public void testNotEquals(){
        assertNotEquals(getAMap(), new TerrainMap(2, 2, new Float[][]{
                new Float[] {0f, 1f, 3f, 5f, 6f},
                new Float[] {0.5f, 1.5f, 1f, 3f, 4f},
                new Float[] {1f, 0.75f, 0f, 1.2f, 3f},
                new Float[] {2.3f, 1.3f, -2f, 0.5f, 3.5f},
                new Float[] {3f, 2f, 0.4f, 1.4f, 4f}
        }));
    }

    @Test
    public void testNotEqualsSize(){
        assertNotEquals(getAMap(), new TerrainMap(1, 1, new Float[][]{
                new Float[] {0f, 1f},
                new Float[] {0.5f, 1.5f}
        }));
    }

    @Test
    public void testReallyNotEquals(){
        assertNotEquals(getAMap(), new LandMap());
    }

    @Test
    public void testGetRaw(){
        ArrayGrid<Float> grid = new FloatArrayArrayGrid(new Float[][]{
                new Float[] {0f, 1f, 3f, 5f, 6f},
                new Float[] {0.5f, 1.5f, 1f, 2.5f, 4f},
                new Float[] {1f, 0.75f, 0f, 1.2f, 3f},
                new Float[] {2.3f, 1.3f, -2f, 0.5f, 3.5f},
                new Float[] {3f, 2f, 0.4f, 1.4f, 4f}
        });
        assertEquals(grid, getAMap().rawValues());
    }

    private TerrainMap getAMap(){
        return new TerrainMap(2, 2, new Float[][]{
                new Float[] {0f, 1f, 3f, 5f, 6f},
                new Float[] {0.5f, 1.5f, 1f, 2.5f, 4f},
                new Float[] {1f, 0.75f, 0f, 1.2f, 3f},
                new Float[] {2.3f, 1.3f, -2f, 0.5f, 3.5f},
                new Float[] {3f, 2f, 0.4f, 1.4f, 4f}
        });
    }

}