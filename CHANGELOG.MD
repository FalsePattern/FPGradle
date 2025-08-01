# Changelog

## [Unreleased]

## [1.0.0] - 2025-07-31

### Changed

- JavaCompatibility api for jarInJar

### Added

- Lwjgl3ify support for jarInJar modern java

## [0.19.7] - 2025-07-27

### Changed

- Update UniMixins to 0.1.22

### Fixed

- Remove security manager command line arg for java 24+ compat

## [0.19.6] - 2025-07-12

### Fixed

- Configuration cache compatibility with gradle offline mode

## [0.19.5] - 2025-07-10

### Fixed

- Update checker will no longer run/crash if gradle is in offline mode or internet is unavailable

## [0.19.4] - 2025-07-06

### Fixed

- Make jarInJar compatible with gradle configuration cache

## [0.19.3] - 2025-07-04

### Added

- Automatic filtering for removing kotlin stdlib transitive dependencies if forgelin is not active (joml includes it for some reason)

## [0.19.2] - 2025-06-26

### Fixed

- Configuration cache failing on mixins

## [0.19.1] - 2025-06-26

### Fixed

- Configuration cache failing on setupCiWorkspace task

## [0.19.0] - 2025-06-26

### Changed

- Update and bundle MC parameter mappings directly in the plugin jar
- Updated lwjgl3ify
- Updated gradle shadow plugin

### Added

- Support for gradle configuration cache
- Option to disable timestamps in versions
- Reproducible jars for mod building

## [0.18.0] - 2025-06-03

### Added

- User configurable JVM vendors for build and run
- Jar-in-jar source set bundling

## [0.17.2] - 2025-06-02

### Changed

- Updated lwjgl3ify add-opens declarations for run tasks

## [0.17.1] - 2025-05-29

### Added

- Name overlap handling for the built-in repositories list

## [0.17.0] - 2025-05-29

### Added

- Class-level API declarations in DSL

## [0.16.1] - 2025-05-26

### Changed

- Bump kotlin version in example mod

### Added

- Include FalsePatternLib as a dependency when forgelin is used

### Fixed

- Scala project builds failing when api jar is present

## [0.16.0] - 2025-05-26

### Changed

- Update lwjgl3ify
- Update JetBrains annotations
- Update plugin dependencies

### Added

- Scala support

## [0.15.1] - 2025-05-05

_Initial public release on gradle plugin portal._