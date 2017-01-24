package edu.ucsf.rbvi.structureVizX.internal.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
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
import edu.ucsf.rbvi.structureVizX.internal.model.AtomSpec;

public class ChimeraIO {

	static private Process chimera;
	static private boolean chimeraLaunched = false;
	static private String chimeraREST = null;
	public static String restURL = "http://127.0.0.1:12345/v1/commands/structureViz/";
	public static String startModel = "listinfo notify start models structureVizX url \""+restURL+"modelChanged\"";
	public static String startSel = "listinfo notify start select structureVizX url \""+restURL+"selectionChanged\"";
	public static String suspendModel = "listinfo notify suspend models structureVizX";
	public static String suspendSel = "listinfo notify suspend select structureVizX";
	public static String resumeModel = "wait 1;listinfo notify resume models structureVizX";
	public static String resumeSel = "wait 1;listinfo notify resume select structureVizX";

	public static String stopSel = "listinfo notify stop select structureVizX";
	public static String stopModel = "listinfo notify stop models structureVizX";

	private StructureManager structureManager;

	public ChimeraIO(StructureManager structureManager) {
		this.structureManager = structureManager;
	}

	public void startListening() {
		sendChimeraCommand(startModel, false);
		sendChimeraCommand(startSel, false);
	}

	public void stopListening() {
		sendChimeraCommand(stopSel+";"+stopModel+";wait 1", false);
	}

	/**
	 * Select something in Chimera
	 * 
	 * @param command
	 *            the selection command to pass to Chimera
	 */
	public void select(String command) {
		sendChimeraCommand(suspendSel, false);
		sendChimeraCommand(command, false);
		sendChimeraCommand(resumeSel , false);
	}

	/**
	 * Select something in Chimera
	 * 
	 * @param command
	 *            the selection command to pass to Chimera
	 */
	public void select(String sel, List<AtomSpec> specs) {
		String command = sel;
		if (specs != null && specs.size() > 0) {
			// System.out.println("Sorting the specs");
			Collections.sort(specs);
			/*
			System.out.println("Sorted specs: ");
			for (AtomSpec spec: specs) 	{
				System.out.println(spec.toSpec());
			}
			*/
			command += " "+AtomSpec.collapseSpecs(specs);
		}
		select(command);
	}

	public void exitChimera() {
		if (isChimeraLaunched()) {
			sendChimeraCommand("exit", false);
			try {
				if (chimera != null)
					chimera.destroy();
			} catch (Exception ex) {
				// ignore
			}
		}
		structureManager.getChimeraManager().clearOnChimeraExit();
		chimera = null;
		chimeraLaunched = false;
	}

	public boolean isChimeraLaunched() {
		// TODO: [Optional] What is the best way to test if chimera is launched?
		if (chimeraLaunched) {
			return true;
		}
		return false;
	}

	public boolean initChimera(int chimeraPort) {
		if (isChimeraLaunched()) {
			// Assume this is a re-init
			exitChimera();
		}
		chimeraREST = "http://127.0.0.1:"+chimeraPort+"/run?";

		// See if we can talk
		List<String> reply = sendChimeraCommandInternal("windowsize", false);
		if (reply != null) {
			chimeraLaunched = true;
			return true;
		}
		structureManager.logError("Unable to establish communication with ChimeraX");
		chimeraLaunched = false;
		return false;
	}

	public boolean launchChimera(List<String> chimeraPaths) {
		int port = 0;
		// Do nothing if Chimera is already launched
		if (isChimeraLaunched()) {
			return true;
		}

		// Try to launch Chimera (eventually using one of the possible paths)
		String error = "Error message: ";
		String warnings = "";
		String workingPath = "";
		// iterate over possible paths for starting Chimera
		for (String chimeraPath : chimeraPaths) {
			// System.out.println("Looking at path: "+chimeraPath);
			File path = new File(chimeraPath);
			if (!path.canExecute()) {
				// System.out.println("Path '"+chimeraPath+"' does not exist");
				error += "File '" + path + "' does not exist.\n";
				continue;
			}
			try {
				// System.out.println("Trying path: "+chimeraPath);
				List<String> args = new ArrayList<String>();
				args.add(chimeraPath);
				args.add("--cmd");
				args.add("remotecontrol rest start");
				ProcessBuilder pb = new ProcessBuilder(args);
				chimera = pb.start();
				BufferedReader reader = new BufferedReader(new InputStreamReader(chimera.getInputStream()));
				String line;
			 	while ((line	= reader.readLine()) != null) {
					if (line.startsWith("REST server started on host 127.0.0.1 port ")) {
						int offset = line.indexOf("port ")+5;
						port = Integer.parseInt(line.trim().substring(offset));
						error = "";
						break;
					} else if (line.startsWith("warning:") || line.startsWith("WARNING")) {
						// Chimera warning messages
						structureManager.logWarning("From ChimeraX "+line.trim());
					} else if (line.contains("chromium")) {
						// Chromium errors -- ignore
						continue;
					} else {
						error += line;
						break;
					}
				}
				workingPath = chimeraPath;
				// System.out.println("chimeraREST = "+chimeraREST);
				structureManager.logInfo("Starting " + chimeraPath + " with REST port "+port);
				break;
			} catch (Exception e) {
				// Chimera could not be started
				structureManager.logError("Unable to launch ChimeraX: "+e.getMessage());
				error += e.getMessage();
			}
		}
		// If no error, then Chimera was launched successfully
		if (error.length() == 0) {
			if (initChimera(port)) {
				// Initialize the listener threads
				structureManager.setChimeraPathProperty(workingPath);
				// TODO: [Optional] Check Chimera version and show a warning if below 1.8
				// Ask Chimera to give us updates
				startListening();
				return true;
			} else {
				return false;
			}
		}

		// Tell the user that Chimera could not be started because of an error
		structureManager.logWarning(error);
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

		return sendChimeraCommandInternal(command, reply);
	}

	static int CONNECT_TIMEOUT = 1; // 1 second
	static int SOCKET_TIMEOUT = 30; // 30 seconds
	private List<String> sendChimeraCommandInternal(String command, boolean reply) {
		
		List<String> response = new ArrayList<>();
		RequestConfig globalConfig = RequestConfig.custom()
						.setCookieSpec(CookieSpecs.IGNORE_COOKIES)
						.setConnectTimeout(CONNECT_TIMEOUT*1000)
						.setSocketTimeout(SOCKET_TIMEOUT*1000).build();
		CloseableHttpClient client = HttpClients.custom().setDefaultRequestConfig(globalConfig).build();
		String args = URLEncoder.encode(command);
		System.out.print("Sending: '"+command+"' ... ");
		HttpGet request = new HttpGet(chimeraREST+"command="+args);
		CloseableHttpResponse response1 = null;
		try {
			response1 = client.execute(request);
			HttpEntity entity1 = response1.getEntity();
			InputStream entityStream = entity1.getContent();
			if (entity1.getContentLength() == 0) {
				System.out.println("done - no response");
				return response;
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(entityStream));
			String inputLine;
			while ((inputLine = reader.readLine()) != null) {
				// System.out.println("From chimera: "+inputLine.trim());
				if (inputLine.trim().length() > 0)
					response.add(inputLine.trim());
			}
			EntityUtils.consume(entity1);

		} catch (Exception e) {
			System.out.println("failed!");
			structureManager.logWarning("Unable to execute command: "+command+" ("+e.getMessage()+")");
			structureManager.getChimeraManager().clearOnChimeraExit();
			chimera = null;
			chimeraLaunched = false;
			response = null;
		} finally {
			if (request != null)
				request.releaseConnection();
		}
		System.out.println("done");
		return response;
	}

}
