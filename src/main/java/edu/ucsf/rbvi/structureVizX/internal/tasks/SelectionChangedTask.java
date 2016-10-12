package edu.ucsf.rbvi.structureVizX.internal.tasks;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

import edu.ucsf.rbvi.structureVizX.internal.model.StructureManager;

public class SelectionChangedTask extends AbstractTask {
	private StructureManager structureManager;
	static boolean selectionUpdaterRunning = false;

	@Tunable(description="Data from Chimera")
	public String chimerax_notification = "";

	public SelectionChangedTask(StructureManager structureManager) {
		this.structureManager = structureManager;
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		(new SelectionUpdater()).start();
	}

  /**
   * Selection updater thread
   */
  class SelectionUpdater extends Thread {

    public SelectionUpdater() {
    }

    public void run() {
      if (SelectionChangedTask.selectionUpdaterRunning) return;
      SelectionChangedTask.selectionUpdaterRunning = true;
      try {
        structureManager.chimeraSelectionChanged();
      } catch (Exception e) {
        // logger.warn("Could not update selection", e);
      }
      SelectionChangedTask.selectionUpdaterRunning = false;
    }
  }


}
