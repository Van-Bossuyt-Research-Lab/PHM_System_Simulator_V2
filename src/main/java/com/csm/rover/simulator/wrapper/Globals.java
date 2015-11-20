package com.csm.rover.simulator.wrapper;

import com.csm.rover.simulator.control.InterfaceAccess;
import com.csm.rover.simulator.objects.FreeThread;
import com.csm.rover.simulator.objects.SynchronousThread;
import com.csm.rover.simulator.objects.ThreadItem;
import com.csm.rover.simulator.visual.AccelPopUp;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Globals {
	private static final Logger LOG = LogManager.getFormatterLogger(Globals.class);

	public static String versionNumber = "2.3.0";

	private static final double time_accelerant = 10;
	private double timeScale = 1.0;
	public long timeMillis = 0;
	
	private Random rnd = new Random();
	
	private boolean begun = false;
	private Map<String, ThreadItem> threads = new ConcurrentHashMap<String, ThreadItem>();
	private boolean milliDone = false;
	
	private int exitTime = -1;
	private AccelPopUp informer;
	
	private static Globals singleton_instance = null;
	public static Globals getInstance(){
		if (singleton_instance == null){
			singleton_instance = new Globals();
		}
		return singleton_instance;
	}
	
	public void startTime(boolean accel){
		begun = true;
		if (accel){
			timeScale = time_accelerant;
		}
		InterfaceAccess.CODE.clock.start();
		ThreadItem.offset = 0;
		new FreeThread(0, new Runnable(){
			public void run(){
				long end = System.nanoTime() + (long)(1000000 / getTimeScale());
				while (System.nanoTime() < end) {}
				threadCheckIn("milli-clock");
			}
		}, SynchronousThread.FOREVER, "milli-clock", true);
	}
	
	public double getTimeAccelerant() {
		return time_accelerant;
	}

	public double getTimeScale(){
		return timeScale;
	}

	public double addErrorToMeasurement(double measurement, double percentError){ // takes in a measurement an adds error by the given percent
		double deviation = rnd.nextDouble()*(measurement*percentError);
		if (rnd.nextBoolean()){
			measurement += deviation;
		}
		else {
			measurement -= deviation;
		}
		return Math.round(measurement*1000)/1000;
	}
	
	public double addErrorToMeasurement(double measurement, double lowDeviation, double highDeviation){ // takes in a measurement and varies it within measure-lowDev to measure+highDev
		double deviation = rnd.nextDouble()*(lowDeviation + highDeviation) - lowDeviation;
		return Math.round((measurement + deviation)*1000)/1000;
	}
	
	//NOT A TRUE SUBTRACTION: if the second angle is clockwise of the first, returns the negative number of units between, positive if second is to ccw
	public double subtractAngles(double first, double second){
		double one = first - second;
		double two = ((first+Math.PI)%(2*Math.PI)) - ((second+Math.PI)%(2*Math.PI));
		if (Math.abs(one-two) < 0.00001){
			return -1*one;
		}
		if (Math.abs(one) > Math.abs(two)){
			return -1*two;
		}
		else {
			return -1*one;
		}
	}
	
	//NOT A TRUE SUBTRACTION: if the second angle is clockwise of the first, returns the negative number of units between, positive if second is to ccw
	public double subtractAnglesDeg(double first, double second){
		double one = first - second;
		double two = ((first+180)%360) - ((second+180)%360);
		if (Math.abs(one-two) < 0.00001){
			return -1*one;
		}
		if (Math.abs(one) > Math.abs(two)){
			return -1*two;
		}
		else {
			return -1*one;
		}
	}
	
	public void setUpAcceleratedRun(int runtime){
		exitTime = runtime;
		informer = new AccelPopUp(exitTime, (int) (exitTime/time_accelerant/60000));
		new FreeThread(1000, new Runnable(){
			public void run(){
				informer.update((int) timeMillis);
				if (timeMillis >= exitTime){
					//Maybe not working? was an error
					LOG.log(Level.INFO, "Exiting");
					System.exit(0);
				}
			}
		}, FreeThread.FOREVER, "accel-pop-up");
	}
	
	public void registerNewThread(String name, int delay, SynchronousThread thread){
		threads.put(name, new ThreadItem(name, delay, timeMillis, thread));
	}
	
	public void checkOutThread(String name){
		if (!name.contains("delay")){
			System.err.println(name + " out.");
		}
		threads.remove(name);
		threadCheckIn(name);
	}
	
	public void threadCheckIn(String name){
		try {
			threads.get(name).markFinished();
			threads.get(name).advance();
		} catch (NullPointerException e) { };//System.err.println("In thread " + name); e.printStackTrace(); }
		if (name.equals("milli-clock") || milliDone){
			for (Object o : threads.keySet()){
				String key = (String) o;
				if (threads.containsKey(key)){
					if (key.equals("milli-clock")){
						continue;
					}
					if (!threads.get(key).isFinished()){
						milliDone = true;
						threads.get(key).shakeThread();
						return;
					}
				}
			}
			milliDone = false;
			timeMillis++;
			for (Object o : threads.keySet()){
				try {
					String key = (String) o;
					threads.get(key).reset();
					if (threads.get(key).getNext() <= timeMillis){
						threads.get(key).grantPermission();
					}
				}
				catch (NullPointerException e){ e.printStackTrace(); }
			}
		}
	}
	
	public String delayThread(String name, int time){
		try {
			if (threads.get(name) == null){
				throw new Exception();
			}
			String rtnname = name +"-delay";
			ThreadItem.offset = 0;
			registerNewThread(rtnname, time, null);
			threads.get(name).suspend();
			return rtnname;
		}
		catch (Exception e){
			try{
				Thread.sleep((long)(time/getTimeScale()), (int)((time/getTimeScale()-time/getTimeScale())*1000000));
			} catch (Exception ex) {
				LOG.log(Level.ERROR, "Delay thread failed", ex);
			}
			return "pass";
		}
	} 
	
	public void threadDelayComplete(String name){
		checkOutThread(name+"-delay");
		threads.get(name).unSuspend();
	}
	
	public void threadIsRunning(String name){
		try {
			threads.get(name).setRunning(true);
		} catch (NullPointerException e) { e.printStackTrace(); }
	}
	
	public boolean getThreadRunPermission(String name){
		try {
			if (threads.get(name).hasPermission() && begun){
				threads.get(name).revokePermission();
				return true;
			}
		}
		catch (NullPointerException e){
			e.printStackTrace();
		}
		return false;
	}
	
}
