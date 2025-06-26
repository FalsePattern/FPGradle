# FPGradle

## [Unreleased]

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