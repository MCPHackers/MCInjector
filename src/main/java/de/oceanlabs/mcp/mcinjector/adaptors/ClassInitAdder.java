package de.oceanlabs.mcp.mcinjector.adaptors;

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.DCONST_0;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.FCONST_0;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.LCONST_0;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.PUTFIELD;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import de.oceanlabs.mcp.mcinjector.Injector;

public class ClassInitAdder extends ClassVisitor {
	private static final Logger log = Logger.getLogger("MCInjector");
	private String className, superName;
	private List<String> constructors = new LinkedList<>();
	private Map<String, String> fields = new LinkedHashMap<>();
	private Injector injector;

	public ClassInitAdder(Injector injector, ClassVisitor cv) {
		super(Opcodes.ASM6, cv);
		this.injector = injector;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.className = name;
		this.superName = superName;
		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		if ((access & ACC_FINAL) == ACC_FINAL && ((access & ACC_STATIC) == 0)) {
			this.fields.put(name, desc);
		}
		return super.visitField(access, name, desc, signature, value);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		if ("<init>".equals(name)) {
			constructors.add(desc);
		}
		return super.visitMethod(access, name, desc, signature, exceptions);
	}

	@Override
	public void visitEnd() {
		ClassVisitor self = this;
		boolean noInit = constructors.isEmpty();
		ClassVisitor supercv = new ClassVisitor(Opcodes.ASM6) {
			@Override
			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				if (noInit && "<init>".equals(name) && !constructors.contains(desc)) {
					log.fine("  Adding synthetic <init>");
					MethodVisitor mv = self.visitMethod(ACC_PRIVATE, "<init>", desc, null, null);
					mv.visitVarInsn(ALOAD, 0);
					for(int i = 0; i < Type.getArgumentTypes(desc).length; i++) {
						mv.visitVarInsn(ALOAD, i + 1);
					}
					mv.visitMethodInsn(INVOKESPECIAL, superName, "<init>", desc, false);
					constructors.add(desc);
				}
				return super.visitMethod(access, name, desc, signature, exceptions);
			}
		};
		ClassNode cls = injector.getClass(superName);
		if(cls != null) {
			cls.accept(supercv);
		}
		
//		if (constructors.isEmpty() && !fields.isEmpty()) {
//			log.fine("  Adding synthetic <init>");
//			MethodVisitor mv = this.visitMethod(ACC_PRIVATE, "<init>", "()V", null, null);
//			mv.visitVarInsn(ALOAD, 0);
//			mv.visitMethodInsn(INVOKESPECIAL, this.superName, "<init>", "()V", false);
//
//			for (Entry<String, String> entry : fields.entrySet()) {
//				mv.visitVarInsn(ALOAD, 0);
//				switch (Type.getType(entry.getValue()).getSort()) {
//				case Type.BOOLEAN:
//				case Type.CHAR:
//				case Type.BYTE:
//				case Type.SHORT:
//				case Type.INT:
//					mv.visitInsn(ICONST_0);
//					break;
//				case Type.FLOAT:
//					mv.visitInsn(FCONST_0);
//					break;
//				case Type.LONG:
//					mv.visitInsn(LCONST_0);
//					break;
//				case Type.DOUBLE:
//					mv.visitInsn(DCONST_0);
//					break;
//				default:
//					mv.visitInsn(ACONST_NULL);
//					break;
//
//				}
//				mv.visitFieldInsn(PUTFIELD, this.className, entry.getKey(), entry.getValue());
//				log.fine("    Field: " + entry.getKey());
//			}
//			mv.visitTypeInsn(NEW, "java/lang/RuntimeException");
//			mv.visitInsn(DUP);
//			mv.visitLdcInsn("Synthetic constructor added by MCP, do not call");
//			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/String;)V", false);
//			mv.visitInsn(ATHROW);
//		}
		super.visitEnd();
	}
}
