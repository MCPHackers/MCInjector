package de.oceanlabs.mcp.mcinjector;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import de.oceanlabs.mcp.mcinjector.adaptors.AccessFixer;
import de.oceanlabs.mcp.mcinjector.adaptors.ApplyMap;
import de.oceanlabs.mcp.mcinjector.adaptors.ClassInitAdder;
import de.oceanlabs.mcp.mcinjector.adaptors.InnerClassInitAdder;
import de.oceanlabs.mcp.mcinjector.data.InjectionContext;

public class MCInjectorImpl implements Injector {

    private final Map<String, ClassNode> nameToNode = new HashMap<>();
    private final List<ClassNode> nodes = new ArrayList<>();
    private final InjectionContext context;
    
    public MCInjectorImpl(InjectionContext injectionContext) {
    	this.context = injectionContext;
    }
	
	public ClassNode getClass(String cls) {
		return nameToNode.get(cls);
	}

	@Override
	public ClassNode processClass(ClassNode cn) {
        ClassNode cnBase = new ClassNode();
		ClassVisitor ca = getVisitor(cnBase);
		cn.accept(ca);
		return cnBase;
	}
	
	private ClassVisitor getVisitor(ClassVisitor ca) {
		ca = new ApplyMap(context, ca);
		ca = new AccessFixer(context, ca);
		ca = new InnerClassInitAdder(ca);
		ca = new ClassInitAdder(this, ca);
		return ca;
	}
	
	public void write(Path out) throws IOException {
		try (OutputStream os = Files.newOutputStream(out)) {
			write(os);
		}
	}

    public void write(OutputStream out) throws IOException {
        try (JarOutputStream jarOut = new JarOutputStream(out)) {
	        for (ClassNode node : nodes) {
	            ClassWriter writer = new ClassWriter(0);
	            node.accept(writer);
	            jarOut.putNextEntry(new ZipEntry(node.name + ".class"));
	            jarOut.write(writer.toByteArray());
	            jarOut.closeEntry();
	        }
        }
    }

    public void index(Path file) throws IOException {
    	try (FileSystem fs = FileSystems.newFileSystem(file, (ClassLoader)null)) {
	        Files.walk(fs.getPath("/")).forEach(entry -> {
	            if (!Files.isDirectory(entry) && entry.getFileName().toString().endsWith(".class")) {
	                ClassReader reader;
	                try {
	                    InputStream is = Files.newInputStream(entry);
	                    reader = new ClassReader(is);
	                    is.close();
	                } catch (IOException e) {
	                    e.printStackTrace();
	                    return;
	                }
	                ClassNode node = new ClassNode();
	                reader.accept(node, 0);
	                nodes.add(node);
	                nameToNode.put(node.name, node);
	            }
	        });
	    }
    }

	public void process() {
		for (int i = 0; i < nodes.size(); i++) {
			ClassNode node = nodes.get(i);
			nodes.set(i, processClass(node));
		}
	}
}
