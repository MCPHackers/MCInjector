package de.oceanlabs.mcp.mcinjector;

import org.objectweb.asm.tree.ClassNode;

public interface Injector {
	ClassNode getClass(String cls);

	ClassNode processClass(ClassNode cn);
}
