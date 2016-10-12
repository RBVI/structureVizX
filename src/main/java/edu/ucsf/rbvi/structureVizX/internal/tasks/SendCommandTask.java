package edu.ucsf.rbvi.structureVizX.internal.tasks;

import java.util.List;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

import edu.ucsf.rbvi.structureVizX.internal.model.CytoUtils;
import edu.ucsf.rbvi.structureVizX.internal.model.StructureManager;

public class SendCommandTask extends AbstractTask implements ObservableTask {

	private StructureManager structureManager;

	@Tunable(description = "Command to send to Chimera")
	public String command = "";

	@Tunable(description = "Selection string to send to Chimera")
	public String select = "";

	public List<String> result;

	public SendCommandTask(StructureManager structureManager) {
		this.structureManager = structureManager;
		this.result = null;
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		taskMonitor.setTitle("Sending Command to Chimera");
		if (select.length() > 0)
			structureManager.getChimeraIO().select(select);
		if (command.length() > 0)
			result = structureManager.getChimeraIO().sendChimeraCommand(command, true);
		// if (result != null) {
		// structureManager.addChimReply(command, result);
		// }
	}

	public Object getResults(Class expectedClass) {
		if (expectedClass.equals(String.class) && result != null) {
			result.add(0, command);
			return CytoUtils.join(result, "\n");
		}
		return result;
	}

	@ProvidesTitle
	public String getTitle() {
		return "Send command to Chimera";
	}
}
