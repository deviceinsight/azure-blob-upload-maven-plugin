package azureblobupload;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Mojo(name = "upload")
public class UploadMojo extends AbstractMojo {
	private final Log log = getLog();

    @Parameter(property = "connectionString", required = true)
    public String connectionString;

    @Parameter(property = "rootDir", required = true /*TODO*/)
    public String rootDir;

    @Override
    public void execute() throws MojoExecutionException {
        try {
			log.info(String.format("Using root directory of: '%s'", Paths.get(rootDir).toAbsolutePath().toString()));
			List<Path> files = Files
				.walk(Paths.get(rootDir))
				.filter(Files::isRegularFile)
				.collect(Collectors.toList());

			final CloudBlobContainer container = CloudStorageAccount
				.parse(connectionString)
				.createCloudBlobClient()
				.getContainerReference("$web");
			files.forEach(f -> {
				final String realPath = f.toString();
				final String cloudStoragePath = f.toString().substring(rootDir.length()+1);
				try {
					final CloudBlockBlob blob = container.getBlockBlobReference(cloudStoragePath);
					blob.uploadFromFile(realPath);
					log.info(String.format("Content of '%s' send to '%s'", realPath, cloudStoragePath));
				} catch (URISyntaxException | StorageException | IOException e) {
					e.printStackTrace();
				}
			});

        } catch (StorageException ex) {
        	log.error(ex);
            throw new MojoExecutionException(String.format(
				"Error returned from the service. Http code: %d and error code: %s",
				ex.getHttpStatusCode(),
				ex.getErrorCode()
			));
        } catch (Exception ex) {
        	log.error(ex);
			throw new MojoExecutionException(String.format(
				"Error returned: %s",
				ex.getMessage()
			));
		}
    }


}
