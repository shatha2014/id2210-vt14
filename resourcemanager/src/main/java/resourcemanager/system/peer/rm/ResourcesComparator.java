// Shatha - Review
package resourcemanager.system.peer.rm;

import java.util.Comparator;

import simulator.snapshot.PeerInfo;

public class ResourcesComparator implements Comparator<PeerInfo> {

	private final PeerInfo peer;

	ResourcesComparator(PeerInfo objPeer) {
		this.peer = objPeer;
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
