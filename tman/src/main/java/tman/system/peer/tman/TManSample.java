package tman.system.peer.tman;

import java.util.ArrayList;


import java.util.List;

import se.sics.kompics.Event;
import se.sics.kompics.address.Address;


public class TManSample extends Event {
	List<TManPeerDescriptor> partners = new ArrayList<TManPeerDescriptor>();


	public TManSample(List<TManPeerDescriptor> partners) {
		this.partners = partners;
	}
        
	public TManSample() {
	}


	public List<TManPeerDescriptor> getSample() {
		return this.partners;
	}
}
