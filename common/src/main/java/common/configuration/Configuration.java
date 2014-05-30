package common.configuration;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import se.sics.kompics.address.Address;
import se.sics.kompics.p2p.bootstrap.BootstrapConfiguration;

public class Configuration {

    public static int SNAPSHOT_PERIOD = 1000;
    public static int AVAILABLE_TOPICS = 20;
    public InetAddress ip = null;

    {
        try {
            ip = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
        }
    }
    int webPort = 8080;
    int bootId = Integer.MAX_VALUE;
    int networkPort = 8081;
    Address bootServerAddress = new Address(ip, networkPort, bootId);
    final long seed;
    BootstrapConfiguration bootConfiguration = new BootstrapConfiguration(bootServerAddress, 60000, 4000, 3, 30000, webPort, webPort);
    CyclonConfiguration cyclonConfiguration;
    TManConfiguration tmanConfiguration;
    RmConfiguration searchConfiguration;

    public Configuration(long seed) throws IOException {
        this.seed = seed;
        searchConfiguration = new RmConfiguration(seed);
        tmanConfiguration = new TManConfiguration(seed, 1000, 0.8);
        // Change by Shatha - change cyclon random view size from 10 to 20 to check its effect on batch requests experiments
        // it had a considerable effect ..
        cyclonConfiguration = new CyclonConfiguration(seed, 5, 20, 1000, 500000,
                (long) (Integer.MAX_VALUE - Integer.MIN_VALUE), 20);

        String c = File.createTempFile("bootstrap.", ".conf").getAbsolutePath();
        bootConfiguration.store(c);
        System.setProperty("bootstrap.configuration", c);

        c = File.createTempFile("cyclon.", ".conf").getAbsolutePath();
        cyclonConfiguration.store(c);
        System.setProperty("cyclon.configuration", c);

        c = File.createTempFile("tman.", ".conf").getAbsolutePath();
        tmanConfiguration.store(c);
        System.setProperty("tman.configuration", c);

        c = File.createTempFile("rm.", ".conf").getAbsolutePath();
        searchConfiguration.store(c);
        System.setProperty("rm.configuration", c);
    }
}
