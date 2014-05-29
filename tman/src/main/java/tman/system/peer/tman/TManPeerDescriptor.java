package tman.system.peer.tman;

import java.io.Serializable;

import common.peer.AvailableResources;
import se.sics.kompics.address.Address;


public class TManPeerDescriptor implements Comparable<TManPeerDescriptor>, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -604331690558185613L;
	private final Address peerAddress;
	private volatile int numFreeCpus;
	private volatile int freeMemInMbs;

	public TManPeerDescriptor(Address objAddress, int numCpus, int freeMemory) {
		this.peerAddress = objAddress;
		this.numFreeCpus = numCpus;
		this.freeMemInMbs = freeMemory;
	}
	
	public Address getAddress()
	{
		return this.peerAddress;
	}
	
	// added
		public int getNumFreeCpus() {
			return numFreeCpus;
		}

		public int getFreeMemInMbs() {
			return freeMemInMbs;
		}
	
	@Override
	public int compareTo(TManPeerDescriptor that) {
		if (this.getNumFreeCpus() > that.getNumFreeCpus() &&
		    this.getFreeMemInMbs() > that.getFreeMemInMbs())
			return 1;
		if (this.getNumFreeCpus() < that.getNumFreeCpus() &&
			this.getFreeMemInMbs() < that.getFreeMemInMbs())
			return -1;
		return 0;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((peerAddress == null) ? 0 : peerAddress.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TManPeerDescriptor other = (TManPeerDescriptor) obj;
		if (peerAddress == null) {
			if (other.peerAddress != null)
				return false;
		} else if (!peerAddress.equals(other.peerAddress))
			return false;
		return true;
	}


	@Override
	public String toString() {
		return peerAddress + "";
	}

	
}
