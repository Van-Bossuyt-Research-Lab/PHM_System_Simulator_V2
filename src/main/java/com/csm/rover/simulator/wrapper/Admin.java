package com.csm.rover.simulator.wrapper;

import com.csm.rover.simulator.ui.implementation.PopUp;
import com.csm.rover.simulator.environments.PlatformEnvironment;
import com.csm.rover.simulator.objects.io.PlatformConfig;
import com.csm.rover.simulator.objects.io.RunConfiguration;
import com.csm.rover.simulator.platforms.Platform;
import com.csm.rover.simulator.platforms.rover.RoverObject;
import com.csm.rover.simulator.platforms.satellite.SatelliteObject;
import com.csm.rover.simulator.ui.implementation.UiFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Optional;

public class Admin {
	private static final Logger LOG = LogManager.getLogger(Admin.class);

//	public static Form GUI;
	private Globals GLOBAL;

    //Runtime Variables
    private PlatformEnvironment environment;
    private ArrayList<PlatformConfig> roverCfgs;
    private ArrayList<PlatformConfig> satCfgs;

	public static void main(String[] args) {
        LOG.log(Level.INFO, "Program runtime log for SPEARS simulation software");
        RegistryManager.checkRegistries();
		Admin admin = getInstance();
		if (args.length == 0) {
			LOG.log(Level.INFO, "Starting simulator in GUI mode");
            UiFactory.getApplication().show();
            File config = new File("config.json");
			if (config.exists() &&
                    (new PopUp()).showConfirmDialog("A quick run configuration file has been found.  Would you like to run the simulator from the file?", "Quick Run", PopUp.YES_NO_OPTIONS) == PopUp.YES_OPTION) {
				try {
					admin.beginSimulation(RunConfiguration.fromFile(config));
				}
				catch (Exception e){
					LOG.log(Level.ERROR, "Simulator failed to start", e);
					(new PopUp()).showConfirmDialog(e.getMessage(), "Failed to Start", PopUp.DEFAULT_OPTIONS);
					System.exit(2);
				}
			}
		}
		else {
			LOG.log(Level.INFO, "Stating simulator in Command Line mode");
			File cfgFile = new File(args[0]);
            if (cfgFile.exists() && getFileType(cfgFile).equals("cfg")){
                try {
                    admin.beginSimulation(RunConfiguration.fromFile(cfgFile));
                }
                catch (Exception e){
                    LOG.log(Level.ERROR, "configuration file failed to initiate", e);
					System.exit(2);
                }
            }
            else {
                LOG.log(Level.FATAL, "Expected a valid file path to a JSON configuration file.  Got: \"{}\"", cfgFile.getAbsolutePath());
				System.exit(3);
            }
		}
	}

    private static String getFileType(File file){
        String filename = file.getName();
        return filename.substring(filename.lastIndexOf(".")+1, filename.length());
    }

	//TODO clean up this interface for OCP
	private Admin(){
		GLOBAL = Globals.getInstance();
	}

    private static Optional<Admin> singleton_instance = Optional.empty();
    public static Admin getInstance(){
        if (!singleton_instance.isPresent()){
            singleton_instance = Optional.of(new Admin());
        }
        return singleton_instance.get();
    }

	public void beginSimulation(RunConfiguration config){
        //TODO begin simulation
	}

	private ArrayList<RoverObject> buildRoversFromConfig(ArrayList<PlatformConfig> configs){
		ArrayList<RoverObject> out = new ArrayList<>();
		for (PlatformConfig config : configs){
            out.add(Platform.<RoverObject>buildFromConfiguration(config));
		}
        return out;
	}

	private ArrayList<SatelliteObject> buildSatellitesFromConfig(ArrayList<PlatformConfig> configs){
        ArrayList<SatelliteObject> out = new ArrayList<>();
        for (PlatformConfig config : configs){
            out.add(Platform.<SatelliteObject>buildFromConfiguration(config));
        }
        return out;
	}

}
