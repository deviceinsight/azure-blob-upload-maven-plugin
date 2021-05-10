package com.deviceinsight.azureblobupload;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

@Mojo(name = "upload")
public class UploadMojo extends AbstractMojo {
	private static final List<String> DEFAULT_EXCLUDES = Collections.emptyList();
	private static final List<String> DEFAULT_INCLUDES = Collections.singletonList("**/**");

	private final Log log = getLog();

	@Parameter(property = "connectionString", required = true)
	public String connectionString;

	@Parameter(property = "rootDir", required = true)
	public String rootDir;

	@Parameter(property = "containerName", required = true)
	public String containerName;

	/**
	 * List of files to include. Specified as fileset patterns that are relative to
	 * the root directory.
	 */
	@Parameter
	public List<String> includes;

	/**
	 * List of files to exclude. Specified as fileset patterns that are relative to
	 * the root directory.
	 */
	@Parameter
	public List<String> excludes;

	@Override
	public void execute() throws MojoExecutionException {
		try {
			final String rootPath = Paths.get(rootDir).toAbsolutePath().toString();

			log.info(String.format("Using root directory of: '%s'", rootPath));

			final CloudBlobContainer container = CloudStorageAccount.parse(connectionString)
					.createCloudBlobClient()
					.getContainerReference(containerName);

			final FileSet fileSet = new FileSet();
			fileSet.setDirectory(rootPath);
			fileSet.setIncludes(getIncludes());
			fileSet.setExcludes(getExcludes());

			FileSetManager fileSetManager = new FileSetManager();
			String[] files = fileSetManager.getIncludedFiles(fileSet);
			log.info(String.format("Found %d files", files.length));

			final FileSystem fs = FileSystems.getDefault();
			for (String file : files) {
				sendFileToCloud(container, fs.getPath(rootDir, file));
			}
		} catch (StorageException ex) {
			log.error(ex);
			throw new MojoExecutionException(
					String.format("Error returned from the service. Http code: %d and error code: %s",
							ex.getHttpStatusCode(), ex.getErrorCode()), ex);
		} catch (Exception ex) {
			log.error(ex);
			throw new MojoExecutionException(String.format("Error returned: %s", ex.getMessage()), ex);
		}
	}

	private void sendFileToCloud(CloudBlobContainer container, Path file) throws MojoExecutionException {
		if (!Files.isRegularFile(file)) {
			return;
		}
		final String realPath = file.toString();
		final String cloudStoragePath = file.toString().substring(rootDir.length() + 1);
		try {
			final CloudBlockBlob blob = container.getBlockBlobReference(cloudStoragePath);
			final String mediaType = Files.probeContentType(file);
			blob.getProperties().setContentType(mediaType);
			blob.uploadFromFile(realPath);
			log.info(String.format("Content of '%s' sent to '%s'", realPath, cloudStoragePath));
		} catch (URISyntaxException | StorageException | IOException ex) {
			throw new MojoExecutionException(ex.getMessage(), ex);
		}
	}

	private List<String> getIncludes() {
		return null != includes && !includes.isEmpty() ? includes : DEFAULT_INCLUDES;
	}

	private List<String> getExcludes() {
		return null != excludes && !excludes.isEmpty() ? excludes : DEFAULT_EXCLUDES;
	}
}
