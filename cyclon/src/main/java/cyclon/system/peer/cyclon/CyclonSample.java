package cyclon.system.peer.cyclon;

import java.util.ArrayList;


import se.sics.kompics.Event;
import se.sics.kompics.address.Address;


public class CyclonSample extends Event {
	// Modified replacing Address with PeerDescriptor
	//ArrayList<Address> nodes = new ArrayList<Address>();
	ArrayList<PeerDescriptor> nodes = new ArrayList<PeerDescriptor>();


	public CyclonSample(ArrayList<PeerDescriptor> nodes) {
		this.nodes = nodes;
	}
        
	public CyclonSample() {
	}


	public ArrayList<PeerDescriptor> getSample() {
		return this.nodes;
	}
}
