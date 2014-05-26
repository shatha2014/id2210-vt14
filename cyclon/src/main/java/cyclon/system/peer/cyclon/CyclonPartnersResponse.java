package cyclon.system.peer.cyclon;

import java.util.ArrayList;


import se.sics.kompics.Event;
import se.sics.kompics.address.Address;


public class CyclonPartnersResponse extends Event {
	// modified replacing Address with PeerDescriptor
	//ArrayList<Address> partners = new ArrayList<Address>();
	ArrayList<PeerDescriptor> partners = new ArrayList<PeerDescriptor>();


	public CyclonPartnersResponse(ArrayList<PeerDescriptor> partners) {
		this.partners = partners;
	}
        
	public CyclonPartnersResponse() {
	}


	public ArrayList<PeerDescriptor> getPartners() {
		return this.partners;
	}
}
