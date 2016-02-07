package com.csm.rover.simulator.rover.autoCode;


import com.csm.rover.simulator.objects.DecimalPoint;
import com.csm.rover.simulator.wrapper.Globals;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GORADROGuided extends RoverAutonomousCode {
    private static final Logger LOG = LogManager.getLogger(GORADROGuided.class);

    private static final long serialVersionUID = -8434875463993541766L;

    private static final double ANGLE_ERROR = Math.PI/16.0;
    private static final double WAYPOINT_ERROR = 1.5;
    private static final double RECALC_TIME = 2000; //ms
    enum States {
        CALCULATING, TURNING, TRAVELING, COLLECTING, STUCK, ESCAPING, DONE
    }

    private static final double STALL_RADIUS = 5;
    private static final int STALL_TIME = 240000;
    private static final double RUN_ROTATION = 5*Math.PI/16.0;
    private static final int RUN_TIME = 15000;

    private static final int HISTORIES = 3;
    private static final int SAMPLE_DIRECTIONS = 16;
    private static final double SAMPLE_RADIUS = 2;

    private static final double AVERAGING_RADIUS = 25;
    private static final double AVERAGING_ANGLE = Math.PI / 6.0; //rad
    private static final double AVERAGING_P_STEP = Math.PI / 45.0;
    private static final double AVERAGING_R_STEP = 0.1;

    private boolean started = false;

    private States state = States.CALCULATING;
    private int score = 0;

    double[][] potentials;

    private double travelDirection = 0;

    private Set<Point> visitedScience = new HashSet<Point>();

    private long lastCalcTime = 0;
    private DecimalPoint lastLoc = new DecimalPoint(0, 0);
    private long timeAtPoint = 0;

    private Point[] waypoints;
    private int current_waypoint = 0;
    private static final double[] attitudes = { 1000, 25, 23.333, 0.666, 15 };
    private double dev_tolerance;

    public GORADROGuided(Point[] waypoints, double dev_tolerance) {
        super("GORADRO-G", "GORADRO-G");
        this.waypoints = waypoints;
        this.dev_tolerance = dev_tolerance;
        potentials = new double[HISTORIES][SAMPLE_DIRECTIONS];
        if (dev_tolerance < 1 && dev_tolerance > -1){
            throw new IllegalArgumentException("dev_tolerance must be > 1 or < -1");
        }
    }

    public GORADROGuided(GORADROGuided orig){
        super(orig);
        //TODO copy variables here
        this.started = orig.started;
        this.state = orig.state;
        this.travelDirection = orig.travelDirection;
        this.lastCalcTime = orig.lastCalcTime;
        this.score = orig.score;
        this.waypoints = orig.waypoints.clone();
        this.current_waypoint = orig.current_waypoint;
        this.dev_tolerance = orig.dev_tolerance;
        this.lastLoc = orig.lastLoc;
        this.timeAtPoint = orig.timeAtPoint;
        this.potentials = orig.potentials.clone();
        this.visitedScience = orig.visitedScience;
    }

    @Override
    public String nextCommand(long milliTime, DecimalPoint location, double direction, Map<String, Double> parameters) {
        if (!started){

            started = true;
        }
        switch (state){
            case CALCULATING:
                calculateBasedDirection(location, direction);
                lastCalcTime = milliTime;
                state = States.TURNING;

            case TURNING:
                if (Math.abs(direction-travelDirection) < ANGLE_ERROR){
                    state = States.TRAVELING;
                    return "move";
                }
                else {
                    if (Globals.getInstance().subtractAngles(direction, travelDirection) < 0){
                        return "spin_cw";
                    }
                    else {
                        return "spin_ccw";
                    }
                }

            case TRAVELING:
                if (hasUnvisitedScience(location)){
                    state = States.COLLECTING;
                    return "stop";
                }
                else if (distanceBetween(location, waypoints[current_waypoint]) < WAYPOINT_ERROR){
                    current_waypoint++;
                    if (current_waypoint == waypoints.length){
                        state = States.DONE;
                    }
                }
                else if (milliTime-timeAtPoint > STALL_TIME){
                    if (distanceBetween(location, lastLoc) > STALL_RADIUS){
                        travelDirection += RUN_ROTATION;
                        state = States.STUCK;
                    }
                    else {
                        lastLoc = location.clone();
                        timeAtPoint = milliTime;
                    }
                }
                else if (milliTime-lastCalcTime > RECALC_TIME) {
                    state = States.CALCULATING;
                }
                return "";

            case COLLECTING:
                score += MAP.getTargetValueAt(location);
                Point mapLoc = MAP.getMapSquare(location);
                visitedScience.add(new Point(mapLoc.x/3, mapLoc.y/3));
                state = States.CALCULATING;
                LOG.log(Level.INFO, "Reached a target at {}.  Score at "+score, location.toString());
                return "";

            case STUCK:
                if (Math.abs(direction-travelDirection) < ANGLE_ERROR){
                    state = States.ESCAPING;
                    lastCalcTime = milliTime;
                    return "move";
                }
                else {
                    if (Globals.getInstance().subtractAngles(direction, travelDirection) < 0){
                        return "spin_cw";
                    }
                    else {
                        return "spin_ccw";
                    }
                }

            case ESCAPING:
                if (milliTime-lastCalcTime > RUN_TIME){
                    lastCalcTime = (long)(milliTime - RECALC_TIME);
                    state = States.CALCULATING;
                }
                return "";

            case DONE:
                return "stop";

            default:
                state = States.CALCULATING;
                return "stop";
        }
    }

    private void calculateBasedDirection(DecimalPoint location, double direction) {
        for (int i = 1; i < HISTORIES; i++){
            for (int j = 0; j < SAMPLE_DIRECTIONS; j++){
                potentials[i-1][j] = potentials[i][j];
            }
        }

        double maxPotential = Integer.MIN_VALUE;
        double maxDirection = 0;
        for (int i = 0; i < SAMPLE_DIRECTIONS; i++){
            double theta = 2*Math.PI*i/(double) SAMPLE_DIRECTIONS;
            double deltaX = SAMPLE_RADIUS * Math.cos(theta);
            double deltaY = SAMPLE_RADIUS * Math.sin(theta);
            DecimalPoint examine = location.offset(deltaX, deltaY);

            //if there is a scientific value at the point raise priority
            int science = 0;
            for (int radius = 0; radius < SAMPLE_RADIUS; radius++){
                if (hasUnvisitedScience(location.offset(radius*Math.cos(theta), radius*Math.sin(theta)))){
                    science = MAP.getTargetValueAt(location.offset(radius * Math.cos(theta), radius * Math.sin(theta)));
                    break;
                }
            }

            //if there is a hazard at the point get less excited
            int hazard = MAP.isPointInHazard(examine) ?  MAP.getHazardValueAt(examine) : 0;

            //Calculate the density of science targets in a wedge away from the rover
            double scienceArea = 0;
            for (double radius = SAMPLE_RADIUS; radius <= AVERAGING_RADIUS; radius += AVERAGING_R_STEP){
                for (double phi = -AVERAGING_ANGLE /2.0; phi <= AVERAGING_ANGLE /2.0; phi += AVERAGING_P_STEP){
                    if (hasUnvisitedScience(location.offset(radius*Math.cos(theta+phi), radius*Math.sin(theta+phi)))){
                        scienceArea += SAMPLE_RADIUS /radius;
                    }
                }
            }
            scienceArea /= AVERAGING_RADIUS * AVERAGING_ANGLE;

            //Calculate the density of hazards in a wedge away from the rover
            double hazardArea = 0;
            for (double radius = SAMPLE_RADIUS; radius <= AVERAGING_RADIUS; radius += AVERAGING_R_STEP){
                for (double phi = -AVERAGING_ANGLE /2.0; phi <= AVERAGING_ANGLE /2.0; phi += AVERAGING_P_STEP){
                    if (MAP.isPointInHazard(location.offset(radius * Math.cos(theta + phi), radius * Math.sin(theta + phi)))){
                        hazardArea += SAMPLE_RADIUS /radius;
                    }
                }
            }
            hazardArea /= AVERAGING_RADIUS * AVERAGING_ANGLE;

            //calculate the potential of the point
            potentials[HISTORIES -1][i] =
                    attitudes[0]*science
                    - attitudes[1]*hazard
                    + attitudes[2]*scienceArea
                    - attitudes[3]*hazardArea
                    - attitudes[4]*getDirectionPenalty(location,theta);
            double average = 0;
            for (int j = 0; j < HISTORIES; j++){
                average += potentials[j][i];
            }
            average /= (double) HISTORIES;
            if (average > maxPotential){
                maxPotential = average;
                maxDirection = theta;
            }
            System.out.println(theta+": "+average);
        }
        System.out.println("\n\n\n");
        travelDirection = maxDirection;
    }

    private boolean hasUnvisitedScience(DecimalPoint loc){
        if (MAP.isPointAtTarget(loc)){
            Point mapLoc = MAP.getMapSquare(loc);
            return !visitedScience.contains(new Point(mapLoc.x/3, mapLoc.y/3));
        }
        return false;
    }

    private double getDirectionPenalty(DecimalPoint location, double direction){
        double functional_tolerance = -dev_tolerance < 0 ? 1/dev_tolerance : dev_tolerance;
        double waypoint_direction =
                Math.atan2(waypoints[current_waypoint].getY()-location.getY(),
                        waypoints[current_waypoint].getX()-location.getX());
        waypoint_direction = (waypoint_direction + 2*Math.PI) % (2*Math.PI);
        return Math.pow(functional_tolerance*(direction-waypoint_direction), 4);
    }

    private double distanceBetween(DecimalPoint a, Point b){
        return distanceBetween(a, new DecimalPoint(b.getX(), b.getY()));
    }

    private double distanceBetween(DecimalPoint a, DecimalPoint b){
        return Math.sqrt(Math.pow(a.getX()-b.getX(),2) + Math.pow(a.getY()-b.getY(), 2));
    }

    @Override
    public RoverAutonomousCode clone() {
        return new GORADROGuided(this);
    }
}
