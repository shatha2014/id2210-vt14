// Shatha - Review
package resourcemanager.system.peer.rm;

import java.util.Comparator;

import simulator.snapshot.PeerInfo;
import tman.system.peer.tman.TManPeerDescriptor;

public class ResourcesComparator implements Comparator<PeerInfo> {

	private final PeerInfo peer;
	TManPeerDescriptor objTManPeerDescriptor;

	ResourcesComparator(PeerInfo objPeer) {
		this.peer = objPeer;
	}

	private int compare_combinations(TManPeerDescriptor o1,TManPeerDescriptor o2) {
		int value_o1 = 0;
		int value_o2 = 0;
		int value_self = 0;
		
		int numFreeCPUs_o1 = o1.getNumFreeCpus();
		int numFreeCPUs_o2 = o2.getNumFreeCpus();
		int numAmountMemory_o1 = o1.getFreeMemInMbs();
		int numAmountMemory_o2 = o2.getFreeMemInMbs();
		int numFreeCPUs_self = objTManPeerDescriptor.getNumFreeCpus();
		int numAmountMemory_self = objTManPeerDescriptor.getFreeMemInMbs();
		
		// defensive action
		if(numFreeCPUs_o1 < 0 || numFreeCPUs_o2 < 0 || numAmountMemory_o1 <0 ||
		   numAmountMemory_o2 < 0 || 	numFreeCPUs_self < 0 || numAmountMemory_self < 0)
					return -1;

		value_o1 += numFreeCPUs_o1 - 4;
		value_o1 += (numAmountMemory_o1 - 6000) / 1000;

		value_o2 += numFreeCPUs_o2 - 4;
		value_o2 += (numAmountMemory_o2 - 6000) / 1000;

		value_self += numFreeCPUs_self - 4;
		value_self += (numAmountMemory_self - 6000) / 1000;

		assert (value_o1 == value_o2);
		if (value_o1 < value_self && value_o2 > value_self)
			return 1;
		if (value_o2 < value_self && value_o1 > value_self)
			return -1;
		else if (Math.abs(value_o1 - value_self) < Math.abs(value_o2
				- value_self))
			return -1;

		return -1;
	}

	
	public int compare(PeerInfo peer1, PeerInfo peer2) {
		int numFreeCPUs_o1 = peer1.getNumFreeCpus();
		int numFreeCPUs_o2 = peer2.getNumFreeCpus();
		int numFreeCPUs_self = peer.getNumFreeCpus();

		assert (numFreeCPUs_o1 == numFreeCPUs_o2);
		if (numFreeCPUs_o1 < numFreeCPUs_self
				&& numFreeCPUs_o2 > numFreeCPUs_self) {
			return 1;
		} else if (numFreeCPUs_o2 < numFreeCPUs_self
				&& numFreeCPUs_o1 > numFreeCPUs_self) {
			return -1;
		} else if (Math.abs(numFreeCPUs_o1 - numFreeCPUs_self) < Math
				.abs(numFreeCPUs_o2 - numFreeCPUs_self)) {
			return -1;
		}
		return 1;
	}

}
