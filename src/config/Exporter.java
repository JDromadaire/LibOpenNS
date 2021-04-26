package config;

import java.util.HashMap;

import BaseSubsystems.NL_BaseSubsystem.NL_BaseSubsystem;
import libs.LibExporter;
import parser.Node;
import server.ServerNode;
import server.SocketNode;

public class Exporter implements LibExporter {
	
	@Override
	public HashMap<String, Node> exportClasses() {
		NL_BaseSubsystem bsub  = new NL_BaseSubsystem();
		bsub.init();
		SocketNode.subsystem = bsub;
		ServerNode.subsystem = bsub;
		
		HashMap<String, Node> strs = new HashMap();
		
		strs.put("ServerSocket", new ServerNode(-1, -1));
		strs.put("Socket", new SocketNode(-1, -1));
		
		return strs;
	}

}
