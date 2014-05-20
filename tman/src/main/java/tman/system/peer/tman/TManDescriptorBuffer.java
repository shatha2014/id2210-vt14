package tman.system.peer.tman;

import java.io.Serializable;
import java.util.ArrayList;
import se.sics.kompics.address.Address;

public class TManDescriptorBuffer implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3610942158550525757L;
	private final Address from;
	private final ArrayList<TManPeerDescriptor> descriptors;


	public TManDescriptorBuffer(Address from,
			ArrayList<TManPeerDescriptor> descriptors) {
		super();
		this.from = from;
		this.descriptors = descriptors;
	}


	public Address getFrom() {
		return from;
	}


	public int getSize() {
		return descriptors.size();
	}


	public ArrayList<TManPeerDescriptor> getDescriptors() {
		return descriptors;
	}
	
	public void addDescriptors(ArrayList<TManPeerDescriptor> objAddedDescriptors)
	{
		for(TManPeerDescriptor obj : objAddedDescriptors)
		{
			if(!this.descriptors.contains(obj))
			{
				this.descriptors.add(obj);
			}
		}
	}
}
