package lava6.l6b;
import java.io.IOException;

public class Lava {
	public final static int version=6;
	public static void main(String[] args) throws IOException {
		System.out.println("Lava version "+version);
		String classname = args[0];
		//load parameters
		String[] args2=null;
		if (args.length>1) {
			args2 = new String[args.length-1];
			System.arraycopy(args,1,args2,0,args2.length);
		}
		//create the memory
		Memory m = new Memory(4096);
		//compile the program
		Compiler c = new Compiler(m,classname);
		c.run();
		char tref = c.getClassTableRef();
		//run the program
		Processor p = new Processor(m,tref);
		p.start(args2);
	}
}