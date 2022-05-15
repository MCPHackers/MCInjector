package de.oceanlabs.mcp.mcinjector.adaptors;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import de.oceanlabs.mcp.mcinjector.MCInjector;
import de.oceanlabs.mcp.mcinjector.MCInjectorImpl;
import de.oceanlabs.mcp.mcinjector.data.Exceptions;

public class ApplyMap extends ClassVisitor {
	String className;
	MCInjectorImpl injector;

	public ApplyMap(MCInjectorImpl injector, ClassVisitor cn) {
		super(Opcodes.ASM6, cn);
		this.injector = injector;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.className = name;
		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		// static constructors
		if (name.equals("<clinit>")) {
			return super.visitMethod(access, name, desc, signature, exceptions);
		}

		exceptions = processExceptions(className, name, desc, exceptions);

		// abstract and native methods don't have a Code attribute
		/*
		 * if ((access & Opcodes.ACC_ABSTRACT) != 0 || (access & Opcodes.ACC_NATIVE) !=
		 * 0) { return super.visitMethod(access, name, desc, signature, exceptions); }
		 */

		return new MethodVisitor(api, cv.visitMethod(access, name, desc, signature, exceptions)) {
		};
	}

	private String[] processExceptions(String cls, String name, String desc, String[] exceptions) {
		Set<String> set = new HashSet<>();
		for (String s : Exceptions.INSTANCE.getExceptions(cls, name, desc))
			set.add(s);
		if (exceptions != null) {
			for (String s : exceptions)
				set.add(s);
		}

		if (set.size() > (exceptions == null ? 0 : exceptions.length)) {
			exceptions = set.stream().sorted().toArray(x -> new String[x]);
			Exceptions.INSTANCE.setExceptions(cls, name, desc, exceptions);
			MCInjector.LOG.log(Level.FINE, "    Adding Exceptions: " + String.join(", ", exceptions));
		}

		return exceptions;
	}
}
