package com.csm.rover.simulator.test;

import com.csm.rover.simulator.map.TerrainMap;
import com.csm.rover.simulator.map.display.LandMapPanel;
import com.csm.rover.simulator.objects.util.DecimalPoint;
import com.csm.rover.simulator.objects.util.FloatArrayArrayGrid;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.awt.Point;

import static org.junit.Assert.assertEquals;

public class Incline {

	LandMapPanel map;
	
	@Before
	public void set(){
        TerrainMap terrainMap = new TerrainMap();
        terrainMap.setValues(4, 1, new FloatArrayArrayGrid(new Float[][]{
                { 0f, 0f, 0f, 0f },
                { 1f, 1f, 1f, 1f },
                { 2f, 2f, 2f, 2f },
                { 3f, 3f, 3f, 3f }
        }));
		map = new LandMapPanel(new Dimension(100, 100), null, null, null, terrainMap);
	}
	
	@Test
	public void test() {
		//System.out.println(map.getHeightAt(new DecimalPoint(0, 0)) + " - " + map.getHeightAt(new DecimalPoint(0.01, 0)));
		assertEquals(Math.PI/4., map.heightMap.getIncline(new DecimalPoint(0, 0), 0), 0.07);
		assertEquals(-Math.PI/4., map.heightMap.getIncline(new DecimalPoint(0, 0), Math.PI), 0.07);
		assertEquals(0, map.heightMap.getIncline(new DecimalPoint(0, 0), Math.PI/2.), 0.07);
		assertEquals(0, map.heightMap .getIncline(new DecimalPoint(0, 0), 3*Math.PI/2), 0.07);
	}
	
	@Test
	public void rightSquare(){
		map.heightMap.setValues(6, 3, new FloatArrayArrayGrid(new Float[][]{
				{ 0f, 0.1f, 0.2f, 1f, 1.1f, 1.2f },
				{ 0.01f, 0.11f, 0.21f, 1.01f, 1.11f, 1.21f },
				{ 0.02f, 0.12f, 0.22f, 1.02f, 1.12f, 1.22f },
				{ 4f, 4.1f, 4.2f, 5f, 5.1f, 5.2f },
				{ 4.01f, 4.11f, 4.21f, 5.01f, 5.11f, 5.21f },
				{ 4.02f, 4.12f, 4.22f, 5.02f, 5.12f, 5.22f }
		}));
		assertEquals(new Point(2, 3), map.heightMap.getMapSquare(new DecimalPoint(-.1, -.1)));
		assertEquals(new Point(5, 5), map.heightMap.getMapSquare(new DecimalPoint(0.9, -0.9)));
		assertEquals(new Point(1, 0), map.heightMap.getMapSquare(new DecimalPoint(-0.5, 0.7)));
		assertEquals(new Point(4, 1), map.heightMap.getMapSquare(new DecimalPoint(0.4, 0.6)));
	}

}
