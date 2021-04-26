package server;

import java.util.ArrayList;
import java.util.HashMap;

import BaseSubsystems.NL_BaseSubsystem.NL_BaseSubsystem;
import BaseSubsystems.NL_BaseSubsystem.NetUtils.NetMessage;
import BaseSubsystems.NL_BaseSubsystem.NetUtils.NetMessage.NetHeader;
import main.EntryPoint;
import parser.nodes.BooleanNode;
import parser.nodes.FunctionNode;
import parser.nodes.NumberNode;
import parser.nodes.StringNode;
import variables.ClassNode;
import variables.VariableContext;

public class SocketNode extends ClassNode {

	public static NL_BaseSubsystem subsystem;
	private int subsys_index;
	private boolean closed = false;
	
	public SocketNode(int col, int line, int subsys_index) {
		super(col, line);
		this.typeName = "openns.Socket";
		this.isRoot = false;
		this.subsys_index = subsys_index;
		this.setFunction();
	}

	protected SocketNode(SocketNode other, ArrayList<Object> args) {
		super(other, args);
		this.typeName = "openns.Socket";
		this.isRoot = false;
		
		System.out.println("Client args "+args);
		if (args.size() != 2) {
			EntryPoint.raiseErr("Expected 2 arguments in Socket constructor, got "+args.size());
			return;
		}
		
		if (!(args.get(0) instanceof StringNode)) {
			EntryPoint.raiseErr("Expected IP Adress (String) as first argument : got "+args.get(1)+" ("+args.get(1).getClass().toString()+")");
			return;
		}
		if (!(args.get(1) instanceof NumberNode) ||
				!(((NumberNode)args.get(1)).isInt()) ||
				!(((NumberNode)args.get(1)).isIntegerRange()) ||
				((Integer)((NumberNode)args.get(1)).getNumber().intValue()) < 0 ||
				((Integer)((NumberNode)args.get(1)).getNumber().intValue()) > 65535) {
			EntryPoint.raiseErr("Expected Number (Integer >=0 && <= 65535) as second argument : got "+args.get(1)+" ("+args.get(1).getClass().toString()+")");
			return;
		}
		
		int port = (((NumberNode)args.get(1)).getNumber().intValue());
		String host = ((StringNode)args.get(0)).getValue();
		
		subsys_index = subsystem.createSocket(host, port, true);
	}
	
	public void setFunction() {
		this.objects.put("write", new WriteFunction(-1, -1, NL_BaseSubsystem.PROTOCOL_DEFAULT));
		this.objects.put("writeSecure", new WriteFunction(-1, -1, NL_BaseSubsystem.PROTOCOL_ENCRYPTED));
		this.objects.put("writeFallback", new WriteFunction(-1, -1, NL_BaseSubsystem.PROTOCOL_FALLBACK));
		this.objects.put("read", new ReadFunction(-1, -1, NL_BaseSubsystem.PROTOCOL_DEFAULT));
		this.objects.put("readSecure", new ReadFunction(-1, -1, NL_BaseSubsystem.PROTOCOL_ENCRYPTED));
		this.objects.put("readFallback", new ReadFallbackFunction(-1, -1));
	
		this.objects.put("close", new CloseFunction(-1, -1));
		this.objects.put("isClosed", new ClosedFunction(-1, -1));
	}

	public SocketNode(int i, int j) {
		super(i,j);
		this.typeName = "openns.Socket";
		this.setFunction();
	}
	
	public Object createInstance(VariableContext context, ArrayList<Object> args) {
		if(!isRoot) {
			System.out.println("Can't create instance with sub child");
			return null;
		}
		return new SocketNode(this, args);
	}
	
	public static boolean getThis(ArrayList<Object> args) {
		if (args.size() == 0) {
			EntryPoint.raiseErr("Expected at least 1 argument, got 0");
			return false;
		}
		if (!(args.get(0) instanceof SocketNode)) {
			EntryPoint.raiseErr("Expected Server Node as first argument, got "+args.get(0).getClass());
			return false;
		}
		return true;
	}
	public static boolean isString(ArrayList<Object> args, int index) {
		if (args.size() <= index) {
			EntryPoint.raiseErr("Expected at least "+(index+1)+" arguments, got "+args.size());
			return false;
		}
		return args.get(index) instanceof StringNode;
	}
	
	public static class ReadFunction extends FunctionNode {

		private int protocol;

		public ReadFunction(int col, int line, int protocol) {
			super(col, line);
			this.protocol = protocol;
		}
		
		public Object evaluate(VariableContext ctx, ArrayList<Object> args, HashMap<StringNode, Object> kwargs_entry) {
			
			if (!getThis(args)) { return new BooleanNode(-2,-2, false); }
			
			SocketNode s = (SocketNode) args.get(0);
			if (s.closed) {
				return new BooleanNode(-2,-2, true);
			}
			
			NetMessage msg = s.subsystem.recv(protocol, s.subsys_index);
			
			if (msg == null) {
				s.closed = true;
				return new BooleanNode(-2,-2, true);
			}
			
			if (msg.crashed) {
				return new BooleanNode(-2,-2, false);
			}
			
			return new StringNode(-2,-2, msg.message);
		}
		
	}
	
	public static class ReadFallbackFunction extends FunctionNode {

		public ReadFallbackFunction(int col, int line) {
			super(col, line);
		}
		
		public Object evaluate(VariableContext ctx, ArrayList<Object> args, HashMap<StringNode, Object> kwargs_entry) {
			
			if (!getThis(args)) { return new BooleanNode(-2,-2, false); }
			
			SocketNode s = (SocketNode) args.get(0);
			
			if (s.subsystem.hasFallBackMessage(s.subsys_index)) {
				String str = s.subsystem.getFallBackMessage(s.subsys_index);
				return new StringNode(-1,-1, str);
			}
			
			return new BooleanNode(-2,-2, false);
		}
		
	}
	
	public static class WriteFunction extends FunctionNode {

		private int protocol;

		public WriteFunction(int col, int line, int protocol) {
			super(col, line);
			this.protocol = protocol;
		}
		
		public Object evaluate(VariableContext ctx, ArrayList<Object> args, HashMap<StringNode, Object> kwargs_entry) {
			
			if (!getThis(args)) { return new BooleanNode(-2,-2, false); }
			if (!isString(args, 1)) { return new BooleanNode(-2,-2, false); }
			
			SocketNode s = (SocketNode) args.get(0);
			if (s.closed) {
				return new BooleanNode(-2,-2, true);
			}
			
			String str = ((StringNode)args.get(1)).getValue();
			
			NetMessage msg = new NetMessage();
			msg.head = new NetHeader();
			msg.message = str;
			s.subsystem.send(this.protocol, s.subsys_index, msg);
			return new BooleanNode(-2,-2, true);
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
			
			SocketNode obj = (SocketNode) args.get(0);
			
			if (obj.closed) {
				return new BooleanNode(-1, -1, false);
			}
			
			obj.subsystem.closeSocket(obj.subsys_index);
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
			
			SocketNode obj = (SocketNode) args.get(0);
			
			return new BooleanNode(-1, -1, obj.closed);
		}
		
	}

	
}
