package simulator.snapshot;

import common.peer.AvailableResources;
import java.util.ArrayList;
import se.sics.kompics.address.Address;

public class PeerInfo {

    private ArrayList<Address> neighbours;
    private final AvailableResources availableResources;
    // shatha added
    private Address self;

    public PeerInfo(AvailableResources availableResources) {
        this.neighbours = new ArrayList<Address>();
        this.availableResources = availableResources;
    }

    public void setAddress(Address selfAddress)
    {
    	this.self = selfAddress;
    }
    
    public Address getAddress()
    {
    	return this.self;
    }
    
    public int getNumFreeCpus() {
        return availableResources.getNumFreeCpus();
    }
    
    public int getFreeMemInMbs() {
        return availableResources.getFreeMemInMbs();
    }
    
    public synchronized void setNeighbours(ArrayList<Address> partners) {
        this.neighbours = partners;
    }

    public synchronized ArrayList<Address> getNeighbours() {
        return new ArrayList<Address>(neighbours);
    }
}