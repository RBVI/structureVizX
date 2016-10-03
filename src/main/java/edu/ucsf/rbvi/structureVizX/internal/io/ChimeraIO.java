package edu.ucsf.rbvi.structureVizX.internal.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import org.cytoscape.application.CyUserLog;
import org.apache.log4j.Logger;

import edu.ucsf.rbvi.structureVizX.internal.model.StructureManager;

public class ChimeraIO {

	static private Process chimera;
	static private String chimeraREST;
	public static String restURL = "http://127.0.0.1:1234/v1/commands/structureViz/";
	public static String startModel = "listen start models url \""+restURL+"modelChanged\"";
	public static String startSel = "listen start select url \""+restURL+"selectionChanged\"";

	final Logger logger = Logger.getLogger(CyUserLog.NAME);

	private StructureManager structureManager;

	public ChimeraIO(StructureManager structureManager) {
		this.structureManager = structureManager;
	}

	public void startListening() {
		sendChimeraCommand(startModel, false);
		sendChimeraCommand(startSel, false);
	}

	public void stopListening() {
		sendChimeraCommand("listen stop models; listen stop select", false);
	}

	/**
	 * Select something in Chimera
	 * 
	 * @param command
	 *            the selection command to pass to Chimera
	 */
	public void select(String command) {
		sendChimeraCommand("listen stop select; "+command+"; "+startSel , false);
	}

	public void exitChimera() {
		if (isChimeraLaunched() && chimera != null) {
			sendChimeraCommand("stop really", false);
			try {
				chimera.destroy();
			} catch (Exception ex) {
				// ignore
			}
		}
		structureManager.getChimeraManager().clearOnChimeraExit();
	}

	public boolean isChimeraLaunched() {
		// TODO: [Optional] What is the best way to test if chimera is launched?
		if (chimera != null) {
			return true;
		}
		return false;
	}

	public boolean launchChimera(List<String> chimeraPaths) {
		// Do nothing if Chimera is already launched
		if (isChimeraLaunched()) {
			return true;
		}

		// Try to launch Chimera (eventually using one of the possible paths)
		String error = "Error message: ";
		String workingPath = "";
		// iterate over possible paths for starting Chimera
		for (String chimeraPath : chimeraPaths) {
			File path = new File(chimeraPath);
			if (!path.canExecute()) {
				error += "File '" + path + "' does not exist.\n";
				continue;
			}
			try {
				List<String> args = new ArrayList<String>();
				args.add(chimeraPath);
				args.add("--start");
				args.add("RESTServer");
				ProcessBuilder pb = new ProcessBuilder(args);
				chimera = pb.start();
				BufferedReader reader = new BufferedReader(new InputStreamReader(chimera.getInputStream()));
				String line = reader.readLine();
				// System.out.println("Chimera return line: "+line.trim());
				if (line.startsWith("REST server on host 127.0.0.1 port ")) {
					int port = Integer.parseInt(line.trim().substring(35));
					chimeraREST = "http://127.0.0.1:"+port+"/run?";
					error = "";
				} else {
					error += line.trim();
				}
				workingPath = chimeraPath;
				logger.info("Strarting " + chimeraPath);
				break;
			} catch (Exception e) {
				// Chimera could not be started
				error += e.getMessage();
			}
		}
		// If no error, then Chimera was launched successfully
		if (error.length() == 0) {
			// Initialize the listener threads
			structureManager.setChimeraPathProperty(workingPath);
			// TODO: [Optional] Check Chimera version and show a warning if below 1.8
			// Ask Chimera to give us updates
			startListening();
			return true;
		}

		// Tell the user that Chimera could not be started because of an error
		logger.warn(error);
		return false;
	}

	/**
	 * Send a command to Chimera.
	 * 
	 * @param command
	 *            Command string to be send.
	 * @param reply
	 *            Flag indicating whether the method should return the reply from Chimera or not.
	 * @return List of Strings corresponding to the lines in the Chimera reply or <code>null</code>.
	 */
	public List<String> sendChimeraCommand(String command, boolean reply) {
		if (!isChimeraLaunched()) {
			return null;
		}
		
		List<String> response = new ArrayList<>();
	    RequestConfig globalConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.IGNORE_COOKIES).build();
		CloseableHttpClient client = HttpClients.custom().setDefaultRequestConfig(globalConfig).build();
		String args = URLEncoder.encode(command);
		// System.out.println("Sending: '"+chimeraREST+"command="+args+"'");
		HttpGet request = new HttpGet(chimeraREST+"command="+args);
		CloseableHttpResponse response1 = null;
		try {
			response1 = client.execute(request);
			HttpEntity entity1 = response1.getEntity();
			InputStream entityStream = entity1.getContent();
			if (entity1.getContentLength() == 0)
				return response;
			BufferedReader reader = new BufferedReader(new InputStreamReader(entityStream));
			String inputLine;
			while ((inputLine = reader.readLine()) != null) {
				// System.out.println("From chimera: "+inputLine.trim());
				response.add(inputLine.trim());
			}
			EntityUtils.consume(entity1);

		} catch (Exception e) {
			logger.warn("Unable to execute command: "+command+" ("+e.getMessage()+")");
			structureManager.getChimeraManager().clearOnChimeraExit();
			return null;
		}
		return response;
	}

}
