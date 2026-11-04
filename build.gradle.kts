plugins {
	id("net.fabricmc.fabric-loom") version "1.18-SNAPSHOT"
}

version = property("mod_version") as String
group = property("mod_group") as String
base.archivesName = property("archives_base_name") as String

dependencies {
	minecraft("com.mojang:minecraft:${property("minecraft_version")}")
	implementation("net.fabricmc:fabric-loader:${property("loader_version")}")
	implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")
}

java {
	toolchain.languageVersion = JavaLanguageVersion.of(25)
	withSourcesJar()
}

tasks.processResources {
	inputs.property("version", project.version)
	filesMatching("fabric.mod.json") {
		expand("version" to project.version)
	}
}
