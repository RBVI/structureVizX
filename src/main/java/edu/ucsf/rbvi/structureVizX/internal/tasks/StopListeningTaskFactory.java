package edu.ucsf.rbvi.structureVizX.internal.tasks;

import org.cytoscape.model.events.NetworkAboutToBeDestroyedListener;
import org.cytoscape.model.events.NetworkAddedListener;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

import edu.ucsf.rbvi.structureVizX.internal.model.CyNetworkListener;
import edu.ucsf.rbvi.structureVizX.internal.model.CySelectionListener;
import edu.ucsf.rbvi.structureVizX.internal.model.StructureManager;

public class StopListeningTaskFactory extends AbstractTaskFactory {

	private StructureManager structureManager;
	private CyNetworkListener netListener;
	private CySelectionListener selListener;

	public StopListeningTaskFactory(StructureManager structureManager, 
	                                CySelectionListener selListener,
																	CyNetworkListener netListener) {
		this.structureManager = structureManager;
		this.selListener = selListener;
		this.netListener = netListener;
	}

	public boolean isReady() {
		if (structureManager.getChimeraManager().isChimeraLaunched())
			return true;
		return false;
	}

	public TaskIterator createTaskIterator() {
		return new TaskIterator(new AbstractTask() {
			public void run(TaskMonitor monitor) {
				System.out.println("Stop listening: setting silence to true");
				selListener.silence(true);
				netListener.silence(true);
			}
		});
	}
}
