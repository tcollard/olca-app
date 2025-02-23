package org.openlca.app.rcp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.openlca.app.collaboration.util.Ssl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SslCertificates {

	private static final Logger log = LoggerFactory.getLogger(SslCertificates.class);

	static void load() {
		log.debug("Loading external ssl certificates");
		File workspace = Workspace.getDir();
		File certificates = new File(workspace, "ssl-certificates");
		if (!certificates.exists() || !certificates.isDirectory()) {
			log.debug("No certificates found in workspace");
			return;
		}
		for (File file : certificates.listFiles()) {
			if (file.isDirectory()) {
				log.debug("Skipping directory: " + file.getName());
				continue;
			}
			String name = file.getName();
			if (!name.endsWith(".cer") && !name.endsWith(".crt") && !name.endsWith(".pem")) {
				log.debug("Skipping file: " + file.getName() + " (filename must end with .cer, .crt or .pem)");
				continue;
			}
			name = name.substring(0, file.getName().lastIndexOf("."));
			log.debug("Loading certificate " + name);
			try (InputStream stream = new FileInputStream(file)) {
				Ssl.addCertificate(name, stream);
				log.debug("Sucessfully added certificate " + name);
			} catch (IOException e) {
				log.error("Error loading certificate " + name, e);
			}
		}
	}

}
