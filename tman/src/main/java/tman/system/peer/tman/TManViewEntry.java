package tman.system.peer.tman;

import java.util.HashSet;

import se.sics.kompics.address.Address;

public class TManViewEntry {
	private final TManPeerDescriptor descriptor;
	private final long addedAt;
	private long sentAt;
	private HashSet<Address> sentTo;


	public TManViewEntry(TManPeerDescriptor descriptor) {
		this.descriptor = descriptor;
		this.addedAt = System.currentTimeMillis();
		this.sentAt = 0;
		this.sentTo = null;
	}


	public boolean isEmpty() {
		return descriptor == null;
	}


	public void sentTo(Address peer) {
		if (sentTo == null) {
			sentTo = new HashSet<Address>();
		}
		sentTo.add(peer);
		sentAt = System.currentTimeMillis();
	}


	public TManPeerDescriptor getDescriptor() {
		return descriptor;
	}


	public long getAddedAt() {
		return addedAt;
	}


	public long getSentAt() {
		return sentAt;
	}


	public boolean wasSentTo(Address peer) {
		return sentTo == null ? false : sentTo.contains(peer);
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((descriptor == null) ? 0 : descriptor.hashCode());
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
		TManViewEntry other = (TManViewEntry) obj;
		if (descriptor == null) {
			if (other.descriptor != null)
				return false;
		} else if (!descriptor.equals(other.descriptor))
			return false;
		return true;
	}
}
