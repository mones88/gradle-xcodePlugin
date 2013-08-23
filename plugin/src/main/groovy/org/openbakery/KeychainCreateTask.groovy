/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openbakery

import org.gradle.api.tasks.TaskAction
import org.gradle.api.InvalidUserDataException


class KeychainCreateTask extends AbstractXcodeTask {


	KeychainCreateTask() {
		super()
		this.description = "Create a propery keychain that is used for signing the app"
	}

	@TaskAction
	def create() {



		if (project.xcodebuild.sdk.startsWith("iphonesimulator")) {
			println("The simulator build does not need a provisioning profile")
			return
		}

		if (project.xcodebuild.signing.keychain) {
			println "Using keychain " + project.xcodebuild.signing.keychain
			println "Internal keychain " + project.xcodebuild.signing.keychainPathInternal
			return
		}

		if (project.xcodebuild.signing.certificateURI == null) {
			println("not certificateURI specifed so do not create the keychain")
			return
		}


		if (project.xcodebuild.signing.certificatePassword == null) {
			throw new InvalidUserDataException("Property project.xcodebuild.signing.certificatePassword is missing")
		}

		def certificateFile = download(project.xcodebuild.signing.keychainDestinationRoot, project.xcodebuild.signing.certificateURI)

		def keychainPath = project.xcodebuild.signing.keychainPathInternal.absolutePath

		println "Create Keychain '" + keychainPath + "'"

		if (!new File(keychainPath).exists()) {
			runCommand(["security", "create-keychain", "-p", project.xcodebuild.signing.keychainPassword, keychainPath])
		}
		runCommand(["security", "-v", "import", certificateFile, "-k", keychainPath, "-P", project.xcodebuild.signing.certificatePassword, "-T", "/usr/bin/codesign"])


		if (getOSVersion().minor >= 9) {
			String keychainList = runCommandWithResult(["security", "list-keychains"]);

			def commandList = [
							"security",
							"list-keychains",
							"-s"
			]
			for (String keychain in keychainList.split("\n")) {
				String trimmedKeychain = keychain.replaceAll(/^\s*\"|\"$/, "");
				if (new File(trimmedKeychain).exists()) {
					commandList.add(trimmedKeychain);
				}

			}
			commandList.add(keychainPath)
			runCommand(commandList)
		}
	}


}