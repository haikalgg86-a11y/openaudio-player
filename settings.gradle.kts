pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "OpenAudioPlayer"

// Main Application Module
include(":app")

// Core Modules
include(":core:common")
include(":core:model")
include(":core:database")
include(":core:preferences")

// Audio Modules
include(":audio:decoder")
include(":audio:engine")
include(":audio:dsp")
include(":audio:verification")

// USB Modules
include(":usb:detection")
include(":usb:descriptors")
include(":usb:uac")
include(":usb:transfer")

// Player Modules
include(":player:session")
include(":player:queue")
include(":player:library")

// Data Modules
include(":data:local")
include(":data:repository")

// Feature UI Modules
include(":feature:home")
include(":feature:library")
include(":feature:player")
include(":feature:dac")
include(":feature:settings")
include(":feature:equalizer")

// Native NDK Modules
include(":native:usb")
include(":native:audio")
include(":native:decoder")
