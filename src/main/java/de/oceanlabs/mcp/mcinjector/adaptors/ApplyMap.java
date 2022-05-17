package de.oceanlabs.mcp.mcinjector.adaptors;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import de.oceanlabs.mcp.mcinjector.MCInjector;
import de.oceanlabs.mcp.mcinjector.data.Exceptions;
import de.oceanlabs.mcp.mcinjector.data.InjectionContext;

public class ApplyMap extends ClassVisitor {
	private final InjectionContext context;
	String className;

	public ApplyMap(InjectionContext context, ClassVisitor cn) {
		super(Opcodes.ASM6, cn);
		this.context = context;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.className = name;
		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		// static constructors
		if (!name.equals("<clinit>")) {
			exceptions = processExceptions(className, name, desc, exceptions);
		}


		// abstract and native methods don't have a Code attribute
		/*
		 * if ((access & Opcodes.ACC_ABSTRACT) != 0 || (access & Opcodes.ACC_NATIVE) !=
		 * 0) { return super.visitMethod(access, name, desc, signature, exceptions); }
		 */

		return super.visitMethod(access, name, desc, signature, exceptions);
	}

	private String[] processExceptions(String cls, String name, String desc, String[] exceptions) {
		Set<String> set = new HashSet<>();
		for (String s : context.exc.getExceptions(cls, name, desc))
			set.add(s);
		if (exceptions != null) {
			for (String s : exceptions)
				set.add(s);
		}

		if (set.size() > (exceptions == null ? 0 : exceptions.length)) {
			exceptions = set.stream().sorted().toArray(x -> new String[x]);
			context.exc.setExceptions(cls, name, desc, exceptions);
			MCInjector.LOG.log(Level.FINE, "    Adding Exceptions: " + String.join(", ", exceptions));
		}

		return exceptions;
	}
}
