package main;

import simulator.core.DataCenterSimulationMain;
import simulator.core.Statistics;
import common.configuration.Configuration;
import common.simulation.scenarios.Scenario;
import common.simulation.scenarios.Scenario1;

public class Main {

    public static void main(String[] args) throws Throwable {
        // TODO - change the random seed, have the user pass it in.
        long seed = System.currentTimeMillis();
        Configuration configuration = new Configuration(seed);

        Scenario scenario = new Scenario1();
        scenario.setSeed(seed);
        scenario.getScenario().simulate(DataCenterSimulationMain.class);
        
//        Statistics singleResourceInstance = Statistics.getSingleResourceInstance();
//		System.out.println("Average delay: " + singleResourceInstance.getAvg());
//		System.out.println("99th percentile delay: " + singleResourceInstance.getNinetyNinth());
		
    }
}
