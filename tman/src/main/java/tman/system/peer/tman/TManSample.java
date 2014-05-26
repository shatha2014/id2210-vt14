package tman.system.peer.tman;

import java.util.ArrayList;


import se.sics.kompics.Event;
import se.sics.kompics.address.Address;


public class TManSample extends Event {
	// changed
	//ArrayList<Address> partners = new ArrayList<Address>();
	ArrayList<TManPeerDescriptor> partners = new ArrayList<TManPeerDescriptor>();


	public TManSample(ArrayList<TManPeerDescriptor> partners) {
		this.partners = partners;
	}
        
	public TManSample() {
	}


	public ArrayList<TManPeerDescriptor> getSample() {
		return this.partners;
	}
}
