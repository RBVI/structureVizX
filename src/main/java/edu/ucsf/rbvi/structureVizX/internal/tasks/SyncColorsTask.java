package edu.ucsf.rbvi.structureVizX.internal.tasks;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;

import edu.ucsf.rbvi.structureVizX.internal.model.RINManager;
import edu.ucsf.rbvi.structureVizX.internal.model.StructureManager;

public class SyncColorsTask extends AbstractTask {

	private StructureManager structureManager;
	private RINManager rinManager;
	private CyNetworkView networkView;

	enum Direction {
		FROM_CYTOSCAPE("From current network view to associated ChimeraX Models"),
		FROM_CHIMERA("From ChimeraX models to current network view");
	
		public String str;
		Direction(String str) {
			this.str = str;
		}

		public String toString() { return str; }
	}


	@Tunable(description = "Direction to synchronize colors", gravity = 1.0, context="gui")
	public ListSingleSelection<Direction> direction = 
		new ListSingleSelection<>(Direction.FROM_CYTOSCAPE, Direction.FROM_CHIMERA);

	@Tunable(description = "Apply colors from current network view to associated Chimera models", context="nogui")
	public boolean cytoscapeToChimera = false;

	@Tunable(description = "Apply colors from associated Chimera models to current network view", context="nogui")
	public boolean chimeraToCytoscape = false;

	public SyncColorsTask(StructureManager structureManager, CyNetworkView networkView) {
		this.structureManager = structureManager;
		this.rinManager = structureManager.getRINManager();
		this.networkView = networkView;
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		taskMonitor.setTitle("Synchronizing Colors with Chimera");
		if (networkView == null) {
			CyNetworkView current = ((CyApplicationManager) structureManager
					.getService(CyApplicationManager.class)).getCurrentNetworkView();
			if (current == null) {
				taskMonitor.setStatusMessage("No network view found, aborting...");
				return;
			} else {
				networkView = current;
			}
		}
		if (chimeraToCytoscape || direction.getSelectedValue() == Direction.FROM_CHIMERA) {
			taskMonitor
					.setStatusMessage("Applying colors from associated Chimera models to current network view ...");
			rinManager.syncChimToCyColors(networkView);
		} else {
			taskMonitor
					.setStatusMessage("Applying colors from current network view to associated Chimera models ...");
			rinManager.syncCyToChimColors(networkView);
		}
	}

}
