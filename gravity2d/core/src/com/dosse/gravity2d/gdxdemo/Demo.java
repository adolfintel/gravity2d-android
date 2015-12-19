/*
 * GNU LGPLv3
 */
package com.dosse.gravity2d.gdxdemo;

import com.dosse.gravity2d.Point;
import com.dosse.gravity2d.Simulation;

/**
 *
 * @author Federico
 */
public class Demo extends Simulation {

    public void createAt(float x, float y, float initSpeedX, float initSpeedY, float mass, float density) {
        add(new Point(x, y, initSpeedX, initSpeedY, mass, density));
    }
    
    public float[][] getPlotData() {
        boolean tryAgain = false;
        do {
            try {
                final float[][] plotData = new float[points.size()][4];
                int i = 0;
                for (Point p : points) {
                    plotData[i][0] = (float) p.getX();
                    plotData[i][1] = (float) p.getY();
                    plotData[i][2] = (float) p.getRadius();
                    plotData[i][3] = (float) p.getDensity();
                    i++;
                }
                return plotData;
            } catch (Throwable e) {
                //I did not synchronize access to the points list for performance reasons. In the rare event that a sync problem arises, ignore it
                tryAgain = true;
            }
        } while (tryAgain);
        return null; //unreachable, but java complained
    }

}
