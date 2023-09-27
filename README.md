[![Maven Central](https://img.shields.io/maven-central/v/com.fathzer/jclop2)](https://central.sonatype.com/artifact/com.fathzer/jclop-jclop2)
<picture>
  <img alt="License" src="https://img.shields.io/badge/license-Apache%202.0-brightgreen.svg">
</picture>
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=jclop2_JClop&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=jclop2_JClop)
[![javadoc](https://javadoc.io/badge2/com.fathzer/jclop2/javadoc.svg)](https://javadoc.io/doc/com.fathzer/jclop2)

# JCLOP (Java CLOud Persistence)

A java framework to allow your application to save/read its data to cloud locations.


The main idea behind JClop is to have a framework that manages the difficult part of synchronization between local and remote copies of a file stored in the Cloud.  
It detects conflicts (where both local and remote copies have been updated since the last synchonization) and allows the developer (or the user) to choose how to fix these conflicts (by forcing one of the copies to be replaced by the other).

JClop provides the developer with a high level interface (```com.fathzer.soft.jclop.Service```) to access local copies of cloud stored files and synchronize them with their remote cloud versions.  
It also provides a swing file chooser dialog (```com.fathzer.soft.jclop.swing.URIChooserDialog```) to select cloud hosted or local files in a unified dialog.

It is limited to standard files (it does not manage folders).

Files are identified by their URI. The uri's scheme determines the Cloud provider (for example dropbox://... for Dropbox).  
The easiest way to get a file URI is to use the file chooser dialog. You can also obtain the list of files hosted on the Cloud with the ```Service.getRemoteEnries``` and ```Service.getURI``` methods.

Be aware that the file uris contain the credentials required to access the remote file. This critical information should be hidden, for instance, when displayed on the user's screen.  
The method ```Service.getDisplayable``` returns a credential free version of URI.

JClop is mainly an abstract framework, not directly useable to synchronize with Cloud hosted files. It only provides an implementation the management of local files. This will allow the developper to manage local and cloud hosted files in the same way.  
Concrete implementations of JClop are provided by other projects. Currently, the only available one is for Dropbox (see [https://github.com/jclop2/dropbox](https://github.com/jclop2/dropbox)). 

## How to use this library
This library requires Java 6+.

### How to select a file
First create an instance of URIChooser. See the concrete implementation documentation (for instance [https://github.com/jclop2/dropbox](https://github.com/jclop2/dropbox)), to find out how to do that.
Then:  
```java
final URIChooser chooser = ...;
final URIChooserDialog dialog = new URIChooserDialog(null, "Please select a file", new URIChooser[] {chooser});
final URI uri = dialog.showDialog();
if (uri != null) {
  // A file was selected
} else {
  // No file selected
}
```

This library provides an URIChooser to choose local files: *com.fathzer.soft.jclop.swing.FileChooserPanel*.

By default the dialog is to select a file for reading. If you want to select a file for writing, and possibly create a new file, you should call ```dialog.setSaveDialog(true)``` before calling *showDialog*.

In order to use the URI with this library, you will have to find which service is managing it. Here is a utility method to do that from a list of URIChooser:
```java
public static Service getService(URI uri, URIChooser[] choosers) {
  String scheme = uri.getScheme();
  for (URIChooser uriChooser : choosers) {
    if (uriChooser.getScheme().equals(scheme)) {
      return uriChooser.getService();
    }
  }
  return null;
}
```

### How to read a file
Once you have a file URI, you should first synchronize the local copy with the Cloud to get available updates.
```java
final SynchronizationState state = service.synchronize(uri, null, null);
```
if *state* is *SYNCHRONIZED*, local and remote copies are now synchronized (if this is not the case, see **[Conflict resolution](#conflict-resolution)**).  
You can now read the local file:
```java
final File localFile = service.getLocalFile(uri);
final InputStream stream = new FileInputStream(localFile);
try {
  // Read the file's content
} finally {
  stream.close();
}
```

### How to write a file
Once you have a file URI, you should first write the content locally to the file returned by *Service.getLocalFileForWriting*.  
**WARNING**:
- You should **NOT** write to the file returned by *Service.getLocalFile*, it could prevent the synchronization of updated content.
- **if the local file write fails, you should delete the file**. If you don't, the next synchronize method call will upload the corrupted (or empty) file to the cloud.

After writing into the local file, synchronize local and remote copies using *Service.synchronize*. Check this method result and, if it does not return *SYNCHRONIZED*, have a look at **[Conflict resolution](#conflict-resolution)**

Here is an example (let's say *writeData* method writes the data into its file argument):
```java
final File writeFile = service.getLocalFileForWriting(uri);
try {
	writeData(writeFile);
} catch (IOException e) {
	if (writeFile.delete()) {
    // The file was deleted.
		throw e;
	} else {
		// PANIC: unable to delete the file after error, next synchronization can corrupt data. You probably should send a specific exception there
    throw new MyPanicException(e);
	}
}
```

### Conflict resolution
When *Service.synchronize* method does not return *SYNCHRONIZED*, there's a conflict (local and remote files have been modified since their last synchronization, or remote has been deleted).  
This library gives no conflict resolution strategy. So, you should decide (or ask the user) how to resolve the conflict.

To delete local copy: ```service.deleteLocal(uri);```  
To replace remote copy by local copy: ```service.upload(uri, null, null);```  
To replace local copy by remote copy: ```service.download(uri, null, null);```

## Adding your own Cloud Provider

To add a cloud provider, you should implement a subclass of ```com.fathzer.soft.jclop.Service``` dedicated to this cloud provider and implement its abstract methods.  
You should also implement a class that implements ```com.fathzer.soft.jclop.swing.URIChooser```. The easiest way is to subclass ```com.fathzer.soft.jclop.swing.AbstractURIChooserPanel```.

You can use [https://github.com/jclop2/dropbox](https://github.com/jclop2/dropbox) as an implementation example.

## TODO
- The utility method described in README should be part of this library, for instance in Service class and/or URIChooser interface.
- Currently, JClop is clearly designed for applications that entirely manage the files' life cycle. In particular, even if working with non zip files, it assumes files are zipped.
For instance, the name of local cached files always ends with a .zip extension (even if the files are not zipped). This can be very confusing when trying to edit the local files with, for instance, a text editor (which is not recommended).
- Rather than having a service.getLocalFileForWriting and a service.getLocalFile (for reading), it would be better to directly have streams that, optionally, compress/decompress on the fly. It would also allow the implementation of an output stream that automatically clears the file when an exception occurs while writing to it.
- Manage the "user over quota".
