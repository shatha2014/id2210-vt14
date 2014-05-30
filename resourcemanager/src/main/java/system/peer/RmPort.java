package system.peer;

import common.simulation.BatchRequestResources;
import common.simulation.RequestResource;
import se.sics.kompics.PortType;

public class RmPort extends PortType {{
	positive(RequestResource.class);
	// added for batch requests task
	positive(BatchRequestResources.class);

}}
