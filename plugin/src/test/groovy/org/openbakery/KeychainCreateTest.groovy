package org.openbakery

import org.gmock.GMockController
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

/**
 * Created with IntelliJ IDEA.
 * User: rene
 * Date: 22.08.13
 * Time: 17:29
 * To change this template use File | Settings | File Templates.
 */
class KeychainCreateTest {

	Project project
	KeychainCreateTask keychainCreateTask


	GMockController mockControl = new GMockController()
	CommandRunner commandRunnerMock
	File keychainDestinationFile
	File certificateFile

	@BeforeClass
	def setup() {
		commandRunnerMock = mockControl.mock(CommandRunner)
		project = ProjectBuilder.builder().build()
		project.buildDir = new File('build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin

		keychainCreateTask = project.tasks.findByName('keychain-create')
		keychainCreateTask.setProperty("commandRunner", commandRunnerMock)

		certificateFile = File.createTempFile("test", ".cert")
		certificateFile.deleteOnExit()
		keychainDestinationFile = new File(project.xcodebuild.signing.keychainDestinationRoot, certificateFile.getName())

	}

	@AfterClass
	def cleanup() {
		certificateFile.delete();
	}


	@Test
	void OSVersion() {
		System.setProperty("os.version", "10.9.0");

		Version version = keychainCreateTask.getOSVersion()
		assert version != null;

		assert version.major == 10
		assert version.minor == 9
		assert version.maintenance == 0
	}


	void expectKeychainCreateCommand() {
		List<String> commandList
		commandList?.clear()
		commandList = ["security", "create-keychain", "-p", "This_is_the_default_keychain_password", project.xcodebuild.signing.keychainPathInternal.toString()]
		commandRunnerMock.runCommand(commandList).times(1)
	}

	void expectKeychainImportCommand() {
		List<String> commandList
		commandList?.clear()
		commandList = ["security", "-v", "import",  keychainDestinationFile.toString(), "-k", project.xcodebuild.signing.keychainPathInternal.toString(), "-P", "password", "-T", "/usr/bin/codesign"];
		commandRunnerMock.runCommand(commandList).times(1)

	}

	void expectKeychainListCommand(result) {
		List<String> commandList
		commandList?.clear()
		commandList = ["security", "list-keychains"];
		commandRunnerMock.runCommandWithResult(commandList).returns(result).times(1)
	}


	void expectKeychainListSetCommand() {
		List<String> commandList
		commandList?.clear()
		String userHome = System.getProperty("user.home")
		commandList = ["security", "list-keychains", "-s"]
		commandList.add(userHome + "/Library/Keychains/login.keychain")
		commandList.add("/Library/Keychains/System.keychain")
		commandList.add(project.xcodebuild.signing.keychainPathInternal.toString())
		commandRunnerMock.runCommand(commandList).times(1)
	}

	@Test
	void create_with_os_x_10_8() {
		System.setProperty("os.version", "10.8.0");
		project.xcodebuild.sdk = 'iphoneos'
		project.xcodebuild.signing.certificateURI = certificateFile.toURL()
		project.xcodebuild.signing.certificatePassword = "password"


		expectKeychainCreateCommand()
		expectKeychainImportCommand()

		mockControl.play {
			keychainCreateTask.create()
		}

	}


	@Test
	void create_with_os_x_10_9() {
		System.setProperty("os.version", "10.9.0");
		project.xcodebuild.sdk = 'iphoneos'
		project.xcodebuild.signing.certificateURI = certificateFile.toURL()
		project.xcodebuild.signing.certificatePassword = "password"

		expectKeychainCreateCommand()
		expectKeychainImportCommand()

		String userHome = System.getProperty("user.home")
		String result = "    \""+ userHome + "/Library/Keychains/login.keychain\"\n" +
								"    \"/Library/Keychains/System.keychain\"";
		expectKeychainListCommand(result)
		expectKeychainListSetCommand()

		mockControl.play {
			keychainCreateTask.create()
		}
	}


	@Test
	void create_with_os_x_10_9_missing_keychain() {
		System.setProperty("os.version", "10.9.0");
		project.xcodebuild.sdk = 'iphoneos'
		project.xcodebuild.signing.certificateURI = certificateFile.toURL()
		project.xcodebuild.signing.certificatePassword = "password"

		expectKeychainCreateCommand()
		expectKeychainImportCommand()
		String userHome = System.getProperty("user.home")
		String result = "    \""+ userHome + "/Library/Keychains/login.keychain\"\n" +
						"    \"/MISSING_PATH/Keychains/MISSING\"\n" +
						"    \"/Library/Keychains/System.keychain\"";
		expectKeychainListCommand(result)
		expectKeychainListSetCommand()

		mockControl.play {
			keychainCreateTask.create()
		}
	}


}
