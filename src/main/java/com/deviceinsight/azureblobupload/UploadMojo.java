package com.deviceinsight.azureblobupload;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;

@Mojo(name = "upload")
public class UploadMojo extends AbstractMojo {

	@Parameter(property = "connectionString", required = true)
	public String connectionString;

	// TODO: Pass a base directory instead
	@Parameter(property = "contents", required = true)
	public String contents;

	@Override
	public void execute() throws MojoExecutionException {

		try {

			// Parse the connection string and create a blob client to interact with Blob storage
			// TODO: Make the name of the container configurable
			// TODO: Upload files from directory recursively
			// Creating a sample file
			new File("index.html").deleteOnExit();
			Writer output = new BufferedWriter(new FileWriter(new File("index.html")));
			output.write(contents);
			output.close();

			// Getting a blob reference
			CloudBlockBlob blob = CloudStorageAccount.parse(connectionString).createCloudBlobClient()
					.getContainerReference("$web").getBlockBlobReference(new File("index.html").getName());

			// Creating blob and uploading file to it
			// TODO: Use maven logging
			System.out.println("Uploading the sample file ");
			blob.uploadFromFile(new File("index.html").getAbsolutePath());

		} catch (StorageException ex) {
			// TODO: Throw MojoExecutionException
			System.out.println(String.format("Error returned from the service. Http code: %d and error code: %s",
					ex.getHttpStatusCode(), ex.getErrorCode()));
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
	}
}
