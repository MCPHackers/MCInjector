package de.oceanlabs.mcp.mcinjector.data;

import java.nio.file.Path;

public interface Injection {

	boolean load(Path file);

	boolean dump(Path file);
}
