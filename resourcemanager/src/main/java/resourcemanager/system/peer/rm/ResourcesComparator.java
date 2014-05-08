// Shatha - Review
package resourcemanager.system.peer.rm;

import se.sics.kompics.address.Address;

public class ResourcesComparator implements Comparable {

	private final int numCpus;
	private final int amountMemInMb;
	private final Address nodeAddress;

	ResourcesComparator(int objNumCpus, int objAmountMemInMb,
			Address objNodeAddress) {
		this.numCpus = objNumCpus;
		this.amountMemInMb = objAmountMemInMb;
		this.nodeAddress = objNodeAddress;
	}

	public int compareTo(Object o) {
		ResourcesComparator obj = (ResourcesComparator) o;

		// Check num of CPUs first then amount of memory
		if (numCpus > obj.numCpus) {
			return 1;
		} 
		
		if (amountMemInMb > obj.amountMemInMb) {
			return 1;
		} else {
			return -1;
		}
	}
	
	public Address getNodeAddress()
	{
		return this.nodeAddress;
    }


}
