package edu.ucsf.rbvi.structureVizX.internal.model;

import org.cytoscape.model.events.NetworkAboutToBeDestroyedEvent;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedListener;
import org.cytoscape.model.events.NetworkAddedEvent;
import org.cytoscape.model.events.NetworkAddedListener;

public class CyNetworkListener implements NetworkAboutToBeDestroyedListener, NetworkAddedListener {

	private StructureManager structureManager;
	private boolean silence = false;

	public CyNetworkListener(StructureManager structureManager) {
		this.structureManager = structureManager;
	}

	public void handleEvent(NetworkAboutToBeDestroyedEvent e) {
		if (silence) return;
		if (structureManager.getChimeraManager().isChimeraLaunched()) {
			structureManager.deassociate(e.getNetwork());
		}
	}

	public void handleEvent(NetworkAddedEvent e) {
		if (silence) return;
		if (structureManager.getChimeraManager().isChimeraLaunched()) {
			structureManager.associate(e.getNetwork());
		}
	}

	public void silence(boolean silence) { this.silence = silence; }

}
