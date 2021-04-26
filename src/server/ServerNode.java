package server;

import java.util.ArrayList;
import java.util.HashMap;

import BaseSubsystems.NL_BaseSubsystem.NL_BaseSubsystem;
import main.EntryPoint;
import parser.nodes.BooleanNode;
import parser.nodes.FunctionNode;
import parser.nodes.NumberNode;
import parser.nodes.StringNode;
import server.SocketNode.CloseFunction;
import server.SocketNode.ClosedFunction;
import variables.ClassNode;
import variables.VariableContext;

public class ServerNode extends ClassNode {
	
	public int id;
	private boolean closed = false;
	protected ServerNode(ClassNode other, ArrayList<Object> args) {
		super(other, args);
		this.typeName = "openns.Server";
		
		System.out.println("Serv args "+args);
		if (args.size() != 1) {
			EntryPoint.raiseErr("Expected 1 argument for server, got "+args.size());
			return;
		}
		
		if(args.get(0) instanceof NumberNode &&
				(((NumberNode)args.get(0)).isInt()) &&
				(((NumberNode)args.get(0)).isIntegerRange()) &&
				((Integer)((NumberNode)args.get(0)).getNumber().intValue())>= 0 &&
				((Integer)((NumberNode)args.get(0)).getNumber().intValue())<= 65535) {
			id = subsystem.createServer((((NumberNode)args.get(0)).getNumber().intValue()));
		} else {
			EntryPoint.raiseErr("Expected a positive integer as server port, received "+args.get(0));
			return;
		}
	}
	
	public ServerNode(int col, int line) {
		super(col, line);
		
		this.typeName = "openns.Server";
		
		this.objects.put("accept", new AcceptFunction(-1, -1));

		this.objects.put("close", new CloseFunction(-1, -1));
		this.objects.put("isClosed", new ClosedFunction(-1, -1));
	}
	
	public static boolean getThis(ArrayList<Object> args) {
		if (args.size() == 0) {
			EntryPoint.raiseErr("Expected at least 1 argument, got 0");
			return false;
		}
		if (!(args.get(0) instanceof ServerNode)) {
			EntryPoint.raiseErr("Expected Server Node as first argument, got "+args.get(0).getClass());
			return false;
		}
		return true;
	}
	
	public Object createInstance(VariableContext context, ArrayList<Object> args) {
		if(!isRoot) {
			System.out.println("Can't create instance with sub child");
			return null;
		}
		return new ServerNode(this, args);
	}
	
	public static class AcceptFunction extends FunctionNode {

		public AcceptFunction(int col, int line) {
			super(col, line);
		}
		
		public Object evaluate(VariableContext ctx, ArrayList<Object> args, HashMap<StringNode, Object> kwargs_entry) {
			if (!getThis(args)) {
				return null;
			}
			
			ServerNode obj = (ServerNode) args.get(0);
			if (obj.closed) {
				return new BooleanNode(-1, -1, false);
			}
			
			if (obj.subsystem.hasNewSocket(obj.id)) {
				return new SocketNode(-1, -1, obj.subsystem.getNewSocket(obj.id));
			}
			
			return new BooleanNode(-1, -1, false);
		}
		
	}

	public static class CloseFunction extends FunctionNode {

		public CloseFunction(int col, int line) {
			super(col, line);
		}
		
		public Object evaluate(VariableContext ctx, ArrayList<Object> args, HashMap<StringNode, Object> kwargs_entry) {
			if (!getThis(args)) {
				return null;
			}
			
			ServerNode obj = (ServerNode) args.get(0);
			
			if (obj.closed) {
				return new BooleanNode(-1, -1, false);
			}
			
			obj.subsystem.closeServer(obj.id);
			obj.closed = true;
			
			return new BooleanNode(-1, -1, true);
		}
		
	}
	
	public static class ClosedFunction extends FunctionNode {

		public ClosedFunction(int col, int line) {
			super(col, line);
		}
		
		public Object evaluate(VariableContext ctx, ArrayList<Object> args, HashMap<StringNode, Object> kwargs_entry) {
			if (!getThis(args)) {
				return null;
			}
			
			ServerNode obj = (ServerNode) args.get(0);
			
			return new BooleanNode(-1, -1, obj.closed);
		}
		
	}

	
	public static NL_BaseSubsystem subsystem;

}
