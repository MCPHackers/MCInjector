package de.oceanlabs.mcp.mcinjector.data;

import java.nio.file.Path;

public class InjectionContext {

	public final Exceptions 	exc = new Exceptions();
	public final Access 		acc = new Access();
	public final Constructors 	ctr = new Constructors();
	
	public InjectionContext(Path exc, Path acc, Path ctr) {
		if (exc != null) this.exc.load(exc);
		if (acc != null) this.acc.load(acc);
		if (ctr != null) this.ctr.load(ctr);
	}

	public void dump(Path exc, Path acc, Path ctr) {
		if (exc != null) this.exc.dump(exc);
		if (acc != null) this.acc.dump(acc);
		if (ctr != null) this.ctr.dump(ctr);
	}
}
