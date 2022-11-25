package execution;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

class RefactoredMain {
	public static void main(String[] args) {
		String mayBeRelativePath = args[0];
//		String absolutePath = FileSystems.getDefault().getPath(mayBeRelativePath).normalize().toAbsolutePath().toString();
		
		Path path = Path.of(args[0]);
		
		if (!new File(path.toAbsolutePath().toString()).exists())
			try {
				Files.createDirectories(path);
			} catch (IOException e) {
				System.out.println("Dwasd");
			}
		
		System.out.println(path.toAbsolutePath());
	}
}